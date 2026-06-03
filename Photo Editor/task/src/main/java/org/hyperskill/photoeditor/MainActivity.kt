package org.hyperskill.photoeditor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    // button field to be initialized later for the gallery button
    private lateinit var btnGallery: Button
    // property for brightness slider
    private lateinit var slBrightness: Slider
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
        // hook slider and gallery listeners

        // set up the slider listener to adjust brightness when the slider value changes
        slBrightness.addOnChangeListener { _, value, _ ->
            // convert value to Int (it will be multiples of 10 from -100 to 100)
            val delta = value.toInt()
            // recompute from BaseBitmap every time
            baseBitmap?.let { original ->
                currentImage.setImageBitmap(applyBrightnessFilter(original, delta))
            }
        }

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

    // a function to bind the views from the layout to the properties in the activity
    // this makes it easier to reference the views later in the code without having to call findViewById multiple times
    private fun bindViews() {
        // initialize views by finding them by their IDs
        currentImage = findViewById(R.id.ivPhoto)
        btnGallery = findViewById(R.id.btnGallery)
        slBrightness = findViewById(R.id.slBrightness)
    }

    // do not change this function
    fun createBitmap(): Bitmap {
        val width = 200
        val height = 100
        val pixels = IntArray(width * height)

        // get pixel array from source
        var R: Int
        var G: Int
        var B: Int
        var index: Int

        for (y in 0 until height) {
            for (x in 0 until width) {
                // get current index in 2D-matrix
                index = y * width + x
                // get color
                R = x % 100 + 40
                G = y % 100 + 80
                B = (x + y) % 100 + 120

                pixels[index] = Color.rgb(R, G, B)
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

                // reapply current slider value from base (no stacking)
                val filtered = applyBrightnessFilter(loadedBitmap, slBrightness.value.toInt())
                // set the filtered bitmap to the ImageView
                currentImage.setImageBitmap(filtered)
            }
        }
}