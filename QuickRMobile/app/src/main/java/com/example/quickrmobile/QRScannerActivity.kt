package com.example.quickrmobile

import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.w3c.dom.Document
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.regex.Pattern

private const val CAMERA_REQUEST_CODE = 101
private const val USERS_KEY = "users"
private const val STUDENT_ID_KEY = "student_id"
private const val SESSIONS_KEY = "sessions"
private const val SESSION_ID_KEY = "session_id"
private const val MODULE_CODE_KEY = "module_code"
private const val DAY_KEY = "day"
private const val START_TIME_KEY = "start_time"
private const val END_TIME_KEY = "end_time"
private const val ENROLLED_SESSION_IDS_KEY = "enrolled_session_ids"
private const val ATTENDED_SESSION_LOG_IDS_KEY = "attended_session_log_ids"
private const val ATTENDED_SESSION_LOG_ID_KEY = "log_id"
private const val ATTENDED_SESSION_LOGS_KEY = "AttendedSessionLogs"
private const val DATE_ATTENDED_KEY = "date_attended"
private const val LATE_KEY = "late"
private const val LOG_ID_KEY = "log_id"
private const val WEEK_ATTENDED_KEY = "week_attended"
private const val ACADEMIC_YEAR_START_DATE = "2021-09-20"

// AUTHOR: Kristopher J Randle
// VERSION: 1.18
class QRScannerActivity : AppCompatActivity()
{
    private lateinit var loggedInStudentID: String

    private lateinit var tvtextview: TextView
    private lateinit var scanbutton: Button
    private lateinit var scannerview: CodeScannerView
    private lateinit var codeScanner: CodeScanner

    private lateinit var studentDocumentSnapshot: DocumentSnapshot
    private val sessionsDocuments: MutableList<DocumentSnapshot> = mutableListOf<DocumentSnapshot>()
    private val attendedSessionDocuments: MutableList<DocumentSnapshot> = mutableListOf<DocumentSnapshot>()
    private val fireStore = FirebaseFirestore.getInstance()

    private lateinit var splitQRCode: List<String>

    private lateinit var qrModuleCode: String
    private lateinit var qrDate: LocalDate
    private lateinit var qrTime: LocalTime

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        studentDocumentSnapshot = MainActivity.getStudentDocumentSnapshot()
        loggedInStudentID = studentDocumentSnapshot.get(STUDENT_ID_KEY).toString()

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

