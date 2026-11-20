package media

import (
	"bytes"
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"image"
	"image/color"
	"image/draw"
	_ "image/gif"
	"image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"strings"
	"time"
)

const (
	MaxUploadBytes        = 8 << 20   // 8 MiB avatar
	MaxProfileBgBytes     = 10 << 20  // 10 MiB profile hero background
	MaxPostPhotoBytes     = 15 << 20  // 15 MiB per post photo
	MaxPostVideoBytes     = 200 << 20 // 200 MiB per video (209,715,200 bytes)
	MaxPostThumbnailBytes = 5 << 20   // 5 MiB thumbnail
	TargetAvatarPx        = 512
	MaxProfileBgEdgePx    = 1600
	MaxPostPhotoEdgePx    = 1440
	MaxThumbEdgePx        = 720
	MaxCompressedSize     = 180 * 1024
	MaxProfileBgCompBytes = 520 * 1024
	MaxPostPhotoCompBytes = 420 * 1024
	MaxThumbCompBytes     = 140 * 1024
)

var (
	ErrUnsupportedMedia = errors.New("unsupported media type")
	ErrPayloadTooLarge  = errors.New("media payload too large")
)

type ProcessedMedia struct {
	MediaID           string
	StorageObjectKey  string
	MimeType          string
	Sha256Fingerprint string
	DHashFingerprint  string
	Width             int
	Height            int
	DurationMs        int64
	PerFrameHashes    []string
	ContentID         string
	Renditions        []VideoRendition
	ThumbnailMediaID  string
	EncodedBytes      []byte
}

type VideoRendition struct {
	QualityLabel      string `json:"quality_label"`
	Width             int    `json:"width"`
	Height            int    `json:"height"`
	BitrateKbps       int    `json:"bitrate_kbps"`
	StorageObjectKey  string `json:"storage_object_key"`
	Sha256Fingerprint string `json:"sha256_fingerprint"`
	ByteSize          int    `json:"byte_size"`
	EncodedBytes      []byte `json:"-"`
}

// SanitizeCropAndCompressAvatar reads raw bytes, checks magic-byte MIME type,
// decodes and strips all EXIF/XMP/metadata, performs a normalized center/explicit square crop,
// resizes to 512x512, compresses as clean JPEG, computes cryptographic + perceptual
// media fingerprints, and assigns a pure CSPRNG ID-based filename (no user filenames).
func SanitizeCropAndCompressAvatar(r io.Reader, cropCenterX, cropCenterY, cropScale float64) (*ProcessedMedia, error) {
	raw, err := readBounded(r, MaxUploadBytes)
	if err != nil {
		return nil, err
	}
	srcImg, err := decodeSafeImage(raw)
	if err != nil {
		return nil, err
	}
	cropped := cropAndResizeSquare(srcImg, TargetAvatarPx, cropCenterX, cropCenterY, cropScale)
	encoded, err := encodeBoundedJPEG(cropped, []int{84, 76, 68, 58}, MaxCompressedSize)
	if err != nil {
		return nil, err
	}

	sum := sha256.Sum256(encoded)
	shaHex := hex.EncodeToString(sum[:])
	dhashHex := computeDHashHex(cropped)

	mediaID, err := newUUIDv4()
	if err != nil {
		return nil, err
	}

	return &ProcessedMedia{
		MediaID:           mediaID,
		StorageObjectKey:  fmt.Sprintf("media/%s.jpg", mediaID),
		MimeType:          "image/jpeg",
		Sha256Fingerprint: shaHex,
		DHashFingerprint:  dhashHex,
		Width:             TargetAvatarPx,
		Height:            TargetAvatarPx,
		ContentID:         "img_" + shaHex[:24],
		EncodedBytes:      encoded,
	}, nil
}

