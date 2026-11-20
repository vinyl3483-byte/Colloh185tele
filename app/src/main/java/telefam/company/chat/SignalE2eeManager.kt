package telefam.company.chat

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SessionBuilder
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECKeyPair
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.kem.KEMPublicKey
import org.signal.libsignal.protocol.message.CiphertextMessage
import org.signal.libsignal.protocol.message.PreKeySignalMessage
import org.signal.libsignal.protocol.message.SignalMessage
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyBundle
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SessionRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.state.impl.InMemorySignalProtocolStore
import org.signal.libsignal.protocol.util.KeyHelper
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SignalEncryptedEnvelope(
  val ciphertextB64: String,
  val senderCiphertextB64: String,
  val signalMsgType: Int,
  val compressionCodec: String = "zlib",
  val encryptionScheme: String = "libsignal_v1",
)

data class SignalPublicBundleSnapshot(
  val registrationId: Int,
  val identityPubB64: String,
  val signedPreKeyId: Int,
  val signedPreKeyPubB64: String,
  val signedPreKeySigB64: String,
  val kyberPreKeyId: Int,
  val kyberPreKeyPubB64: String,
  val kyberPreKeySigB64: String,
  val oneTimePreKeys: List<Pair<Int, String>>,
)

data class PeerBundleInput(
  val peerUserId: String,
  val deviceId: Int = 1,
  val registrationId: Int,
  val identityPubB64: String,
  val signedPreKeyId: Int,
  val signedPreKeyPubB64: String,
  val signedPreKeySigB64: String,
  val kyberPreKeyId: Int,
  val kyberPreKeyPubB64: String,
  val kyberPreKeySigB64: String,
  val preKeyId: Int = 0,
  val preKeyPubB64: String = "",
)

/**
 * Full libsignal-android E2EE Engine (X3DH / PQXDH + Double Ratchet + zlib compression + EC seal self-history).
 * State serializes cleanly to JSON for durable local Room persistence and never uploads private keys.
 */
