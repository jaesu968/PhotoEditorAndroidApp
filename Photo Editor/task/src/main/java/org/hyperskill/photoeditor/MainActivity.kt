package org.hyperskill.photoeditor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.PermissionChecker
import com.google.android.material.slider.Slider
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var currentImage: ImageView
    // button field to be initialized later for the gallery button
    private lateinit var btnGallery: Button
    // button field to be initialized later to save an edited image
    private lateinit var btnSave: Button
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
        // button click listener for the save button
        btnSave.setOnClickListener {
            // check for permission, override the onRequestPermissionsResults method to check whether the user granted the permission or not,
            // if granted, save the image, if not, show a toast message that permission is required to save the image
            if(checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
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
        slBrightness.addOnChangeListener { _, value, _ ->
            // convert value to Int (it will be multiples of 10 from -100 to 100)
            val delta = value.toInt()
            // recompute from BaseBitmap every time
            baseBitmap?.let { original ->
                currentImage.setImageBitmap(applyBrightnessFilter(original, delta))
            }
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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

                // reapply current slider value from base (no stacking)
                val filtered = applyBrightnessFilter(loadedBitmap, slBrightness.value.toInt())
                // set the filtered bitmap to the ImageView
                currentImage.setImageBitmap(filtered)
            }
        }
}