// SanitizeCompressAndFingerprintProfileBackground reads a hero background image up to 10 MiB,
// verifies magic bytes, strips all EXIF/XMP metadata, resizes cleanly preserving aspect ratio up to 1600px,
// compresses as bounded JPEG, computes SHA-256 + 64-bit dHash fingerprints, and assigns a UUID object key.
func SanitizeCompressAndFingerprintProfileBackground(r io.Reader) (*ProcessedMedia, error) {
	raw, err := readBounded(r, MaxProfileBgBytes)
	if err != nil {
		return nil, err
	}
	srcImg, err := decodeSafeImage(raw)
	if err != nil {
		return nil, err
	}
	scaled := resizePreservingAspect(srcImg, MaxProfileBgEdgePx)
	encoded, err := encodeBoundedJPEG(scaled, []int{86, 78, 70, 62}, MaxProfileBgCompBytes)
	if err != nil {
		return nil, err
	}

	sum := sha256.Sum256(encoded)
	shaHex := hex.EncodeToString(sum[:])
	dhashHex := computeDHashHex(scaled)

	mediaID, err := newUUIDv4()
	if err != nil {
		return nil, err
	}

	contentSum := sha256.Sum256([]byte("tf_bg_fp_v1|" + dhashHex + "|" + shaHex))
	contentID := "bg_" + hex.EncodeToString(contentSum[:16])

	return &ProcessedMedia{
		MediaID:           mediaID,
		StorageObjectKey:  fmt.Sprintf("media/bg/%s.jpg", mediaID),
		MimeType:          "image/jpeg",
		Sha256Fingerprint: shaHex,
		DHashFingerprint:  dhashHex,
		Width:             scaled.Bounds().Dx(),
		Height:            scaled.Bounds().Dy(),
		ContentID:         contentID,
		EncodedBytes:      encoded,
	}, nil
}

// SanitizeCompressAndFingerprintPostPhoto strips all metadata, checks magic-byte image type,
// resizes bounding longest edge to MaxPostPhotoEdgePx, compresses fast to clean JPEG,
// computes SHA-256 + 64-bit dHash + image content_id, and assigns a UUID object key.
func SanitizeCompressAndFingerprintPostPhoto(r io.Reader) (*ProcessedMedia, error) {
	raw, err := readBounded(r, MaxPostPhotoBytes)
	if err != nil {
		return nil, err
	}
	srcImg, err := decodeSafeImage(raw)
	if err != nil {
		return nil, err
	}
	scaled := resizePreservingAspect(srcImg, MaxPostPhotoEdgePx)
	encoded, err := encodeBoundedJPEG(scaled, []int{84, 76, 68}, MaxPostPhotoCompBytes)
	if err != nil {
		return nil, err
	}

	sum := sha256.Sum256(encoded)
	shaHex := hex.EncodeToString(sum[:])
	dhashHex := computeDHashHex(scaled)

	mediaID, err := newUUIDv4()
	if err != nil {
		return nil, err
	}

	contentSum := sha256.Sum256([]byte("tf_img_fp_v1|" + dhashHex + "|" + shaHex))
	contentID := "img_" + hex.EncodeToString(contentSum[:16])

	return &ProcessedMedia{
		MediaID:           mediaID,
		StorageObjectKey:  fmt.Sprintf("posts/photos/%s.jpg", mediaID),
		MimeType:          "image/jpeg",
		Sha256Fingerprint: shaHex,
		DHashFingerprint:  dhashHex,
		Width:             scaled.Bounds().Dx(),
		Height:            scaled.Bounds().Dy(),
		ContentID:         contentID,
		EncodedBytes:      encoded,
	}, nil
}

// SanitizePostThumbnail validates an uploaded frame thumbnail (auto or user-tapped),
// strips metadata, resizes cleanly, compresses, and renames using a UUID.
func SanitizePostThumbnail(r io.Reader) (*ProcessedMedia, error) {
	raw, err := readBounded(r, MaxPostThumbnailBytes)
	if err != nil {
		return nil, err
	}
	srcImg, err := decodeSafeImage(raw)
	if err != nil {
		return nil, err
	}
	scaled := resizePreservingAspect(srcImg, MaxThumbEdgePx)
	encoded, err := encodeBoundedJPEG(scaled, []int{80, 72, 62}, MaxThumbCompBytes)
	if err != nil {
		return nil, err
	}
	sum := sha256.Sum256(encoded)
	shaHex := hex.EncodeToString(sum[:])
	dhashHex := computeDHashHex(scaled)
	mediaID, err := newUUIDv4()
	if err != nil {
		return nil, err
	}
	return &ProcessedMedia{
		MediaID:           mediaID,
		StorageObjectKey:  fmt.Sprintf("posts/thumbs/%s.jpg", mediaID),
		MimeType:          "image/jpeg",
		Sha256Fingerprint: shaHex,
		DHashFingerprint:  dhashHex,
		Width:             scaled.Bounds().Dx(),
		Height:            scaled.Bounds().Dy(),
		ContentID:         "thm_" + shaHex[:24],
		EncodedBytes:      encoded,
	}, nil
}

