package telefam.company.core

enum class ContactFilterTab(
  val apiParam: String,
  val subtitleUpper: String,
) {
  ALL("all", "ALL"),
  SUGGESTIONS("suggestions", "SUGGESTIONS"),
  FOLLOWERS("followers", "FOLLOWERS"),
  FOLLOWING("following", "FOLLOWING"),
  FRIENDS("friends", "FRIENDS"),
}

enum class ContactRelationshipState {
  FOLLOW,
  FOLLOWING,
  FOLLOW_BACK,
  FRIENDS;

  companion object {
    fun compute(isFollowing: Boolean, followsYou: Boolean): ContactRelationshipState = when {
      isFollowing && followsYou -> FRIENDS
      isFollowing -> FOLLOWING
      followsYou -> FOLLOW_BACK
      else -> FOLLOW
    }
  }
}

object TelefamContactsRules {
  const val PAGE_SIZE = 20
  const val MAX_ACTIONS_PER_MINUTE = 20
  const val ONE_MINUTE_MS = 60_000L
  const val RATE_LIMIT_TOAST_MESSAGE = "Try again later"

  fun initialsFor(fullName: String, username: String): String {
    val words = fullName.trim().split(' ').filter { it.isNotBlank() }
    return when {
      words.size >= 2 -> "${words.first().first()}${words.last().first()}".uppercase()
      words.size == 1 && words[0].length >= 2 -> words[0].take(2).uppercase()
      words.size == 1 -> words[0].uppercase()
      username.length >= 2 -> username.take(2).uppercase()
      else -> "TF"
    }
  }
}
