package com.example.quickrmobile

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.BoringLayout
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.inject.Deferred
import java.sql.Time
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDate.of
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.jar.Manifest
import javax.xml.transform.Result


private const val CAMERA_REQUEST_CODE = 101
private const val STUDENT_ID_KEY = "student_id"
private const val SESSIONS_KEY = "sessions"
private const val SESSION_ID_KEY = "session_id"
private const val MODULE_CODE_KEY = "module_code"
private const val DAY_KEY = "day"
private const val ENROLLED_SESSION_IDS_KEY = "enrolled_session_ids"

// AUTHOR: Kristopher J Randle
// VERSION: 1.14
class QRScannerActivity : AppCompatActivity()
{
    private lateinit var loggedInStudentID: String

    private lateinit var tvtextview: TextView
    private lateinit var scanbutton: Button
    private lateinit var scannerview: CodeScannerView
    private lateinit var codeScanner: CodeScanner

    private lateinit var studentDocumentSnapshot: DocumentSnapshot
    private val sessionsDocuments: MutableList<DocumentSnapshot> = mutableListOf<DocumentSnapshot>()

    private val fireStore = FirebaseFirestore.getInstance()

    private lateinit var splitQRCode: List<String>

    private lateinit var qrModuleCode: String
    private lateinit var qrDate: LocalDate
    private lateinit var qrTime: LocalTime

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loggedInStudentID = intent.getStringExtra(STUDENT_ID_KEY).toString()
        studentDocumentSnapshot = MainActivity.getStudentDocumentSnapshot()

        tvtextview = findViewById(R.id.tv_textView)
        scanbutton = findViewById(R.id.scan_button)
        scannerview = findViewById(R.id.scanner_view)
        scannerview.visibility = View.GONE

        codeScanner = CodeScanner(this, scannerview)

        scanbutton.setOnClickListener{
            startScanner()
        }
    }

    private fun deconstructQRCode(it: com.google.zxing.Result)
    {
        tvtextview.text = it.text
        splitQRCode = it.text.split("_")


        qrModuleCode = splitQRCode[0]

        var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        var date = LocalDate.parse(splitQRCode[1], formatter)

        Log.w(TAG, splitQRCode.toString())
        Log.w(TAG, date.toString())

    }

    private fun validateQRCode(it: com.google.zxing.Result)
    {
        // RETRIEVE the enrolled session ids for the student:
        val enrolledSessionIds: List<Int> = studentDocumentSnapshot.get(ENROLLED_SESSION_IDS_KEY) as List<Int> // this works!! - retrieves List<Int> = [1,2,3,4]
        // GET all of the SESSION documents that the USER is enrolled on
        retrieveSessionDocuments(enrolledSessionIds)
        // FIND the SESSION document that matches the module_code scanned in the QR



        // STORE the day of that SESSION and check it matches the current day
        // STORE the start_time and end_time of the SESSION and check the current time is between them
        // IF all checks pass, CREATE a new AttendedSessionLog Document to register the attendance

    }

    private fun retrieveSessionDocuments(ids: List<Int>)
    {
        val sessionsRef = fireStore.collection(SESSIONS_KEY)

        sessionsRef
            .whereIn(SESSION_ID_KEY, ids)
            .get()
            .addOnSuccessListener { documents ->
                for(document in documents) {
                    sessionsDocuments.add(document)
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener{ exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener {
                verifySession()
            }
    }

    private fun verifySession()
    {
       // for(document in sessionsDocuments){
       //     if(document.getString(MODULE_CODE_KEY) == )
       // }
    }

    private fun startScanner()
    {
        setupPermissions()
        startCodeScanner()
    }

    private fun startCodeScanner()
    {
        scannerview.visibility = View.VISIBLE
        tvtextview.visibility = View.VISIBLE
        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.SINGLE
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback {
                runOnUiThread{
                    scannerview.visibility = View.GONE
                    deconstructQRCode(it)
                }
            }

            errorCallback = ErrorCallback {
                runOnUiThread{
                    Log.e("Main", "Camera initialisation error: ${it.message}")
                }
            }
            scannerview.setOnClickListener{
                codeScanner.startPreview()
            }
        }
    }

    override fun onResume(){
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun setupPermissions(){
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)

        if(permission != PackageManager.PERMISSION_GRANTED){
            makeRequest()
        }
    }

    private fun makeRequest(){
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode){
            CAMERA_REQUEST_CODE -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "You need the camera permission to be able to use this app!", Toast.LENGTH_SHORT).show()
                }
                else{
                    // successful
                }
            }
        }
    }

}