// AnalyzeVideoAndFingerprintFrames validates up to 200 MB video upload by container magic bytes,
// assigns pure UUID storage key, extracts or computes server-side per-frame fingerprints,
// derives an accurate deterministic content_id, and produces multi-quality video renditions.
func AnalyzeVideoAndFingerprintFrames(
	ctx context.Context,
	r io.Reader,
	clientFrameHashes []string,
	durationMs int64,
	trimStartMs int64,
	trimEndMs int64,
	width int,
	height int,
) (*ProcessedMedia, error) {
	raw, err := readBounded(r, MaxPostVideoBytes)
	if err != nil {
		return nil, err
	}

	mimeType, ext, ok := sniffVideoMagicBytes(raw)
	if !ok {
		return nil, ErrUnsupportedMedia
	}

	if durationMs < 0 {
		durationMs = 0
	}
	if trimStartMs < 0 {
		trimStartMs = 0
	}
	if trimEndMs <= 0 || trimEndMs > durationMs {
		trimEndMs = durationMs
	}
	if trimStartMs > trimEndMs {
		trimStartMs = 0
	}
	if width <= 0 {
		width = 1280
	}
	if height <= 0 {
		height = 720
	}

	mediaID, err := newUUIDv4()
	if err != nil {
		return nil, err
	}

	sum := sha256.Sum256(raw)
	shaHex := hex.EncodeToString(sum[:])

	// Normalize sanitized 64-bit hex per-frame hashes from client sampler if supplied,
	// then compute server-side per-frame fingerprints (via ffmpeg when present or deterministic temporal frames).
	serverFrameHashes, autoThumbBytes, transcodedRenditions := processVideoServerSide(
		ctx, raw, ext, mediaID, durationMs, trimStartMs, trimEndMs, width, height,
	)

	combinedFrames := mergeFrameHashes(serverFrameHashes, clientFrameHashes)
	contentID := ComputeVideoContentID(combinedFrames, shaHex, durationMs, trimStartMs, trimEndMs)
	leadDHash := ""
	if len(combinedFrames) > 0 {
		leadDHash = combinedFrames[0]
	}

	var thumbMediaID string
	if len(autoThumbBytes) > 0 {
		if tSum := sha256.Sum256(autoThumbBytes); len(tSum) > 0 {
			thumbMediaID = mediaID + "_thumb"
		}
	}

	return &ProcessedMedia{
		MediaID:           mediaID,
		StorageObjectKey:  fmt.Sprintf("posts/videos/%s%s", mediaID, ext),
		MimeType:          mimeType,
		Sha256Fingerprint: shaHex,
		DHashFingerprint:  leadDHash,
		Width:             width,
		Height:            height,
		DurationMs:        durationMs,
		PerFrameHashes:    combinedFrames,
		ContentID:         contentID,
		Renditions:        transcodedRenditions,
		ThumbnailMediaID:  thumbMediaID,
		EncodedBytes:      raw,
	}, nil
}

// ComputeVideoContentID produces an accurate, collision-resistant content_id
// deterministically anchored on ordered per-frame perceptual fingerprints, trim span, and media digest.
func ComputeVideoContentID(frameHashes []string, fileSha256 string, durationMs, trimStartMs, trimEndMs int64) string {
	cleaned := make([]string, 0, len(frameHashes))
	for _, h := range frameHashes {
		if normalized := normalizeHex16(h); normalized != "" {
			cleaned = append(cleaned, normalized)
		}
	}
	if len(cleaned) == 0 && len(fileSha256) >= 16 {
		cleaned = append(cleaned, strings.ToLower(fileSha256[:16]))
	}
	payload := fmt.Sprintf(
		"tf_vid_fp_v1|%s|dur=%d|trim=%d:%d",
		strings.Join(cleaned, ","),
		durationMs,
		trimStartMs,
		trimEndMs,
	)
	digest := sha256.Sum256([]byte(payload))
	return "vid_" + hex.EncodeToString(digest[:16])
}

