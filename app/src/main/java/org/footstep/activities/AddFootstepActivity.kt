package org.footstep.activities

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_add_footstep.*
import org.footstep.R
import org.footstep.database.DatabaseHandler
import org.footstep.models.FootstepModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class AddFootstepActivity : AppCompatActivity(), View.OnClickListener{

    private var cal = Calendar.getInstance()
    private lateinit var dataSetListener: DatePickerDialog.OnDateSetListener
    private var savedImagePath: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private var mFootstepDetails: FootstepModel? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_footstep)
        setSupportActionBar(toolbar_add_place)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar_add_place.setNavigationOnClickListener {
            onBackPressed()
        }

        if (!Places.isInitialized()) {
            Places.initialize(this@AddFootstepActivity, resources.getString(R.string.google_maps_api_key))
        }

        dataSetListener = DatePickerDialog.OnDateSetListener{
                view, year, month, dayOfMonth ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        updateDateInView()

        if (intent.hasExtra(MainActivity.EXTRA_DETAILS)) {
            mFootstepDetails = intent.getParcelableExtra(MainActivity.EXTRA_DETAILS) as FootstepModel
        }

        if(mFootstepDetails != null) {
            supportActionBar?.title = "Edit Happy Place"

            et_title.setText(mFootstepDetails!!.title)
            et_description.setText(mFootstepDetails!!.description)
            et_date.setText(mFootstepDetails!!.date)
            et_location.setText(mFootstepDetails!!.location)
            mLatitude = mFootstepDetails!!.latitude
            mLongitude = mFootstepDetails!!.longitude

            savedImagePath = Uri.parse(mFootstepDetails!!.image)

            iv_place_image.setImageURI(savedImagePath)

            btn_save.text = "UPDATE"
        }

        et_date.setOnClickListener(this)
        tv_add_image.setOnClickListener(this)
        btn_save.setOnClickListener(this)
        et_location.setOnClickListener(this)

    }

    private fun updateDateInView() {
        val myFormat = "yyyy-MM-dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        et_date.setText(sdf.format(cal.time).toString())
    }

    override fun onClick(v: View?) {
        when(v!!.id) {
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddFootstepActivity,
                    dataSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                    "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems) {
                    dialog, which ->
                    when(which) {
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save -> {
                when {
                    et_title.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    et_description.text.isNullOrEmpty() -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT)
                            .show()
                    }
//                    et_location.text.isNullOrEmpty() -> {
//                        Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT)
//                            .show()
//                    }
                    savedImagePath == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val footstepModel = FootstepModel(
                            if (mFootstepDetails == null) 0 else mFootstepDetails!!.id,
                            et_title.text.toString(),
                            savedImagePath.toString(),
                            et_description.text.toString(),
                            et_date.text.toString(),
                            et_location.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        val dbHandler = DatabaseHandler(this)
                        if (mFootstepDetails == null) {
                            val addHappyPlace = dbHandler.addRow(footstepModel)

                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK);
                                finish()//finishing activity
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateRow(footstepModel)

                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK);
                                finish()//finishing activity
                            }
                        }
                    }
                }

            }
            R.id.et_location -> {
                try {
                    // These are the list of fields which we required is passed
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    // Start the autocomplete intent with a unique request code.
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddFootstepActivity)
                    startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun takePhotoFromCamera() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    // Here after all the permission are granted launch the CAMERA to capture an image.
                    if (report!!.areAllPermissionsGranted()) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(intent,
                            CAMERA
                        )
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()
    }

    private fun choosePhotoFromGallery() {
        Dexter.withActivity(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if(report.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        startActivityForResult(galleryIntent,
                            GALLERY
                        )
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It Looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss()
            }.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
           if (requestCode == GALLERY) {
               if (data != null) {
                   val contentURI = data.data
                   try {
//                       iv_place_image.visibility = View.VISIBLE
//                       iv_place_image.setImageURI(data.data)
                       @Suppress("DEPRECATION")
                       val selectedImageBitmap =
                           MediaStore.Images.Media.getBitmap(this.contentResolver, contentURI)
                       iv_place_image.setImageBitmap(selectedImageBitmap)

                       savedImagePath = saveImageToInternalStorage(selectedImageBitmap)
                       Log.i("Saved Image : ", "Path :: $savedImagePath")
                   } catch (e: Exception) {
                        e.printStackTrace()
                       Toast.makeText(this@AddFootstepActivity, "Failed!", Toast.LENGTH_SHORT)
                           .show()
                   }
               }
           } else if (requestCode == CAMERA) {
               val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
               iv_place_image.setImageBitmap(thumbnail)
               savedImagePath = saveImageToInternalStorage(thumbnail)
               Log.i("Saved Camera Image  : ", "Path :: $savedImagePath")
           } else if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
               val place: Place = Autocomplete.getPlaceFromIntent(data!!)
               et_location.setText(place.address)
               mLatitude = place.latLng!!.latitude
               mLongitude = place.latLng!!.longitude
           }
        }
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var path = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        var file = File(path, "${UUID.randomUUID()}.jpg")
        file.outputStream()
            .use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }
        return Uri.parse(file.absolutePath)
    }

    companion object {
        private const val GALLERY = 1
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "FootstepImages"
        // A constant variable for place picker
        private const val PLACE_AUTOCOMPLETE_REQUEST_CODE = 3
    }
}