        if(it.text.contains("_"))
        {
            splitQRCode = it.text.split("_")
            validateQRCode()
        }
        else
        {
            Toast.makeText(this, "Please scan a valid QuickR code.", Toast.LENGTH_LONG).show()
        }
    }

    private fun validateQRCode()
    {
        val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        var dateValid = false
        var timeValid = false

        // CHECK the date format in the QR matches the QuickR format
        dateValid = try
        {
            LocalDate.parse(splitQRCode[1], dateFormat)
            true
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            false
        }
        // CHECK the time format in the QR matches the QuickR format
        timeValid = try
        {
            LocalTime.parse(splitQRCode[2], DateTimeFormatter.ofPattern("HH:mm:ss"))
            true
        }
        catch (e: Exception)
        {
            e.printStackTrace()
            false
        }

        if(dateValid && timeValid)
        {
            qrModuleCode = splitQRCode[0]
            qrDate = LocalDate.parse(splitQRCode[1], dateFormat)
            qrTime = LocalTime.parse(splitQRCode[2])

            Log.w(TAG, splitQRCode.toString())
            Log.w(TAG, qrDate.toString())
            Log.w(TAG, qrTime.toString())

            // CHECK the QR code isn't older than 10 seconds:
            if(LocalTime.now().isAfter(qrTime.plus(Duration.ofSeconds(10))))
            {
                Toast.makeText(this, "That QR code was created more than 10 seconds ago!", Toast.LENGTH_LONG).show()
                return
            }
            else
            {
                // RETRIEVE the enrolled session ids for the student:
                val enrolledSessionIds: List<Int> = studentDocumentSnapshot.get(ENROLLED_SESSION_IDS_KEY) as List<Int> // this works!! - retrieves List<Int> = [1,2,3,4]
                // GET all of the SESSION documents that the USER is enrolled on
                retrieveSessionDocuments(enrolledSessionIds)
            }
        }
        else
        {
            Toast.makeText(this, "Please scan a valid QuickR code.", Toast.LENGTH_LONG).show()
        }
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
                val attendedSessionLogIds: List<Int> = studentDocumentSnapshot.get(ATTENDED_SESSION_LOG_IDS_KEY) as List<Int> // Functioning fine, all active student log ids are retrieved
                retrieveAttendedSessionDocuments(attendedSessionLogIds)
            }
    }

    private fun retrieveAttendedSessionDocuments(ids: List<Int>)
    {
        val attendedSessionsRef = fireStore.collection(ATTENDED_SESSION_LOGS_KEY)
        val formattedQRDate = qrDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        // GET all of the active students attendance logs:
        attendedSessionsRef
            .whereIn(ATTENDED_SESSION_LOG_ID_KEY, ids)
            .get()
            .addOnSuccessListener { documents ->
                for(document in documents) {
                    attendedSessionDocuments.add(document)
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener{ exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
            .addOnCompleteListener {
                for(document in attendedSessionDocuments)
                {
                    if(document.getString(DATE_ATTENDED_KEY) != formattedQRDate.toString())
                    {
                        // REMOVE all session logs apart from today's to filter out the data:
                        attendedSessionDocuments.remove(document)
                    }
                }

                verifySession()
            }

    }

    private fun checkDuplicateAttendance(attendedSession : DocumentSnapshot) : Boolean
    {
        var alreadyLoggedAttendance = false


        for(document in attendedSessionDocuments) // attendance log count is not 0
        {
            val string = document.getString(SESSION_ID_KEY)
            Toast.makeText(this, string, Toast.LENGTH_LONG).show()

            // CHECK if the student has already created an attendance log for the session they just scanned today
            if(document.getString(SESSION_ID_KEY)  == attendedSession.getString(SESSION_ID_KEY))
            {

                // IF it matches, the user has already logged their attendance for this session once
                alreadyLoggedAttendance = true
            }
        }

        return alreadyLoggedAttendance
    }

    private fun verifySession()
    {
        var attendedSession: DocumentSnapshot
        var validSession: Boolean = false

        for(document in sessionsDocuments){
            // FIND the SESSION documents that match the module_code scanned in the QR
            if(document.getString(MODULE_CODE_KEY) == qrModuleCode){
                // CHECK that the module session is being held today:
                if(document.getString(DAY_KEY) == LocalDate.now().dayOfWeek.toString()){
                    Log.w(TAG, "Day of the week matches!")
                    // STORE the session start and end time as LocalTimes:
                    val sessionStartTime = LocalTime.parse(document.getString(START_TIME_KEY))
                    val sessionEndTime = LocalTime.parse(document.getString(END_TIME_KEY))

                    Log.w(TAG, sessionStartTime.toString())
                    Log.w(TAG, sessionEndTime.toString())

                    // CHECK the time the QR was scanned is between the start_time and end_time of the session:
                    if(sessionStartTime.isBefore(qrTime) && sessionEndTime.isAfter(qrTime)){
                        attendedSession = document
                        // ENSURE the student has not already signed in for this session:
                        if(!checkDuplicateAttendance(attendedSession))
                        {

                            validSession = true
                            var isLate = false
                            var currentWeek = getCurrentWeek()

                            if(qrTime.isAfter(sessionStartTime.plus(Duration.ofMinutes(15)))){
                                isLate = true
                            }
                            // IF all checks pass, CREATE a new AttendedSessionLog Document to register the attendance:
                            logAttendance(attendedSession, isLate, currentWeek)
                        }
                        else
                        {
                            // Student has already signed in for this session
                            Toast.makeText(this, "You can not check-in to the same session twice!!!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }

        if(!validSession){
            Toast.makeText(this, "You don't attend this session!", Toast.LENGTH_LONG).show()
        }
    }

    private fun logAttendance(attendedSession: DocumentSnapshot, isLate: Boolean, currentWeek: Long){
        val newAttendanceLog: MutableMap<String, Any> = HashMap()
        val newLogID = generateRandomID(20)

        newAttendanceLog[DATE_ATTENDED_KEY] = qrDate.toString()
        newAttendanceLog[LATE_KEY] = isLate
        newAttendanceLog[LOG_ID_KEY] = newLogID
        newAttendanceLog[SESSION_ID_KEY] = (attendedSession.get(SESSION_ID_KEY).toString()).toInt()
        newAttendanceLog[STUDENT_ID_KEY] = loggedInStudentID.toInt()
        newAttendanceLog[WEEK_ATTENDED_KEY] = currentWeek.toInt()

        fireStore.collection(ATTENDED_SESSION_LOGS_KEY)
            .add(newAttendanceLog)
            .addOnSuccessListener {
                Toast.makeText(this, "Attendance Successfully Recorded!", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener{
                Toast.makeText(this, "Attendance Log Could Not Be Created.", Toast.LENGTH_LONG).show()
            }

        val studentDocRef = fireStore.collection(USERS_KEY).document(loggedInStudentID)
        studentDocRef.update(ATTENDED_SESSION_LOG_IDS_KEY, FieldValue.arrayUnion(newLogID))

        codeScanner.stopPreview();
    }

    private fun generateRandomID(length: Int) : String
    {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { charset.random() }
            .joinToString("")
    }

    private fun getCurrentWeek(): Long {

        val daysBetween = ChronoUnit.DAYS.between(LocalDate.parse(ACADEMIC_YEAR_START_DATE), LocalDate.now())
        return daysBetween / 7
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

            codeScanner.startPreview()
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