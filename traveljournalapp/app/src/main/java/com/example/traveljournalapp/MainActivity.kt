package com.example.traveljournalapp

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.Manifest
import android.app.Instrumentation.ActivityResult
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.util.Pair
import androidx.core.content.FileProvider
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.traveljournalapp.adapters.TaskRecyclerViewAdapter
import com.example.traveljournalapp.databinding.ActivityMainBinding
import com.example.traveljournalapp.models.Task
import com.example.traveljournalapp.utils.Status
import com.example.traveljournalapp.utils.clearEditText
import com.example.traveljournalapp.utils.longToastShow
import com.example.traveljournalapp.utils.setupDialog
import com.example.traveljournalapp.utils.validateEditText
import com.example.traveljournalapp.viewmodels.TaskViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.bumptech.glide.Glide
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val addTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.add_task_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private val updateTaskDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.update_task_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private val tripDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.trip_dialog)
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private val loadingDialog: Dialog by lazy {
        Dialog(this, R.style.DialogCustomTheme).apply {
            setupDialog(R.layout.loading_dialog)
        }
    }

    private val taskViewModel: TaskViewModel by lazy {
        ViewModelProvider(this)[TaskViewModel::class.java]
    }

    private lateinit var image : ImageView
    private var imgUri: Uri? = null

    private fun showImageFromUri(imageView: ImageView, uriString: String) {
        // Use Glide to load and display the image
        Glide.with(this)
            .load(Uri.parse(uriString))
            .into(imageView)
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isReadPermissionGranted = false
    private var isLocationPermissionGranted = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            isReadPermissionGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: isReadPermissionGranted
            isLocationPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: isLocationPermissionGranted
        }

        //add trip start
        val addCloseImg = addTaskDialog.findViewById<ImageView>(R.id.closeImg)
        addCloseImg.setOnClickListener { addTaskDialog.dismiss() }

        val addImg = addTaskDialog.findViewById<ImageView>(R.id.addImg)
        val btnAddImg = addTaskDialog.findViewById<Button>(R.id.addImgBtn)


        btnAddImg.setOnClickListener {
            image = addImg
            uploadImage(image)
        }

        val addETTitle = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val addETTitleL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)

        addETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETTitle, addETTitleL)
            }
        })

        val addETDesc = addTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val addETDescL = addTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

        addETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(addETDesc, addETDescL)
            }
        })

        val textDates = addTaskDialog.findViewById<TextView>(R.id.dateRangeTxt)
        val btnCalendar = addTaskDialog.findViewById<Button>(R.id.btn_calendar)

        btnCalendar.setOnClickListener {
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Dates")
                .setSelection(
                    Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()
            dateRangePicker.show(supportFragmentManager, "date_picker")

            dateRangePicker.addOnPositiveButtonClickListener {
                val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                textDates.text = "${simpleDateFormat.format(it.first)} - ${simpleDateFormat.format(it.second)}"
            }
        }

        val ratingBar = addTaskDialog.findViewById<RatingBar>(R.id.ratingBar)
        ratingBar.stepSize = 0.5f

        mainBinding.addTaskFABtn.setOnClickListener {
            clearEditText(addETTitle, addETTitleL)
            clearEditText(addETDesc, addETDescL)
            textDates.text = "No selected dates"
            ratingBar.rating = 0.0f
            addImg.setImageResource(R.drawable.default_image)
            addTaskDialog.show()
        }

        val saveTaskBtn = addTaskDialog.findViewById<Button>(R.id.saveTaskBtn)
        saveTaskBtn.setOnClickListener {
            if (validateEditText(addETTitle, addETTitleL)
                && validateEditText(addETDesc, addETDescL)
            ) {
                addTaskDialog.dismiss()
                val newTask = Task(
                    UUID.randomUUID().toString(),
                    addETTitle.text.toString().trim(),
                    addETDesc.text.toString().trim(),
                    textDates.text.toString().trim(),
                    ratingBar.rating,
                    Date(),
                    imgUri.toString().trim()
                )
                taskViewModel.insertTask(newTask).observe(this) {
                    when (it.status) {
                        Status.LOADING -> {
                            loadingDialog.show()
                        }

                        Status.SUCCESS -> {
                            loadingDialog.dismiss()
                            if (it.data?.toInt() != -1) {
                                longToastShow("Task Added Successfully")
                            }
                        }

                        Status.ERROR -> {
                            loadingDialog.dismiss()
                            it.message?.let { it1 -> longToastShow(it1) }
                        }
                    }
                }
            }
        }
        //add trip end

        //update trip start
        val updateETTitle = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskTitle)
        val updateETTitleL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskTitleL)

        updateETTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETTitle, updateETTitleL)
            }
        })

        val updateETDesc = updateTaskDialog.findViewById<TextInputEditText>(R.id.edTaskDesc)
        val updateETDescL = updateTaskDialog.findViewById<TextInputLayout>(R.id.edTaskDescL)

        updateETDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable) {
                validateEditText(updateETDesc, updateETDescL)
            }
        })

        val updateImg = updateTaskDialog.findViewById<ImageView>(R.id.addImg)
        val btnUpdateImg = updateTaskDialog.findViewById<Button>(R.id.addImgBtn)


        btnUpdateImg.setOnClickListener {
            image = updateImg
            uploadImage(image)
        }

        val updateCloseImg = updateTaskDialog.findViewById<ImageView>(R.id.closeImg)
        updateCloseImg.setOnClickListener { updateTaskDialog.dismiss() }

        val updateTextDates = updateTaskDialog.findViewById<TextView>(R.id.dateRangeTxt)
        val updateBtnCalendar = updateTaskDialog.findViewById<Button>(R.id.btn_calendar)

        updateBtnCalendar.setOnClickListener {
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Dates")
                .setSelection(
                    Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()
            dateRangePicker.show(supportFragmentManager, "date_picker")

            dateRangePicker.addOnPositiveButtonClickListener {
                val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                updateTextDates.text = "${simpleDateFormat.format(it.first)} - ${simpleDateFormat.format(it.second)}"
            }
        }

        val updateRatingBar = updateTaskDialog.findViewById<RatingBar>(R.id.ratingBar)
        updateRatingBar.stepSize = 0.5f

        val updateTaskBtn = updateTaskDialog.findViewById<Button>(R.id.updateTaskBtn)

        // update trip end

        //each trip
        val tripCloseImg = tripDialog.findViewById<ImageView>(R.id.closeImg)
        tripCloseImg.setOnClickListener { tripDialog.dismiss() }

        val tripImage = tripDialog.findViewById<ImageView>(R.id.tripImg)
        val tripTitle = tripDialog.findViewById<TextView>(R.id.titleTrip)
        val tripRating = tripDialog.findViewById<RatingBar>(R.id.ratingBarTrip)
        val tripDateRange = tripDialog.findViewById<TextView>(R.id.datesRangeTrip)
        val tripDescTxt = tripDialog.findViewById<TextView>(R.id.descTrip)
        val tripAddTxt = tripDialog.findViewById<TextView>(R.id.dateAddedTxt)

        //each trip end

        val taskRecyclerViewAdapter = TaskRecyclerViewAdapter (
            deleteUpdateCallback = { type, position, task ->
                if (type == "delete") {
                    taskViewModel
                        //  .deleteTask(task)
                        .deleteTaskUsingId(task.id)
                        .observe(this) {
                            when (it.status) {
                                Status.LOADING -> {
                                    loadingDialog.show()
                                }

                                Status.SUCCESS -> {
                                    loadingDialog.dismiss()
                                    if (it.data != -1) {
                                        longToastShow("Trip Deleted Successfully")
                                    }
                                }

                                Status.ERROR -> {
                                    loadingDialog.dismiss()
                                    it.message?.let { it1 -> longToastShow(it1) }
                                }
                            }
                        }
                } else if (type == "update") {
                    updateETTitle.setText(task.title)
                    updateETDesc.setText(task.description)
                    updateTextDates.text = task.dateRange
                    updateRatingBar.rating = task.rating
                    showImageFromUri(updateImg, task.image)
                    updateTaskBtn.setOnClickListener {
                        if (validateEditText(updateETTitle, updateETTitleL)
                            && validateEditText(updateETDesc, updateETDescL)
                        ) {
                            val updateTask = Task(
                                task.id,
                                updateETTitle.text.toString().trim(),
                                updateETDesc.text.toString().trim(),
                                updateTextDates.text.toString().trim(),
                                updateRatingBar.rating,
                                Date(),
                                imgUri.toString().trim()
                            )
                            updateTaskDialog.dismiss()
                            loadingDialog.show()
                            taskViewModel
                                .updateTask(updateTask)
                                //.updateTaskField(
                                // task.id,
                                // updateETTitle.text.toString().trim(),
                                // updateETDesc.text.toString().trim()
                                //)
                                .observe(this) {
                                    when (it.status) {
                                        Status.LOADING -> {
                                            loadingDialog.show()
                                        }

                                        Status.SUCCESS -> {
                                            loadingDialog.dismiss()
                                            if (it.data != -1) {
                                                longToastShow("Trip Updated Successfully")
                                            }
                                        }

                                        Status.ERROR -> {
                                            loadingDialog.dismiss()
                                            it.message?.let { it1 -> longToastShow(it1) }
                                        }
                                    }
                                }
                        }
                    }
                    updateTaskDialog.show()
                }
            },
            showTripDialogCallback = { task ->
                //showTripDialog(task)

                showImageFromUri(tripImage, task.image)
                tripTitle.text = task.title
                tripRating.rating = task.rating
                tripDateRange.text = task.dateRange
                tripDescTxt.text = task.description

                val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss a", Locale.getDefault())
                tripAddTxt.text = dateFormat.format(task.date)


                tripDialog.show()

            }
        )
        mainBinding.taskRV.adapter = taskRecyclerViewAdapter

        callGetTaskList(taskRecyclerViewAdapter)

        requestPermission()
    }

    private fun requestPermission() {
        isReadPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED

        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val permissionRequest : MutableList<String> = ArrayList()

        if(!isReadPermissionGranted) {
            permissionRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
        }

        if(!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if(permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }


    private fun uploadImage(image: ImageView?) {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.type = "image/*"
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 1) {
            val imageUri: Uri? = data?.data

            image.setImageURI(imageUri)
            imgUri = imageUri
        }
    }


    //private fun showTripDialog(task: Task) {
        //val tripImage = tripDialog.findViewById<ImageView>(R.id.tripImg)
        //val tripTitle = tripDialog.findViewById<TextView>(R.id.titleTrip)
        //val tripRating = tripDialog.findViewById<RatingBar>(R.id.ratingBarTrip)
        //val tripDateRange = tripDialog.findViewById<TextView>(R.id.datesRangeTrip)
        //val tripDescTxt = tripDialog.findViewById<TextView>(R.id.descTrip)
        //val tripAddTxt = tripDialog.findViewById<TextView>(R.id.dateAddedTxt)

        //showImageFromUri(tripImage, task.image)
        //tripTitle.text = task.title
        //tripRating.rating = task.rating
        //tripDateRange.text = task.dateRange
        //tripDescTxt.text = task.description

        //val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss a", Locale.getDefault())
        //tripAddTxt.text = dateFormat.format(task.date)


        //tripDialog.show()
    //}

    private fun callGetTaskList(taskRecyclerViewAdapter:TaskRecyclerViewAdapter){
        loadingDialog.show()
        CoroutineScope(Dispatchers.Main).launch {
            taskViewModel.getTaskList().collect {
                when (it.status) {
                    Status.LOADING -> {
                        loadingDialog.show()
                    }

                    Status.SUCCESS -> {
                        it.data?.collect { taskList ->
                            loadingDialog.dismiss()
                            taskRecyclerViewAdapter.addAllTask(taskList)
                        }
                    }

                    Status.ERROR -> {
                        loadingDialog.dismiss()
                        it.message?.let { it1 -> longToastShow(it1) }
                    }
                }
            }
        }
    }
}