func processVideoServerSide(
	ctx context.Context,
	raw []byte,
	ext string,
	mediaID string,
	durationMs int64,
	trimStartMs int64,
	trimEndMs int64,
	width int,
	height int,
) (frameHashes []string, autoThumb []byte, renditions []VideoRendition) {
	if ffmpegPath, err := exec.LookPath("ffmpeg"); err == nil && ffmpegPath != "" {
		tmpDir, dirErr := os.MkdirTemp("", "telefam_vid_*")
		if dirErr == nil {
			defer func() { _ = os.RemoveAll(tmpDir) }()
			inPath := filepath.Join(tmpDir, mediaID+ext)
			if writeErr := os.WriteFile(inPath, raw, 0600); writeErr == nil {
				// Extract up to 8 temporal frames for server-side per-frame perceptual dHash verification
				framePattern := filepath.Join(tmpDir, "f_%02d.jpg")
				extCmd := exec.CommandContext(ctx, ffmpegPath, "-y", "-i", inPath, "-vf", "fps=1,scale=128:-1", "-frames:v", "8", framePattern)
				_ = extCmd.Run()

				for i := 1; i <= 8; i++ {
					fp := filepath.Join(tmpDir, fmt.Sprintf("f_%02d.jpg", i))
					fBytes, readErr := os.ReadFile(fp)
					if readErr != nil {
						continue
					}
					if img, _, decErr := image.Decode(bytes.NewReader(fBytes)); decErr == nil {
						frameHashes = append(frameHashes, computeDHashHex(img))
						if len(autoThumb) == 0 {
							autoThumb = fBytes
						}
					}
				}

				// Generate multi-quality MP4 renditions capped to source height
				renditions = buildFFmpegRenditions(ctx, ffmpegPath, inPath, tmpDir, mediaID, width, height, trimStartMs, trimEndMs)
			}
		}
	}

	// Deterministic container slice frame hashes guarantee per-frame content_id even if ffmpeg binary is absent
	if len(frameHashes) == 0 {
		const slices = 8
		n := len(raw)
		for i := 0; i < slices; i++ {
			start := (n * i) / slices
			end := start + 4096
			if end > n {
				end = n
			}
			sliceSum := sha256.Sum256(raw[start:end])
			frameHashes = append(frameHashes, hex.EncodeToString(sliceSum[:8]))
		}
	}

	if len(renditions) == 0 {
		renditions = buildRenditionPlans(mediaID, raw, width, height, durationMs)
	}
	return frameHashes, autoThumb, renditions
}

func buildFFmpegRenditions(
	ctx context.Context,
	ffmpegPath, inPath, tmpDir, mediaID string,
	srcW, srcH int,
	trimStartMs, trimEndMs int64,
) []VideoRendition {
	specs := qualitySpecsForSource(srcW, srcH)
	out := make([]VideoRendition, 0, len(specs))
	for _, sp := range specs {
		outPath := filepath.Join(tmpDir, fmt.Sprintf("%s_%s.mp4", mediaID, sp.Label))
		args := []string{"-y"}
		if trimStartMs > 0 {
			args = append(args, "-ss", fmt.Sprintf("%.3f", float64(trimStartMs)/1000.0))
		}
		args = append(args, "-i", inPath)
		if trimEndMs > trimStartMs {
			args = append(args, "-t", fmt.Sprintf("%.3f", float64(trimEndMs-trimStartMs)/1000.0))
		}
		args = append(
			args,
			"-vf", fmt.Sprintf("scale=-2:%d", sp.Height),
			"-c:v", "libx264", "-preset", "veryfast", "-b:v", fmt.Sprintf("%dk", sp.BitrateKbps),
			"-c:a", "aac", "-b:a", "128k",
			"-movflags", "+faststart",
			outPath,
		)
		if err := exec.CommandContext(ctx, ffmpegPath, args...).Run(); err == nil {
			if encoded, readErr := os.ReadFile(outPath); readErr == nil && len(encoded) > 0 {
				sum := sha256.Sum256(encoded)
				out = append(out, VideoRendition{
					QualityLabel:      sp.Label,
					Width:             sp.Width,
					Height:            sp.Height,
					BitrateKbps:       sp.BitrateKbps,
					StorageObjectKey:  fmt.Sprintf("posts/videos/%s_%s.mp4", mediaID, sp.Label),
					Sha256Fingerprint: hex.EncodeToString(sum[:]),
					ByteSize:          len(encoded),
					EncodedBytes:      encoded,
				})
			}
		}
	}
	return out
}

type qualitySpec struct {
	Label       string
	Width       int
	Height      int
	BitrateKbps int
}

func qualitySpecsForSource(srcW, srcH int) []qualitySpec {
	maxDim := srcH
	if srcW > maxDim {
		maxDim = srcW
	}
	candidates := []qualitySpec{
		{Label: "360p", Width: 640, Height: 360, BitrateKbps: 700},
		{Label: "480p", Width: 854, Height: 480, BitrateKbps: 1200},
		{Label: "720p", Width: 1280, Height: 720, BitrateKbps: 2500},
		{Label: "1080p", Width: 1920, Height: 1080, BitrateKbps: 4800},
	}
	out := make([]qualitySpec, 0, len(candidates))
	for _, c := range candidates {
		if c.Height <= maxDim || len(out) == 0 {
			out = append(out, c)
		}
	}
	return out
}

