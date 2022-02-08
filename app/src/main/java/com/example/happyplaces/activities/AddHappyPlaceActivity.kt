package com.example.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder

import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.happyplaces.*
import com.example.happyplaces.R
import com.example.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.create
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : AppCompatActivity(),View.OnClickListener {
    private var binding: ActivityAddHappyPlaceBinding? = null

    private var myCalendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener
    private lateinit var galleryImageResultLauncher: ActivityResultLauncher<Intent>

    private lateinit var resultLauncherCamera: ActivityResultLauncher<Intent>
    private lateinit var resultLauncherLocation: ActivityResultLauncher<Intent>

    private var saveImageToInternalStorage : Uri? = null
    private var myLatitude : Double = 0.0
    private var myLongitude : Double = 0.0

    private var myHappyPlaceDetails: HappyPlaceEntity? = null

    private lateinit var myFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding?.root)



        setSupportActionBar(binding?.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding?.toolbarAddPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        myFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        if (!Places.isInitialized()){
            Places.initialize(this,resources.getString(R.string.google_maps_api_key))
        }


        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            myHappyPlaceDetails = intent.getSerializableExtra(MainActivity.EXTRA_PLACE_DETAILS) as HappyPlaceEntity
        }

        dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR,year)
            myCalendar.set(Calendar.MONTH,month)
            myCalendar.set(Calendar.DAY_OF_MONTH,dayOfMonth)
            updateDateInView()
        }

        if (myHappyPlaceDetails != null){
            supportActionBar?.title = "Edit Happy Place"
            binding?.etTitle?.setText(myHappyPlaceDetails!!.title)
            binding?.etDescription?.setText(myHappyPlaceDetails!!.description)
            binding?.etDate?.setText(myHappyPlaceDetails!!.date)
            binding?.etLocation?.setText(myHappyPlaceDetails!!.location)
            myLatitude = myHappyPlaceDetails!!.latitude
            myLongitude = myHappyPlaceDetails!!.longitude

            saveImageToInternalStorage = Uri.parse(myHappyPlaceDetails!!.image)
            binding?.ivPlaceImage?.setImageURI(saveImageToInternalStorage)
            binding?.btnSave?.text = getString(R.string.update)
        }


        binding?.etDate?.setOnClickListener(this)
        binding?.tvAddImage?.setOnClickListener(this)
        registerOnActivityForResult()
        binding?.btnSave?.setOnClickListener(this)

        binding?.etLocation?.setOnClickListener(this)
        binding?.tvSelectCurrentLocation?.setOnClickListener(this)
    }

    private fun isLocationEnabled(): Boolean{
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun addRecord(happyPlaceDao: HappyPlaceDao){
        val title = binding?.etTitle?.text.toString()
        val image = saveImageToInternalStorage.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()
        if (title.isNotEmpty()&&image.isNotEmpty()&&description.isNotEmpty()&&date.isNotEmpty()
            &&location.isNotEmpty()){
            lifecycleScope.launch {
                happyPlaceDao.insert(
                    HappyPlaceEntity(title = title, image = image, description = description,
                date = date, location = location, latitude = myLatitude, longitude = myLongitude)
                )
                Toast.makeText(applicationContext,"Record saved",Toast.LENGTH_LONG).show()
                binding?.etTitle?.text?.clear()
                binding?.etDescription?.text?.clear()
                binding?.etDate?.text?.clear()
                binding?.etLocation?.text?.clear()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        val myLocationRequest = create()
        myLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        myLocationRequest.interval = 1000
        myLocationRequest.numUpdates = 1

        Looper.myLooper()?.let {
            myFusedLocationProviderClient.requestLocationUpdates(
                myLocationRequest,myLocationCallBack, it
            )
        }

    }

    private val myLocationCallBack = object: LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val myLastLocation: Location = locationResult.lastLocation
            myLatitude = myLastLocation.latitude
            Log.i("Current Latitude", "$myLatitude")
            myLongitude = myLastLocation.longitude
            Log.i("Current Longitude", "$myLongitude")
            val addressTask =
                GetAddressFromLatLng(this@AddHappyPlaceActivity, myLatitude, myLongitude)
            addressTask.setAddressListener(object :
                GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?) {
                    Log.e("Address ::", "" + address)
                    binding?.etLocation?.setText(address)
                }

                override fun onError() {
                    Log.e("Get Address ::", "Something is wrong...")
                }
            })
            lifecycleScope.launch(Dispatchers.IO) {
                //CoroutineScope tied to this LifecycleOwner's Lifecycle.
                //This scope will be cancelled when the Lifecycle is destroyed
                addressTask.launchBackgroundProcessForRequest() //starts the task to get the address in text from the lat and lng values
            }
        }
    }

    override fun onClick(v: View?) {
        when(v!!.id){
            R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    myCalendar.get(Calendar.YEAR),
                    myCalendar.get(Calendar.MONTH),
                    myCalendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Selection Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){
                        _, which ->
                    when(which){
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save ->{
                when{
                    binding?.etTitle?.text?.isEmpty() == true -> {
                        Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
                    }
                    binding?.etDescription?.text?.isEmpty() == true -> {
                        Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT)
                            .show()
                    }
                    binding?.etDate?.text?.isEmpty() == true -> {
                        Toast.makeText(this, "Please enter date", Toast.LENGTH_SHORT)
                            .show()
                    }
                    binding?.etLocation?.text?.isEmpty() == true -> {
                        Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT)
                            .show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(this, "Please add image", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val happyPlaceDao = (application as HappyPlaceApp).db.happyPlaceDao()
                        if (myHappyPlaceDetails == null){
                        addRecord(happyPlaceDao)
                        finish()
                        }else{
                            updateRecord(myHappyPlaceDetails!!.id,happyPlaceDao)
                            finish()
                        }

                    }
                }
            }
            R.id.et_location -> {
                try {
                    val fields = listOf(Place.Field.ID,Place.Field.NAME,Place.Field.LAT_LNG,Place.Field.ADDRESS)
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                        .build(this)
                    resultLauncherLocation.launch(intent)
                }catch (e :Exception){
                    e.printStackTrace()
                }
            }
            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()){
                    Toast.makeText(this,
                    "Your location provider is turned off. Please turn it on.",Toast.LENGTH_LONG).show()

                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                }else{
                    Dexter.withContext(this).withPermissions(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ).withListener(object : MultiplePermissionsListener{
                        override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                            if (report!!.areAllPermissionsGranted()){
                                requestNewLocationData()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(
                            p0: MutableList<PermissionRequest>?,
                            p1: PermissionToken?
                        ) {
                            showRationalDialogForPermissions()
                        }
                    }).onSameThread()
                        .check()

                }
            }
        }
    }


    private fun updateRecord(id: Int, happyPlaceDao: HappyPlaceDao){
        val title = binding?.etTitle?.text.toString()
        val image = saveImageToInternalStorage.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()
        lifecycleScope.launch {
            happyPlaceDao.update(HappyPlaceEntity(id,title,image,description,date,location,myLatitude,myLongitude))
            Toast.makeText(applicationContext,"Record Updated",
                Toast.LENGTH_SHORT).show() } }




    private fun choosePhotoFromGallery(){
        Dexter.withContext(this)
            .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        val galleryIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        galleryImageResultLauncher.launch(galleryIntent)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this).setMessage("It looks like you have turned off permission required for this feature. It can be enabled under the applications settings.")
            .setPositiveButton("GO TO SETTINGS")
            {_,_->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName,null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){ dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    private fun updateDateInView(){
        val myFormat = "yyyy.MM.dd"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(myCalendar.time).toString())
    }

    @Suppress("DEPRECATION")
    private fun registerOnActivityForResult() {
        galleryImageResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    if (data != null) {
                        val contentUri = data.data
                        try {

                            val selectedImageBitmap: Bitmap = if (Build.VERSION.SDK_INT < 28){
                                MediaStore.Images.Media.getBitmap(this.contentResolver,contentUri)
                            }else{
                                val source:ImageDecoder.Source = ImageDecoder.createSource(this.contentResolver,contentUri!!)
                                ImageDecoder.decodeBitmap(source)
                            }

                            saveImageToInternalStorage = saveImageToInternalStorage(selectedImageBitmap)
                            Log.e("Saved Image : ", "Path :: $saveImageToInternalStorage")

                            binding?.ivPlaceImage?.setImageURI(contentUri)
                        } catch (e: IOException) {
                            e.printStackTrace()
                            Toast.makeText(
                                this,
                                "Failed to load image from gallery",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        resultLauncherCamera =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // There are no request codes
                    val data: Intent? = result.data

                    val thumbNail: Bitmap = data!!.extras?.get("data") as Bitmap
                    saveImageToInternalStorage = saveImageToInternalStorage(thumbNail)
                    Log.e("Saved Image : ", "Path :: $saveImageToInternalStorage")
                    binding?.ivPlaceImage?.setImageBitmap(thumbNail)
                }
            }

        resultLauncherLocation = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val place: Place = Autocomplete.getPlaceFromIntent(data!!)
                binding?.etLocation?.setText(place.address)
                myLatitude = place.latLng!!.latitude
                myLongitude = place.latLng!!.longitude
            }
        }
    }



    private fun takePhotoFromCamera() {
        Dexter.withContext(this).withPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ).withListener(object : MultiplePermissionsListener {
            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                if (report!!.areAllPermissionsGranted()) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    resultLauncherCamera.launch(cameraIntent)
                }
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>,
                token: PermissionToken
            ) {
                showRationalDialogForPermissions()
            }
        }).onSameThread().check()
    }
    private fun saveImageToInternalStorage(bitmap: Bitmap):Uri{
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY,Context.MODE_PRIVATE)
        file = File(file,"${UUID.randomUUID()}.jpg")
        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG,100,stream)
            stream.flush()
            stream.close()
        }catch (e: IOException){
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    companion object{
        private const val IMAGE_DIRECTORY = "HappyPlacesImages"

    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}