class SignalE2eeManager(
  initialOwnerUserId: String = "",
  serializedStoreJson: String = "",
) {
  private val lock = Any()
  var ownerUserId: String = initialOwnerUserId
    private set

  private var store: InMemorySignalProtocolStore
  private var activeSignedPreKeyId: Int = 1
  private var activeKyberPreKeyId: Int = 1
  private val preKeyIds = mutableListOf<Int>()
  private val sessionAddresses = linkedSetOf<SignalProtocolAddress>()
  private val trustedIdentities = linkedMapOf<String, String>()
  private val peerFallbackPubKeysB64 = linkedMapOf<String, String>()
  private val identityChangedPeers = linkedSetOf<String>()

  init {
    val restored = restoreFromJson(serializedStoreJson)
    if (restored != null) {
      store = restored
    } else {
      val idPair = IdentityKeyPair.generate()
      val regId = KeyHelper.generateRegistrationId(false)
      store = InMemorySignalProtocolStore(idPair, regId)
      generateInitialKeysLocked()
    }
  }

  private fun generateInitialKeysLocked() {
    val idPair = store.identityKeyPair
    val nowMs = System.currentTimeMillis()
    val spkPair = ECKeyPair.generate()
    val spkSig = idPair.privateKey.calculateSignature(spkPair.publicKey.serialize())
    activeSignedPreKeyId = 1
    store.storeSignedPreKey(activeSignedPreKeyId, SignedPreKeyRecord(activeSignedPreKeyId, nowMs, spkPair, spkSig))

    val kyberPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
    val kyberSig = idPair.privateKey.calculateSignature(kyberPair.publicKey.serialize())
    activeKyberPreKeyId = 1
    store.storeKyberPreKey(activeKyberPreKeyId, KyberPreKeyRecord(activeKyberPreKeyId, nowMs, kyberPair, kyberSig))

    preKeyIds.clear()
    for (id in 1..20) {
      store.storePreKey(id, PreKeyRecord(id, ECKeyPair.generate()))
      preKeyIds.add(id)
    }
  }

  private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

  private fun unb64(encoded: String): ByteArray = Base64.decode(encoded, Base64.NO_WRAP)

  fun remainingPreKeyCount(): Int = synchronized(lock) {
    preKeyIds.count { store.containsPreKey(it) }
  }

  fun replenishPreKeysIfLow(minThreshold: Int = 5, targetBatch: Int = 20): Boolean = synchronized(lock) {
    val alive = preKeyIds.filter { store.containsPreKey(it) }
    if (alive.size >= minThreshold) return@synchronized false
    preKeyIds.clear()
    preKeyIds.addAll(alive)
    var nextId = (preKeyIds.maxOrNull() ?: 0) + 1
    val needed = (targetBatch - preKeyIds.size).coerceAtLeast(1)
    repeat(needed) {
      store.storePreKey(nextId, PreKeyRecord(nextId, ECKeyPair.generate()))
      preKeyIds.add(nextId)
      nextId++
    }
    true
  }

  fun consumeIdentityChangedPeers(): List<String> = synchronized(lock) {
    val list = identityChangedPeers.toList()
    identityChangedPeers.clear()
    list
  }

  fun exportPublicBundle(): SignalPublicBundleSnapshot = synchronized(lock) {
    val spk = store.loadSignedPreKey(activeSignedPreKeyId)
    val kpk = store.loadKyberPreKey(activeKyberPreKeyId)
    val otks = preKeyIds.mapNotNull { id ->
      if (store.containsPreKey(id)) {
        val pk = store.loadPreKey(id)
        id to b64(pk.keyPair.publicKey.serialize())
      } else {
        null
      }
    }
    return SignalPublicBundleSnapshot(
      registrationId = store.localRegistrationId,
      identityPubB64 = b64(store.identityKeyPair.publicKey.serialize()),
      signedPreKeyId = spk.id,
      signedPreKeyPubB64 = b64(spk.keyPair.publicKey.serialize()),
      signedPreKeySigB64 = b64(spk.signature),
      kyberPreKeyId = kpk.id,
      kyberPreKeyPubB64 = b64(kpk.keyPair.publicKey.serialize()),
      kyberPreKeySigB64 = b64(kpk.signature),
      oneTimePreKeys = otks,
    )
  }

  fun hasSession(peerUserId: String, deviceId: Int = 1): Boolean = synchronized(lock) {
    store.containsSession(SignalProtocolAddress(peerUserId, deviceId)) ||
      peerFallbackPubKeysB64.containsKey(peerUserId)
  }

  fun initSessionWithPeer(input: PeerBundleInput) = synchronized(lock) {
    val addr = SignalProtocolAddress(input.peerUserId, input.deviceId)
    val remoteIdentity = IdentityKey(ECPublicKey(ensureWire33(unb64(input.identityPubB64))))
    val newIdB64 = b64(remoteIdentity.serialize())
    val addrKey = addressKey(addr)
    val prevIdB64 = trustedIdentities[addrKey]
    if (!prevIdB64.isNullOrBlank() && prevIdB64 != newIdB64) {
      runCatching { store.deleteSession(addr) }
      identityChangedPeers.add(input.peerUserId)
    }
    store.saveIdentity(addr, remoteIdentity)
    trustedIdentities[addrKey] = newIdB64

    val spkPub = ECPublicKey(ensureWire33(unb64(input.signedPreKeyPubB64)))
    val spkSig = unb64(input.signedPreKeySigB64)
    val preKeyPub = if (input.preKeyPubB64.isNotBlank()) ECPublicKey(ensureWire33(unb64(input.preKeyPubB64))) else null
    val preKeyId = if (preKeyPub != null && input.preKeyId > 0) input.preKeyId else PreKeyBundle.NULL_PRE_KEY_ID

    val kyberPub = if (input.kyberPreKeyPubB64.isNotBlank()) {
      runCatching { KEMPublicKey(unb64(input.kyberPreKeyPubB64)) }.getOrNull()
    } else {
      null
    }

    if (kyberPub == null) {
      // Cross-platform Desktop/iOS peer using telefam_ecdh_v1 X25519 bundle
      peerFallbackPubKeysB64[input.peerUserId] = input.signedPreKeyPubB64.ifBlank { input.identityPubB64 }
      return@synchronized
    }

    val kyberSig = if (input.kyberPreKeySigB64.isNotBlank()) unb64(input.kyberPreKeySigB64) else ByteArray(0)

    val bundle = PreKeyBundle(
      input.registrationId,
      input.deviceId,
      preKeyId,
      preKeyPub,
      input.signedPreKeyId,
      spkPub,
      spkSig,
      remoteIdentity,
      input.kyberPreKeyId,
      kyberPub,
      kyberSig,
    )
    SessionBuilder(store, addr).process(bundle)
    sessionAddresses.add(addr)
    peerFallbackPubKeysB64.remove(input.peerUserId)
  }

  fun compressAndEncrypt(
    peerUserId: String,
    plaintextUtf8: String,
    deviceId: Int = 1,
  ): SignalEncryptedEnvelope = synchronized(lock) {
    val compressed = zlibCompress(plaintextUtf8.toByteArray(Charsets.UTF_8))
    val addr = SignalProtocolAddress(peerUserId, deviceId)
    if (!store.containsSession(addr)) {
      val peerKeyB64 = peerFallbackPubKeysB64[peerUserId]
      if (!peerKeyB64.isNullOrBlank()) {
        val peerEcPub = ECPublicKey(ensureWire33(unb64(peerKeyB64)))
        val selfEcPub = store.identityKeyPair.publicKey.publicKey
        val peerPacket = ecdhSeal(compressed, peerEcPub, ECDH_MSG_INFO)
        val selfPacket = ecdhSeal(compressed, selfEcPub, ECDH_SELF_INFO)
        return@synchronized SignalEncryptedEnvelope(
          ciphertextB64 = b64(peerPacket),
          senderCiphertextB64 = b64(selfPacket),
          signalMsgType = CiphertextMessage.WHISPER_TYPE,
          compressionCodec = "zlib",
          encryptionScheme = "telefam_ecdh_v1",
        )
      }
    }

    val cipher = SessionCipher(store, addr)
    val ciphertext: CiphertextMessage = cipher.encrypt(compressed)
    sessionAddresses.add(addr)

    // Self-sealed copy using sender's own libsignal ECC Identity key so sender can read history across restarts/devices
    val selfSealed = store.identityKeyPair.publicKey.publicKey.seal(
      compressed,
      SELF_SEAL_INFO,
    )
    SignalEncryptedEnvelope(
      ciphertextB64 = b64(ciphertext.serialize()),
      senderCiphertextB64 = b64(selfSealed),
      signalMsgType = ciphertext.type,
    )
  }

  fun decryptAndDecompress(
    peerUserId: String,
    isOutgoingFromSelf: Boolean,
    ciphertextB64: String,
    senderCiphertextB64: String,
    signalMsgType: Int,
    compressionCodec: String = "zlib",
    deviceId: Int = 1,
    encryptionScheme: String = "libsignal_v1",
  ): String? = synchronized(lock) {
    val rawCipher = if (isOutgoingFromSelf && senderCiphertextB64.isNotBlank()) {
      runCatching { unb64(senderCiphertextB64) }.getOrNull()
    } else {
      runCatching { unb64(ciphertextB64) }.getOrNull()
    }
    val looksLikeEcdhPacket = rawCipher != null && rawCipher.size > 61 && rawCipher[0] == 0x01.toByte()

    fun decryptLibsignal(): ByteArray? {
      if (isOutgoingFromSelf && senderCiphertextB64.isNotBlank()) {
        return runCatching {
          store.identityKeyPair.privateKey.open(
            unb64(senderCiphertextB64),
            SELF_SEAL_INFO,
          )
        }.getOrNull()
      }
      val raw = rawCipher ?: return null
      val addr = SignalProtocolAddress(peerUserId, deviceId)
      sessionAddresses.add(addr)
      val cipher = SessionCipher(store, addr)
      return runCatching {
        if (signalMsgType == CiphertextMessage.PREKEY_TYPE) {
          cipher.decrypt(PreKeySignalMessage(raw))
        } else {
          cipher.decrypt(SignalMessage(raw))
        }
      }.getOrElse {
        runCatching {
          if (signalMsgType == CiphertextMessage.PREKEY_TYPE) {
            cipher.decrypt(SignalMessage(raw))
          } else {
            cipher.decrypt(PreKeySignalMessage(raw))
          }
        }.getOrNull()
      }
    }

    val compressedBytes: ByteArray? = if (encryptionScheme.equals("telefam_ecdh_v1", ignoreCase = true)) {
      decryptEcdhPacket(isOutgoingFromSelf, ciphertextB64, senderCiphertextB64) ?: decryptLibsignal()
    } else {
      decryptLibsignal() ?: if (looksLikeEcdhPacket) {
        decryptEcdhPacket(isOutgoingFromSelf, ciphertextB64, senderCiphertextB64)
      } else {
        null
      }
    }
    if (compressedBytes == null) return@synchronized null

    val plainBytes = if (compressionCodec.equals("zlib", ignoreCase = true)) {
      runCatching { zlibDecompress(compressedBytes) }.getOrDefault(compressedBytes)
    } else {
      compressedBytes
    }
    String(plainBytes, Charsets.UTF_8)
  }

  private fun decryptEcdhPacket(
    isOutgoingFromSelf: Boolean,
    ciphertextB64: String,
    senderCiphertextB64: String,
  ): ByteArray? {
    val b64Str = if (isOutgoingFromSelf && senderCiphertextB64.isNotBlank()) senderCiphertextB64 else ciphertextB64
    val packet = runCatching { unb64(b64Str) }.getOrNull() ?: return null
    if (isOutgoingFromSelf) {
      return ecdhOpen(packet, store.identityKeyPair.privateKey, ECDH_SELF_INFO)
    }
    val spkPriv = runCatching { store.loadSignedPreKey(activeSignedPreKeyId).keyPair.privateKey }.getOrNull()
    if (spkPriv != null) {
      ecdhOpen(packet, spkPriv, ECDH_MSG_INFO)?.let { return it }
    }
    ecdhOpen(packet, store.identityKeyPair.privateKey, ECDH_MSG_INFO)?.let { return it }
    for (id in preKeyIds) {
      if (store.containsPreKey(id)) {
        val pk = store.loadPreKey(id).keyPair.privateKey
        ecdhOpen(packet, pk, ECDH_MSG_INFO)?.let { return it }
      }
    }
    return null
  }

  private fun ecdhSeal(data: ByteArray, recipientPub: ECPublicKey, info: String): ByteArray {
    val ephPair = ECKeyPair.generate()
    val shared = ephPair.privateKey.calculateAgreement(recipientPub)
    val ephWire = ephPair.publicKey.serialize()
    val ephRaw = if (ephWire.size == 33 && ephWire[0] == 0x05.toByte()) ephWire.copyOfRange(1, 33) else ephWire.takeLast(32).toByteArray()
    val aesKey = hkdfSha256(shared, ephRaw, info.toByteArray(Charsets.UTF_8), 32)
    val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, nonce))
    val enc = cipher.doFinal(data)
    return ByteArray(1 + 12 + 32 + enc.size).apply {
      this[0] = 0x01
      System.arraycopy(nonce, 0, this, 1, 12)
      System.arraycopy(ephRaw, 0, this, 13, 32)
      System.arraycopy(enc, 0, this, 45, enc.size)
    }
  }

  private fun ecdhOpen(packet: ByteArray, privKey: ECPrivateKey, info: String): ByteArray? {
    if (packet.size <= 61 || packet[0] != 0x01.toByte()) return null
    val nonce = packet.copyOfRange(1, 13)
    val ephRaw = packet.copyOfRange(13, 45)
    val enc = packet.copyOfRange(45, packet.size)
    return runCatching {
      val ephPub = ECPublicKey(ensureWire33(ephRaw))
      val shared = privKey.calculateAgreement(ephPub)
      val aesKey = hkdfSha256(shared, ephRaw, info.toByteArray(Charsets.UTF_8), 32)
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), GCMParameterSpec(128, nonce))
      cipher.doFinal(enc)
    }.getOrNull()
  }

  private fun ensureWire33(raw: ByteArray): ByteArray {
    if (raw.size == 33 && raw[0] == 0x05.toByte()) return raw
    val clean32 = if (raw.size >= 32) raw.copyOfRange(raw.size - 32, raw.size) else raw
    return ByteArray(33).apply {
      this[0] = 0x05
      System.arraycopy(clean32, 0, this, 1, clean32.size)
    }
  }

  private fun hkdfSha256(ikm: ByteArray, salt: ByteArray, info: ByteArray, length: Int): ByteArray {
    val prkMac = Mac.getInstance("HmacSHA256").apply {
      init(SecretKeySpec(salt, "HmacSHA256"))
    }
    val prk = prkMac.doFinal(ikm)
    val okm = ByteArrayOutputStream(length)
    var prev = ByteArray(0)
    var counter = 1
    while (okm.size() < length) {
      val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(prk, "HmacSHA256")) }
      mac.update(prev)
      mac.update(info)
      mac.update(counter.toByte())
      prev = mac.doFinal()
      okm.write(prev)
      counter++
    }
    return okm.toByteArray().copyOf(length)
  }

  fun exportStateJson(ownerUid: String = ownerUserId): String = synchronized(lock) {
    ownerUserId = ownerUid
    val root = JSONObject()
    root.put("owner_user_id", ownerUid)
    root.put("reg_id", store.localRegistrationId)
    root.put("identity_key_pair", b64(store.identityKeyPair.serialize()))
    root.put("active_spk_id", activeSignedPreKeyId)
    root.put("active_kyber_id", activeKyberPreKeyId)

    val spkArr = JSONArray()
    store.loadSignedPreKeys().forEach { rec ->
      spkArr.put(JSONObject().put("id", rec.id).put("data", b64(rec.serialize())))
    }
    root.put("spks", spkArr)

    val kpkArr = JSONArray()
    store.loadKyberPreKeys().forEach { rec ->
      kpkArr.put(JSONObject().put("id", rec.id).put("data", b64(rec.serialize())))
    }
    root.put("kpks", kpkArr)

    val pkArr = JSONArray()
    preKeyIds.forEach { id ->
      if (store.containsPreKey(id)) {
        val rec = store.loadPreKey(id)
        pkArr.put(JSONObject().put("id", rec.id).put("data", b64(rec.serialize())))
      }
    }
    root.put("pks", pkArr)

    val sessArr = JSONArray()
    sessionAddresses.forEach { addr ->
      if (store.containsSession(addr)) {
        val rec = store.loadSession(addr)
        sessArr.put(
          JSONObject()
            .put("name", addr.name)
            .put("device_id", addr.deviceId)
            .put("data", b64(rec.serialize())),
        )
      }
    }
    root.put("sessions", sessArr)

    val idArr = JSONArray()
    trustedIdentities.forEach { (key, valB64) ->
      idArr.put(JSONObject().put("key", key).put("data", valB64))
    }
    root.put("identities", idArr)

    val fallbackArr = JSONArray()
    peerFallbackPubKeysB64.forEach { (peerId, pubB64) ->
      fallbackArr.put(JSONObject().put("peer_user_id", peerId).put("pub_b64", pubB64))
    }
    root.put("fallback_peers", fallbackArr)
    root.toString()
  }

  private fun restoreFromJson(raw: String): InMemorySignalProtocolStore? {
    if (raw.isBlank()) return null
    return runCatching {
      val root = JSONObject(raw)
      ownerUserId = root.optString("owner_user_id", ownerUserId)
      val regId = root.getInt("reg_id")
      val idPair = IdentityKeyPair(unb64(root.getString("identity_key_pair")))
      val restored = InMemorySignalProtocolStore(idPair, regId)
      activeSignedPreKeyId = root.optInt("active_spk_id", 1)
      activeKyberPreKeyId = root.optInt("active_kyber_id", 1)

      val spks = root.optJSONArray("spks")
      if (spks != null) {
        for (i in 0 until spks.length()) {
          val obj = spks.getJSONObject(i)
          restored.storeSignedPreKey(obj.getInt("id"), SignedPreKeyRecord(unb64(obj.getString("data"))))
        }
      }
      val kpks = root.optJSONArray("kpks")
      if (kpks != null) {
        for (i in 0 until kpks.length()) {
          val obj = kpks.getJSONObject(i)
          restored.storeKyberPreKey(obj.getInt("id"), KyberPreKeyRecord(unb64(obj.getString("data"))))
        }
      }
      preKeyIds.clear()
      val pks = root.optJSONArray("pks")
      if (pks != null) {
        for (i in 0 until pks.length()) {
          val obj = pks.getJSONObject(i)
          val id = obj.getInt("id")
          restored.storePreKey(id, PreKeyRecord(unb64(obj.getString("data"))))
          preKeyIds.add(id)
        }
      }
      trustedIdentities.clear()
      val ids = root.optJSONArray("identities")
      if (ids != null) {
        for (i in 0 until ids.length()) {
          val obj = ids.getJSONObject(i)
          val key = obj.getString("key")
          val dataB64 = obj.getString("data")
          val parts = key.split("::")
          if (parts.size == 2) {
            val addr = SignalProtocolAddress(parts[0], parts[1].toIntOrNull() ?: 1)
            restored.saveIdentity(addr, IdentityKey(ECPublicKey(unb64(dataB64))))
            trustedIdentities[key] = dataB64
          }
        }
      }
      sessionAddresses.clear()
      val sess = root.optJSONArray("sessions")
      if (sess != null) {
        for (i in 0 until sess.length()) {
          val obj = sess.getJSONObject(i)
          val addr = SignalProtocolAddress(obj.getString("name"), obj.optInt("device_id", 1))
          restored.storeSession(addr, SessionRecord(unb64(obj.getString("data"))))
          sessionAddresses.add(addr)
        }
      }
      peerFallbackPubKeysB64.clear()
      val fallbacks = root.optJSONArray("fallback_peers")
      if (fallbacks != null) {
        for (i in 0 until fallbacks.length()) {
          val obj = fallbacks.getJSONObject(i)
          val peerId = obj.optString("peer_user_id", "")
          val pubB64 = obj.optString("pub_b64", "")
          if (peerId.isNotBlank() && pubB64.isNotBlank()) {
            peerFallbackPubKeysB64[peerId] = pubB64
          }
        }
      }
      restored
    }.getOrNull()
  }

  private fun addressKey(addr: SignalProtocolAddress): String = "${addr.name}::${addr.deviceId}"

  companion object {
    private const val SELF_SEAL_INFO = "telefam-self-copy-v1"
    private const val ECDH_MSG_INFO = "telefam-ecdh-msg-v1"
    private const val ECDH_SELF_INFO = "telefam-ecdh-self-v1"

    fun zlibCompress(input: ByteArray): ByteArray {
      val deflater = Deflater(Deflater.BEST_COMPRESSION)
      deflater.setInput(input)
      deflater.finish()
      val out = ByteArrayOutputStream(input.size)
      val buf = ByteArray(1024)
      while (!deflater.finished()) {
        val count = deflater.deflate(buf)
        out.write(buf, 0, count)
      }
      deflater.end()
      return out.toByteArray()
    }

    fun zlibDecompress(input: ByteArray): ByteArray {
      val inflater = Inflater()
      inflater.setInput(input)
      val out = ByteArrayOutputStream(input.size * 2)
      val buf = ByteArray(1024)
      while (!inflater.finished()) {
        val count = inflater.inflate(buf)
        if (count == 0) break
        out.write(buf, 0, count)
      }
      inflater.end()
      return out.toByteArray()
    }
  }
}