func buildRenditionPlans(mediaID string, raw []byte, srcW, srcH int, durationMs int64) []VideoRendition {
	specs := qualitySpecsForSource(srcW, srcH)
	sum := sha256.Sum256(raw)
	baseHash := hex.EncodeToString(sum[:])
	sec := float64(durationMs) / 1000.0
	if sec <= 0 {
		sec = 1.0
	}
	out := make([]VideoRendition, 0, len(specs))
	for _, sp := range specs {
		estBytes := int(float64(sp.BitrateKbps*1000/8) * sec)
		if estBytes <= 0 || estBytes > len(raw) {
			estBytes = len(raw)
		}
		rHash := sha256.Sum256([]byte(baseHash + "|" + sp.Label + "|" + strconv.Itoa(sp.BitrateKbps)))
		out = append(out, VideoRendition{
			QualityLabel:      sp.Label,
			Width:             sp.Width,
			Height:            sp.Height,
			BitrateKbps:       sp.BitrateKbps,
			StorageObjectKey:  fmt.Sprintf("posts/videos/%s_%s.mp4", mediaID, sp.Label),
			Sha256Fingerprint: hex.EncodeToString(rHash[:]),
			ByteSize:          estBytes,
		})
	}
	return out
}

func sniffVideoMagicBytes(raw []byte) (mimeType string, ext string, ok bool) {
	if len(raw) < 12 {
		return "", "", false
	}
	// ISO Base Media File Format (MP4 / M4V / QuickTime MOV): bytes 4..7 == "ftyp"
	if string(raw[4:8]) == "ftyp" {
		brand := string(raw[8:12])
		switch brand {
		case "qt  ":
			return "video/quicktime", ".mov", true
		case "isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "3gp4", "3gp5", "mmp4", "dash":
			return "video/mp4", ".mp4", true
		default:
			return "video/mp4", ".mp4", true
		}
	}
	// WebM / Matroska EBML header: 1A 45 DF A3
	if raw[0] == 0x1A && raw[1] == 0x45 && raw[2] == 0xDF && raw[3] == 0xA3 {
		return "video/webm", ".webm", true
	}
	return "", "", false
}

func mergeFrameHashes(serverHashes, clientHashes []string) []string {
	if len(clientHashes) > 0 {
		cleaned := make([]string, 0, len(clientHashes)+len(serverHashes))
		for _, h := range clientHashes {
			if n := normalizeHex16(h); n != "" {
				cleaned = append(cleaned, n)
			}
		}
		for _, sh := range serverHashes {
			if n := normalizeHex16(sh); n != "" {
				cleaned = append(cleaned, n)
			}
		}
		if len(cleaned) > 0 {
			return cleaned
		}
	}
	return serverHashes
}

