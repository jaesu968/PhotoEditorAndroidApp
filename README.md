# Photo Editor (Hyperskill)

Android photo editor project built with Kotlin. The app lets users pick an image from the gallery, apply real-time filters, and save the edited output.

## Project Goals

- Build Android UI with `ConstraintLayout` and Material components
- Practice bitmap-based pixel manipulation in Kotlin
- Handle Android storage/runtime permission flow
- Apply multiple filters in a deterministic pipeline
- Keep the UI responsive during expensive image processing

## Stage-by-Stage Progress

### Stage 1 - Take a picture (gallery load)

**Implemented**
- Added `ImageView` (`ivPhoto`) to preview image output
- Added gallery button (`btnGallery`)
- Implemented gallery pick flow with activity result launcher

**Key concepts**
- `Intent.ACTION_PICK`
- `ActivityResultContracts.StartActivityForResult`
- `BitmapFactory.decodeStream(...)`

**Key code idea**
```kotlin
val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
activityResultLauncher.launch(intent)
```

### Stage 2 - Brightness filter

**Implemented**
- Added `slBrightness`
- Implemented `applyBrightnessFilter(source, delta)`
- Preserved original source with `baseBitmap` to avoid stacking errors

**Key concepts**
- `getPixels` / `setPixels`
- Per-channel RGB adjustment
- Channel clamping to `[0, 255]`

**Key code idea**
```kotlin
val r = clamp(Color.red(color) + delta)
val g = clamp(Color.green(color) + delta)
val b = clamp(Color.blue(color) + delta)
```

### Stage 3 - Save a picture

**Implemented**
- Added `btnSave`
- Added required storage/media permissions in `AndroidManifest.xml`
- Implemented runtime permission request and callback flow
- Saved edited image as JPEG to `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`

**Key concepts**
- Runtime permissions (`WRITE_EXTERNAL_STORAGE` for required API levels)
- `onRequestPermissionsResult(...)`
- Media insertion via `ContentResolver`

**Key code idea**
```kotlin
val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
val output = contentResolver.openOutputStream(uri!!)
bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)
```

### Stage 4 - Contrast filter

**Implemented**
- Added `slContrast`
- Implemented contrast using average image brightness
- Applied filter order: `brightness -> contrast`

**Key concepts**
- `avgBrightness` from full accumulation (`Long`) then division to `Int`
- Contrast factor: `alpha = (255.0 + contrast) / (255.0 - contrast)`

**Key code idea**
```kotlin
val avgBrightness = (brightnessSum / pixels.size).toInt()
val alpha = (255.0 + contrast) / (255.0 - contrast)
val rOut = clamp((alpha * (r - avgBrightness) + avgBrightness).toInt())
```

### Stage 5 - Saturation and gamma

**Implemented**
- Added `slSaturation` (`-250..250`, step `10`, default `0`)
- Added `slGamma` (`0.2..4.0`, step `0.2`, default `1`)
- Implemented saturation and gamma formulas
- Extended the filter pipeline to:
  1. Brightness
  2. Contrast
  3. Saturation
  4. Gamma

**Key concepts**
- Per-pixel `rgbAvg = (r + g + b) / 3`
- Gamma transform: `255 * (channel / 255.0).pow(gamma)`

**Key code idea**
```kotlin
val bright = applyBrightnessFilter(source, brightness)
val contrast = applyContrastFilter(bright, contrastValue)
val saturation = applySaturationFilter(contrast, saturationValue)
val gamma = applyGammaFilter(saturation, gammaValue)
```

### Stage 6 - No more freezes

**Implemented**
- Added coroutine dependency:
  - `implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.9"`
- Moved filter computation to background thread (`Dispatchers.Default`)
- Kept UI updates on the main thread (`runOnUiThread`)
- Added cancellation for stale computations (`lastJob?.cancel()`)
- Added lifecycle cleanup in `onDestroy()` (`lastJob` + `uiScope` cancel)

**Key concepts**
- Activity-owned coroutine scope
- Async filter recomputation on slider changes
- Cancel obsolete jobs to avoid outdated UI writes

**Key code idea**
```kotlin
lastJob?.cancel()
lastJob = uiScope.launch(Dispatchers.Default) {
    val finalBitmap = computeFilteredBitmap(source, brightness, contrast, saturation, gamma)
    ensureActive()
    runOnUiThread { currentImage.setImageBitmap(finalBitmap) }
}
```

## Current Architecture Notes

- `baseBitmap` always stores original loaded image
- Slider changes call one unified render entrypoint: `updateImageFromSliders()`
- Filter pipeline is deterministic and order-dependent
- `clamp(v)` keeps channels in valid RGB range

## UI Controls

- `btnGallery` - pick image from the gallery
- `btnSave` - save current edited image
- `slBrightness` - brightness adjustment
- `slContrast` - contrast adjustment
- `slSaturation` - saturation adjustment
- `slGamma` - gamma adjustment
