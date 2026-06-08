# Photo Editor (Hyperskill)

This is an educational Android project built with Kotlin and `ConstraintLayout`.
The app loads a photo from the gallery, applies multiple image filters, and saves the edited result.

## Project Goals

- Practice Android UI layout and view binding
- Work with bitmaps and per-pixel image processing
- Handle runtime permissions and media saving
- Build a filter pipeline where order matters

## Stage-by-Stage Progress

### Stage 1 - Take a picture (load from the gallery)

**What was implemented**
- Added `ImageView` (`ivPhoto`) to display an image
- Added gallery button (`btnGallery`) to pick media from storage
- Implemented activity result flow to decode the selected image into a bitmap

**Key concepts**
- `Intent.ACTION_PICK`
- `ActivityResultContracts.StartActivityForResult`
- `contentResolver.openInputStream(uri)` + `BitmapFactory.decodeStream(...)`

**Key code idea**
```kotlin
val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
activityResultLauncher.launch(intent)
```

---

### Stage 2 - Brightness filter

**What was implemented**
- Added brightness slider (`slBrightness`)
- Implemented `applyBrightnessFilter(source, delta)`
- Recomputed output from a preserved `baseBitmap` to avoid stacking artifacts

**Key concepts**
- Reading and writing pixels via `getPixels` / `setPixels`
- Per-channel transform with clamping to `[0, 255]`
- Real-time slider-driven rendering

**Key code idea**
```kotlin
val r = clamp(Color.red(color) + delta)
val g = clamp(Color.green(color) + delta)
val b = clamp(Color.blue(color) + delta)
```

---

### Stage 3 - Save a picture

**What was implemented**
- Added save button (`btnSave`)
- Declared storage/media permissions in `AndroidManifest.xml`
- Implemented runtime permission request flow and callback
- Saved current bitmap to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` as JPEG

**Key concepts**
- Runtime permission check + request
- `onRequestPermissionsResult(...)`
- Writing media through `ContentResolver`

**Key code idea**
```kotlin
val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
val output = contentResolver.openOutputStream(uri!!)
bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
```

---

### Stage 4 - Contrast filter

**What was implemented**
- Added contrast slider (`slContrast`)
- Implemented contrast adjustment using image average brightness
- Applied filters in the required order: brightness -> contrast

**Key concepts**
- Average image brightness from full pixel accumulation (`Long`)
- Contrast alpha factor as `Double`
- Order-sensitive filter chaining

**Key code idea**
```kotlin
val avgBrightness = (brightnessSum / pixels.size).toInt()
val alpha = (255.0 + contrast) / (255.0 - contrast)
val rOut = clamp((alpha * (r - avgBrightness) + avgBrightness).toInt())
```

---

### Stage 5 - Saturation and gamma

**What was implemented**
- Added saturation slider (`slSaturation`), range `-250..250`, step `10`
- Added gamma slider (`slGamma`), range `0.2..4.0`, step `0.2`, default `1`
- Implemented saturation and gamma filters and integrated into the render pipeline
- Unified slider listeners so every change re-renders from `baseBitmap`

**Key concepts**
- Per-pixel saturation using `rgbAvg = (r + g + b) / 3`
- Gamma transform with exponent `g`: `255 * (channel / 255.0)^g`
- Full pipeline order:
  1. brightness
  2. contrast
  3. saturation
  4. gamma

**Key code idea**
```kotlin
val bright = applyBrightnessFilter(source, brightness)
val contrast = applyContrastFilter(bright, contrastValue)
val saturation = applySaturationFilter(contrast, saturationValue)
val gamma = applyGammaFilter(saturation, gammaValue)
currentImage.setImageBitmap(gamma)
```

---

## Architecture Notes

- `baseBitmap` stores the original selected image
- Every slider change calls a single render path (`updateImageFromSliders`)
- `clamp(v)` ensures all channel values stay in RGB bounds

## Current UI Controls

- `btnGallery` - pick image from the gallery
- `btnSave` - save current edited image
- `slBrightness` - brightness adjustment
- `slContrast` - contrast adjustment
- `slSaturation` - saturation adjustment
- `slGamma` - gamma adjustment