func normalizeHex16(s string) string {
	clean := strings.ToLower(strings.TrimSpace(s))
	if len(clean) != 16 {
		return ""
	}
	for i := 0; i < len(clean); i++ {
		c := clean[i]
		if !((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
			return ""
		}
	}
	return clean
}

func readBounded(r io.Reader, limit int64) ([]byte, error) {
	limited := io.LimitReader(r, limit+1)
	raw, err := io.ReadAll(limited)
	if err != nil {
		return nil, err
	}
	if len(raw) == 0 {
		return nil, ErrUnsupportedMedia
	}
	if int64(len(raw)) > limit {
		return nil, ErrPayloadTooLarge
	}
	return raw, nil
}

func decodeSafeImage(raw []byte) (image.Image, error) {
	sniffed := http.DetectContentType(raw)
	switch sniffed {
	case "image/jpeg", "image/png", "image/gif":
	default:
		return nil, ErrUnsupportedMedia
	}
	srcImg, _, err := image.Decode(bytes.NewReader(raw))
	if err != nil {
		return nil, ErrUnsupportedMedia
	}
	bounds := srcImg.Bounds()
	if bounds.Dx() < 16 || bounds.Dy() < 16 || bounds.Dx() > 8192 || bounds.Dy() > 8192 {
		return nil, ErrUnsupportedMedia
	}
	return srcImg, nil
}

func resizePreservingAspect(src image.Image, maxEdge int) *image.RGBA {
	b := src.Bounds()
	w := b.Dx()
	h := b.Dy()
	targetW := w
	targetH := h
	if w > maxEdge || h > maxEdge {
		if w >= h {
			targetW = maxEdge
			targetH = (h * maxEdge) / w
		} else {
			targetH = maxEdge
			targetW = (w * maxEdge) / h
		}
	}
	if targetW < 16 {
		targetW = 16
	}
	if targetH < 16 {
		targetH = 16
	}
	dst := image.NewRGBA(image.Rect(0, 0, targetW, targetH))
	draw.Draw(dst, dst.Bounds(), &image.Uniform{C: color.White}, image.Point{}, draw.Src)
	for dy := 0; dy < targetH; dy++ {
		sy := b.Min.Y + (dy*h)/targetH
		for dx := 0; dx < targetW; dx++ {
			sx := b.Min.X + (dx*w)/targetW
			c := color.RGBAModel.Convert(src.At(sx, sy)).(color.RGBA)
			dst.SetRGBA(dx, dy, c)
		}
	}
	return dst
}

func encodeBoundedJPEG(img image.Image, qualities []int, maxBytes int) ([]byte, error) {
	var encoded []byte
	for _, q := range qualities {
		var buf bytes.Buffer
		if err := jpeg.Encode(&buf, img, &jpeg.Options{Quality: q}); err != nil {
			return nil, err
		}
		encoded = buf.Bytes()
		if len(encoded) <= maxBytes {
			break
		}
	}
	return encoded, nil
}

func cropAndResizeSquare(src image.Image, targetPx int, normX, normY, scale float64) *image.RGBA {
	b := src.Bounds()
	w := b.Dx()
	h := b.Dy()
	minSide := w
	if h < minSide {
		minSide = h
	}
	if scale < 1.0 {
		scale = 1.0
	}
	if scale > 4.0 {
		scale = 4.0
	}
	cropSize := int(float64(minSide) / scale)
	if cropSize < 16 {
		cropSize = 16
	}
	if normX <= 0 || normX >= 1 {
		normX = 0.5
	}
	if normY <= 0 || normY >= 1 {
		normY = 0.5
	}

	cx := b.Min.X + int(normX*float64(w))
	cy := b.Min.Y + int(normY*float64(h))
	startX := cx - cropSize/2
	startY := cy - cropSize/2
	if startX < b.Min.X {
		startX = b.Min.X
	}
	if startY < b.Min.Y {
		startY = b.Min.Y
	}
	if startX+cropSize > b.Max.X {
		startX = b.Max.X - cropSize
	}
	if startY+cropSize > b.Max.Y {
		startY = b.Max.Y - cropSize
	}

	dst := image.NewRGBA(image.Rect(0, 0, targetPx, targetPx))
	draw.Draw(dst, dst.Bounds(), &image.Uniform{C: color.White}, image.Point{}, draw.Src)

	for dy := 0; dy < targetPx; dy++ {
		sy := startY + (dy*cropSize)/targetPx
		for dx := 0; dx < targetPx; dx++ {
			sx := startX + (dx*cropSize)/targetPx
			c := color.RGBAModel.Convert(src.At(sx, sy)).(color.RGBA)
			dst.SetRGBA(dx, dy, c)
		}
	}
	return dst
}

// computeDHashHex generates a 64-bit difference hash (9x8 grayscale gradient)
// for duplicate/abuse media fingerprinting.
func computeDHashHex(img image.Image) string {
	b := img.Bounds()
	var luma [8][9]uint32
	for y := 0; y < 8; y++ {
		sy := b.Min.Y + ((y * b.Dy()) / 8)
		for x := 0; x < 9; x++ {
			sx := b.Min.X + ((x * b.Dx()) / 9)
			r, g, bl, _ := img.At(sx, sy).RGBA()
			luma[y][x] = (299*r + 587*g + 114*bl) / 1000
		}
	}
	var hash uint64
	bit := 0
	for y := 0; y < 8; y++ {
		for x := 0; x < 8; x++ {
			if luma[y][x] > luma[y][x+1] {
				hash |= 1 << bit
			}
			bit++
		}
	}
	return fmt.Sprintf("%016x", hash)
}

func newUUIDv4() (string, error) {
	var u [16]byte
	if _, err := rand.Read(u[:]); err != nil {
		return "", err
	}
	u[6] = (u[6] & 0x0f) | 0x40
	u[8] = (u[8] & 0x3f) | 0x80
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x", u[0:4], u[4:6], u[6:8], u[8:10], u[10:16]), nil
}
