package org.hyperskill.photoeditor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import com.google.android.material.slider.Slider
import kotlin.math.pow
import kotlinx.coroutines.*
class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    // button field to be initialized later for the gallery button
    private lateinit var btnGallery: Button
    // button field to be initialized later to save an edited image
    private lateinit var btnSave: Button
    // property for brightness slider
    private lateinit var slBrightness: Slider
    // property for the contrast slider
    private lateinit var slContrast: Slider
    // property for the saturation slider
    private lateinit var slSaturation: Slider
    // property for the gamma slider
    private lateinit var slGamma: Slider
    // field to keep track of the last job in case we wish to cancel it
    // beginning of asynchronous coding for slider adjustments to
    // avoid blocking the main thread and ensure a smooth user experience when applying filters to the image,
    // especially for larger images where processing can take more time
    private var lastJob: Job? = null
    // activity owned scope
    // for launching code asynchronously for slider adjustments to
    // avoid blocking the main thread and ensure a smooth user experience when applying filters to the image,
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    // property for baseBitmap to store the original image for brightness adjustment
    private var baseBitmap: Bitmap? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        // get initial bitmap from the createBitmap function and set it to the ImageView
        val initial = createBitmap()
        baseBitmap = initial

        //do not change this line
        currentImage.setImageBitmap(createBitmap())
        // set up click listener for the gallery button
        btnGallery.setOnClickListener {
            // create the intent to open the gallery from the device
            val intent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            // launch the intent using the activity result launcher
            activityResultLauncher.launch(intent)
        }
        // button click listener for the save button
        btnSave.setOnClickListener {
            // check for permission, override the onRequestPermissionsResults method to check whether the user granted the permission or not,
            // if granted, save the image, if not, show a toast message that permission is required to save the image
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // if permission is granted, override onRequestPermissionsResult
                // get a bitmap from the current image in the ImageView
                val bitmap: Bitmap = (currentImage.drawable as BitmapDrawable).bitmap
                // get content values to insert the image into the MediaStore
                val values = ContentValues()
                // place the appropriate metadata for the image such as title, display name, description, MIME type, and date added
                values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                values.put(MediaStore.Images.ImageColumns.WIDTH, bitmap.width)
                values.put(MediaStore.Images.ImageColumns.HEIGHT, bitmap.height)
                // insert the content values into the MediaStore and get the URI for the new image
                val uri = this@MainActivity.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return@setOnClickListener // if the URI is null, return early
                // open an output stream to the URI and compress the bitmap into JPEG format to save it
                val outputStream = contentResolver.openOutputStream(uri) ?: return@setOnClickListener
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            } else {
                // request permission if it is not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0
                )
            }
        }
        // hook slider and gallery listeners

        // set up the slider listener to adjust brightness when the slider value changes
        // apply filters together in required order to avoid stacking effects,
        // meaning that when one slider is adjusted,
        // it should apply both filters together based on the current values of both sliders to ensure
        // that the adjustments are applied correctly without compounding the effects of previous adjustments
        // Brightness slider listener first
        slBrightness.addOnChangeListener { _, _, _ ->
            updateImageFromSliders() // get all current slider values and apply them together to the base bitmap to update the image in the ImageView
            }
        // Contrast slider listener second
        slContrast.addOnChangeListener { _, _, _ ->
            updateImageFromSliders() // get all current slider values and apply them together to the base bitmap to update the image in the ImageView
            }
        // Saturation slider listener third
        slSaturation.addOnChangeListener { _, _, _ ->
            updateImageFromSliders() // get all current slider values and apply them together to the base bitmap to update the image in the ImageView
            }
        // Gamma slider listener fourth
        slGamma.addOnChangeListener { _, _, _ ->
            updateImageFromSliders() // get all current slider values and apply them together to the base bitmap to update the image in the ImageView
        }
    }
    // helper function to override onRequestPermissionResult
    // this function checks if the permission request code matches and if the permission was granted,
    // then it calls the save image logic again to save the image after permission is granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            // if the request code matches and the permission is granted, call the save button click listener logic again to save the image
            if (requestCode == 0 && grantResults.isNotEmpty()
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                btnSave.callOnClick()
            }
        }

    // helper function to apply both contrast and brightness filters
    fun computeFilteredBitmap(source : Bitmap, brightness: Int, contrast: Int, saturation: Int, gamma: Double): Bitmap {
        // get brightness adjusted bitmap
        val brightBitmap = applyBrightnessFilter(source, brightness)
        // get adjusted contrast bitmap by applying contrast filter to the adjusted brightness bitmap
        // to ensure that both adjustments are applied together correctly without stacking effects
        val contrastBitmap = applyContrastFilter(brightBitmap, contrast)
        // apply the saturation filter to the adjusted contrast bitmap with the current saturation slider value to ensure that all adjustments are applied together correctly without stacking effects
        val saturationBitmap = applySaturationFilter(contrastBitmap, saturation)
        // apply the gamma filter to the adjusted saturation bitmap with the current gamma slider value to ensure that all adjustments are applied together correctly without stacking effects
        val gammaBitmap = applyGammaFilter(saturationBitmap, gamma)
        // return the final adjusted bitmap with all filters applied together (don't set ImageView)
        return gammaBitmap
    }
    // tiny helper function to apply all filters together for the sliders
    private fun updateImageFromSliders(){
        // capture immutable inputs before launching
        // remember source is baseBitmap
        val source = baseBitmap ?: return
        val brightness = slBrightness.value.toInt()
        val contrast = slContrast.value.toInt()
        val saturation = slSaturation.value.toInt()
        val gamma = slGamma.value.toDouble()
        // cancel the previous job if there is one
        lastJob?.cancel()

        // launch on background dispatcher
        lastJob = uiScope.launch(Dispatchers.Default) {
           // compute the final bitmap with all filters applied together
            val finalBitmap = computeFilteredBitmap(source, brightness, contrast, saturation, gamma)
            // check if the coroutine was canceled while calculating, if so, stop immediately, saving resources
            ensureActive()
            // use runOnUIThread to apply changes to final image on Main Thread
            runOnUiThread {
                currentImage.setImageBitmap(finalBitmap)
            }
        }
    }
    // a function to apply gamma changes to the image based on the slider value
    fun applyGammaFilter(source: Bitmap, gamma: Double): Bitmap {
        // read all pixels from the source bitmap into an array
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        // get the pixel data from the source bitmap and store it in the "pixels" array
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        // for each pixel, adjust the RGB values based on the gamma value from the slider
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            // apply gamma adjustment formula to each RGB channel and clamp the result
            // gamma adjustment formula: 255.0 * (red / 255.0 ^ gamma) to adjust the brightness of the image based on the gamma value from the slider,
            // where red is the original red value of the pixel (substitute same formula for green and, blue channels)
            // and gamma is the value from the slider that controls the intensity of the adjustment
            val rGamma = clamp((255.0 * (r / 255.0).pow(gamma)).toInt())
            val gGamma = clamp((255.0 * (g / 255.0).pow(gamma)).toInt())
            val bGamma = clamp((255.0 * (b / 255.0).pow(gamma)).toInt())
            // set the adjusted color back to the pixel array
            pixels[i] = Color.argb(Color.alpha(color), rGamma, gGamma, bGamma)
        }
        // create a new bitmap with the adjusted pixel array
        val adjustedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // set the adjusted pixels to the new bitmap
        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        // return the adjusted bitmap to be displayed in the ImageView
        return adjustedBitmap
    }
    // a function to apply saturation changes to the image based on the slider value
    fun applySaturationFilter(source: Bitmap, saturation: Int): Bitmap {
        // read all pixels from the source bitmap into an array
        val width = source.width
        val height = source.height
        val pixels= IntArray(width * height)
        // get the pixel data from the source bitmap and store it in the "pixels" array
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        // variable for the alpha value, alpha = (255.0 + saturation) / (255.0 - saturation)
        val alpha = (255.0 + saturation) / (255.0 - saturation) // double to avoid integer division and loss of precision
        // for each pixel, adjust the RGB values based on the saturation value from the slider
        for (i in pixels.indices){
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            // compute average rgb values to compute the adjusted RGB values based on the saturation adjustment formula
            val rgbAvg = (r + g + b) / 3
            // apply saturation adjustment formula to each RGB channel and clamp the result
            val rSat = clamp((alpha * (r - rgbAvg) + rgbAvg).toInt())
            val gSat = clamp((alpha * (g - rgbAvg) + rgbAvg).toInt())
            val bSat = clamp((alpha * (b - rgbAvg) + rgbAvg).toInt())
            // set the adjusted color back to the pixel array
            pixels[i] = Color.argb(Color.alpha(color), rSat, gSat, bSat)
        }

        // create a new bitmap with the adjusted pixel array
        val adjustedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // set the adjusted pixels to the new bitmap
        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        // return the adjusted bitmap to be displayed in the ImageView
        return adjustedBitmap
    }
    //a function to apply contrast changes to the image based on the slider value
    fun applyContrastFilter(source: Bitmap, contrast: Int): Bitmap {
        // read all pixels from the source bitmap into an array
        val width = source.width // get the width of the source bitmap
        val height = source.height // get the height of the source bitmap
        val pixels = IntArray(width * height) // create an array to hold the pixel data
        // get the pixel data from the source bitmap and store it in the "pixels" array
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        // variable for the sum of the brightness
        var brightnessSum = 0L // should be a long to avoid overflow when summing brightness values of all pixels
        // for each pixel, adjust the RGB values based on the contrast factor
        // first loop, for each pixel compute brightness (r + g + b) / 3
        for (i in pixels.indices){
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            // compute brightness for the pixel and add it to the brightness sum
            brightnessSum += (r + g + b) / 3
        }
        // avgBrightness will be computed by (Sum Of Brightness / pixelCount).toInt() after the loop
        // compute the average brightness of the image
        val avgBrightness = (brightnessSum / pixels.size).toInt()
        // compute alpha, alpha = (255.0 + contrast) / (255.0 - contrast)
        val alpha = (255.0 + contrast) / (255.0 - contrast)
        // second loop, for each pixel, clamp the adjusted RGB values based on the contrast factor and the average brightness
        for(i in pixels.indices){
            val color = pixels[i]
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            // apply contrast adjustment formula to each RGB channel and clamp the result
            val rContrast = clamp((alpha * (r - avgBrightness) + avgBrightness).toInt())
            val gContrast = clamp((alpha * (g - avgBrightness) + avgBrightness).toInt())
            val bContrast = clamp((alpha * (b - avgBrightness) + avgBrightness).toInt())
            // set the adjusted color back to the pixel array
            pixels[i] = Color.argb(Color.alpha(color), rContrast, gContrast, bContrast)
        }
        // create a new bitmap with the adjusted pixel array
        val adjustedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // set the adjusted pixels to the new bitmap
        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        // return the adjusted bitmap to be displayed in the ImageView
        return adjustedBitmap
    }

    // a function to apply brightness changes to the image based on the slider value
    fun applyBrightnessFilter(source: Bitmap, delta: Int): Bitmap {
        // read pixel array from the source bitmap
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        // for each pixel, adjust the RGB values based on the delta value from the slider
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = clamp(Color.red(color) + delta)
            val g = clamp(Color.green(color) + delta)
            val b = clamp(Color.blue(color) + delta)
            // set the adjusted color back to the pixel array
            pixels[i] = Color.argb(Color.alpha(color), r, g, b)
        }
        // create a new bitmap with the adjusted pixel array
        val adjustedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        // set the adjusted pixels to the new bitmap
        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        // return the adjusted bitmap to be displayed in the ImageView
        return adjustedBitmap
    }

    // clamping helper function
    // this function ensures that the RGB values stay within the valid range of 0 to 255 after applying the brightness adjustment
    private fun clamp(v: Int) = v.coerceIn(0, 255)

    // helper function to check to image save permission
    fun checkPermission(manifestPermission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.checkSelfPermission(manifestPermission) == PackageManager.PERMISSION_GRANTED
        } else {
            PermissionChecker.checkSelfPermission(this, manifestPermission) == PermissionChecker.PERMISSION_GRANTED
        }
    }

    // A function to bind the views from the layout to the properties in the activity
    // This makes it easier to reference the views later in the code without having to call findViewById multiple times.
    private fun bindViews() {
        // initialize views by finding them by their IDs
        currentImage = findViewById(R.id.ivPhoto)
        btnGallery = findViewById(R.id.btnGallery)
        slBrightness = findViewById(R.id.slBrightness)
        btnSave = findViewById(R.id.btnSave)
        slContrast = findViewById(R.id.slContrast)
        slSaturation = findViewById(R.id.slSaturation)
        slGamma = findViewById(R.id.slGamma)
    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)

        // get pixel array from source
        var r: Int
        var g: Int
        var b: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                r = x % 100 + 40
                g = y % 100 + 80
                b = (x + y) % 100 + 120

                pixels[index] = Color.rgb(r, g, b)
            }
        }

        // output bitmap
        val bitmapOut = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        bitmapOut.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmapOut
    }


    // ActivityResultLauncher to handle the result from the gallery
    private val activityResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            // check if the result is OK and data is not null
            if (result.resultCode == RESULT_OK) {
                // get the URI of the selected image from the gallery
                val photoUri = result.data?.data ?: return@registerForActivityResult
                // decode a bitmap from the selected image URI
                val loadedBitmap = contentResolver.openInputStream(photoUri)?.use { inputStream ->
                    // set the loaded bitmap as the current image and also update the baseBitmap for brightness adjustments
                    BitmapFactory.decodeStream(inputStream)
                } ?: return@registerForActivityResult
                // keep the original image as untouched base
                baseBitmap = loadedBitmap

                // Show the picked image immediately; filters are applied asynchronously after this.
                currentImage.setImageBitmap(loadedBitmap)

                // call updateImageSliders(), keeps one rendering path and keeps UI responsive
                updateImageFromSliders()
            }
        }
    // override onDestroy() so it clears out jobs and uiScope
    // make sure no background tasks are running when the activity is closed to avoid memory leaks and unnecessary resource usage
    override fun onDestroy() {
        lastJob?.cancel() // cancel any jobs
        uiScope.cancel() // cancel the scope to clean up resources and avoid memory leaks when the activity is destroyed
        super.onDestroy() // destroy activity
    }

}
