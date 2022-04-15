package com.example.quickrmobile

import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.budiyev.android.codescanner.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.lang.Exception
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*

private const val CAMERA_REQUEST_CODE = 101
private const val USERS_KEY = "users"
private const val STUDENT_ID_KEY = "student_id"
private const val STUDENT_NAME_KEY = "student_name"
private const val SESSIONS_KEY = "sessions"
private const val SESSION_ID_KEY = "session_id"
private const val MODULE_CODE_KEY = "module_code"
private const val SESSION_TUTOR_KEY = "session_tutor"
private const val DAY_KEY = "day"
private const val START_WEEK_KEY = "start_week"
private const val END_WEEK_KEY = "end_week"
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
private const val MINUTES_LATE_KEY = "minutes_late"

// AUTHOR: Kristopher J Randle
// VERSION: 1.24
// CodeScanner Author: yuriy-budiyev - https://github.com/yuriy-budiyev/code-scanner
class QRScannerActivity : AppCompatActivity()
{
    private lateinit var loggedInStudentID: String

    private lateinit var tvTextView: TextView
    private lateinit var tvAttendance: TextView
    private lateinit var tvPunctuality: TextView
    private lateinit var tvCurrentSession: TextView
    private lateinit var tvStudentNameAndNumber: TextView
    private lateinit var tvAttendancePercentage: TextView
    private lateinit var tvPunctualityPercentage: TextView
    private lateinit var tvModuleCode: TextView
    private lateinit var tvSessionDate: TextView
    private lateinit var tvSessionTime: TextView
    private lateinit var tvSessionTutor: TextView

    private lateinit var imageViewAttendanceIcon: ImageView
    private lateinit var imageViewPercentageIcon: ImageView
    private lateinit var imageViewAttendedIcon: ImageView

    private lateinit var scanbutton: Button
    private lateinit var scannerview: CodeScannerView
    private lateinit var codeScanner: CodeScanner

    private lateinit var studentDocumentSnapshot: DocumentSnapshot
    private lateinit var currentSessionDocumentSnapshot: DocumentSnapshot
    private val sessionsDocuments: MutableList<DocumentSnapshot> = mutableListOf<DocumentSnapshot>()
    private val attendedSessionDocuments: MutableList<DocumentSnapshot> = mutableListOf<DocumentSnapshot>()
    private val fireStore = FirebaseFirestore.getInstance()

    private lateinit var splitQRCode: List<String>

    private lateinit var qrModuleCode: String
    private lateinit var qrDate: LocalDate
    private lateinit var qrTime: LocalTime
    private var qrWaiverLatePenalty = false
    private var minutesLate = 0

    private var noAttendedSession = false
    private var scannerStarted = false

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        studentDocumentSnapshot = MainActivity.getStudentDocumentSnapshot()
        loggedInStudentID = studentDocumentSnapshot.get(STUDENT_ID_KEY).toString()

        tvTextView = findViewById(R.id.tvTextView)
        tvAttendance = findViewById(R.id.textViewAttendance)
        tvPunctuality = findViewById(R.id.textViewPunctuality)
        tvCurrentSession = findViewById(R.id.textViewCurrentSession)
        tvStudentNameAndNumber = findViewById(R.id.textViewStudentNameAndNumber)
        tvAttendancePercentage = findViewById(R.id.textViewAttendancePercentage)
        tvPunctualityPercentage = findViewById(R.id.textViewPunctualityPercentage)
        tvModuleCode = findViewById(R.id.textViewModuleCode)
        tvSessionDate = findViewById(R.id.textViewSessionDate)
        tvSessionTime = findViewById(R.id.textViewSessionTime)
        tvSessionTutor = findViewById(R.id.textViewSessionTutor)

        imageViewAttendanceIcon = findViewById(R.id.imageViewAttendance)
        imageViewPercentageIcon = findViewById(R.id.imageViewPunctuality)
        imageViewAttendedIcon = findViewById(R.id.imageViewAttendedIcon)

        scanbutton = findViewById(R.id.scan_button)
        scannerview = findViewById(R.id.scanner_view)
        scannerview.visibility = View.GONE

        codeScanner = CodeScanner(this, scannerview)

        tvStudentNameAndNumber.text = "Welcome, " + studentDocumentSnapshot.getString(STUDENT_NAME_KEY) + " (" + studentDocumentSnapshot.get(STUDENT_ID_KEY).toString() + ")"
        tvStudentNameAndNumber.setTypeface(null, Typeface.BOLD)

        initialiseMetrics()

        scanbutton.setOnClickListener{
            if(!scannerStarted)
            {
                startScanner()
                hideUIElements()
            }
            else
            {
                onResume()
                scannerview.visibility = View.VISIBLE
                hideUIElements()
            }
        }
    }

    private fun hideUIElements()
    {
        tvAttendance.visibility = View.GONE
        tvPunctuality.visibility = View.GONE
        tvCurrentSession.visibility = View.GONE
        tvStudentNameAndNumber.visibility = View.GONE
        tvAttendancePercentage.visibility = View.GONE
        tvPunctualityPercentage.visibility = View.GONE
        tvModuleCode.visibility = View.GONE
        tvSessionDate.visibility = View.GONE
        tvSessionTime.visibility = View.GONE
        tvSessionTutor.visibility = View.GONE
        imageViewAttendanceIcon.visibility = View.GONE
        imageViewPercentageIcon.visibility = View.GONE
        imageViewAttendedIcon.visibility = View.GONE
    }

    private fun showUIElements()
    {
        tvAttendance.visibility = View.VISIBLE
        tvPunctuality.visibility = View.VISIBLE
        tvCurrentSession.visibility = View.VISIBLE
        tvStudentNameAndNumber.visibility = View.VISIBLE
        tvAttendancePercentage.visibility = View.VISIBLE
        tvPunctualityPercentage.visibility = View.VISIBLE
        tvModuleCode.visibility = View.VISIBLE
        tvSessionDate.visibility = View.VISIBLE
        tvSessionTime.visibility = View.VISIBLE
        tvSessionTutor.visibility = View.VISIBLE
        imageViewAttendanceIcon.visibility = View.VISIBLE
        imageViewPercentageIcon.visibility = View.VISIBLE
        imageViewAttendedIcon.visibility = View.VISIBLE
    }

    private fun initialiseMetrics()
    {
        // RETRIEVE the enrolled session ids for the student:
        val enrolledSessionIds: List<Int> = studentDocumentSnapshot.get(ENROLLED_SESSION_IDS_KEY) as List<Int> // this works!! - retrieves List<Int> = [1,2,3,4]

        // GET all of the SESSION documents that the USER is enrolled on
        retrieveSessionDocuments(enrolledSessionIds)
    }

    private fun updateMetrics()
    {
        tvAttendancePercentage.text = calculateAttendance().toString() + "%"
        tvPunctualityPercentage.text = calculatePunctuality().toString() + "%"

        if(::currentSessionDocumentSnapshot.isInitialized)
        {

            tvModuleCode.text = currentSessionDocumentSnapshot.getString(MODULE_CODE_KEY)
            tvSessionDate.text = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)).toString()
            tvSessionTime.text = currentSessionDocumentSnapshot.getString(START_TIME_KEY) + " - " + currentSessionDocumentSnapshot.getString(END_TIME_KEY)
            tvSessionTutor.text = currentSessionDocumentSnapshot.getString(SESSION_TUTOR_KEY)

        }
        else
        {
            tvModuleCode.text = ""
            tvSessionDate.text = "No timetabled session"
            tvSessionTime.text = ""
            tvSessionTutor.text = ""
        }
    }

    private fun calculateAttendance(): Int
    {
        var totalNumberOfTimetabledSessions = 0f
        val currentAcademicWeek = getCurrentWeek()

        for (document in sessionsDocuments)
        {
            val sessionStartWeek = (document.get(START_WEEK_KEY) as Long)
            // currentWeek - sessionStartWeek = expectedNumber of logs for each session
            totalNumberOfTimetabledSessions += currentAcademicWeek - sessionStartWeek
        }

        // actualNumberOfLogs / expectedNumberOfLogs = attendance
        val attendancePercentage = (attendedSessionDocuments.size / totalNumberOfTimetabledSessions) * 100

        return attendancePercentage.toInt()
    }

    private fun calculatePunctuality() : Int
    {
        var numberOfLateLogs = 0f

        for(document in attendedSessionDocuments)
        {
            // count all of the attendance logs that are marked as 'late'
            if(document.get(LATE_KEY) as Boolean)
            {
                numberOfLateLogs++
            }
        }

        // Punctuality = (number of late logs / total logs) * 100
        val punctualityPercentage = (numberOfLateLogs / attendedSessionDocuments.size) * 100

        return punctualityPercentage.toInt()
    }

    private fun retrieveSessionDocuments(ids: List<Int>)
    {
        var sessionsRef = fireStore.collection(SESSIONS_KEY)

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
                for(document in sessionsDocuments)
                {
                    if(document.getString(DAY_KEY) == LocalDate.now().dayOfWeek.toString())
                    {
                        val sessionStartTime = LocalTime.parse(document.getString(START_TIME_KEY))
                        val sessionEndTime = LocalTime.parse(document.getString(END_TIME_KEY))

                        Log.w(TAG, sessionStartTime.toString())
                        Log.w(TAG, sessionEndTime.toString())

                        if (sessionStartTime.isBefore(LocalTime.now()) && sessionEndTime.isAfter(LocalTime.now()))
                        {
                            currentSessionDocumentSnapshot = document

                        }


                    }
                }

                val attendedSessionLogIds: List<Int> = studentDocumentSnapshot.get(ATTENDED_SESSION_LOG_IDS_KEY) as List<Int> // Functioning fine, all active student log ids are retrieved
                if(attendedSessionLogIds.count() == 0)
                {
                    // SKIP the attended sessions check and verify the session:
                    noAttendedSession = true
                }
                else
                {
                    retrieveAttendedSessionDocuments(attendedSessionLogIds)
                }
            }
    }

    private fun retrieveAttendedSessionDocuments(ids: List<Int>)
    {
        var attendedSessionsRef = fireStore.collection(ATTENDED_SESSION_LOGS_KEY)

        //val formattedQRDate = qrDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
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

                updateMetrics()

                // CHECK to see if the student has already checked into the current session
                if(::currentSessionDocumentSnapshot.isInitialized)
                {
                    if(checkDuplicateAttendance(currentSessionDocumentSnapshot))
                    {
                        // IF they have an attendance log for the current session, update to GUI with a tick icon
                        imageViewAttendedIcon.setImageResource(R.drawable.tick)
                    }

                }
                else
                {
                    // IF they dont have an attendance log for the current session, update to GUI with a cross icon
                    imageViewAttendedIcon.setImageResource(R.drawable.cross)
                }
            }

    }

    private fun deconstructQRCode(it: com.google.zxing.Result)
    {
        tvTextView.text = it.text

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
            if(splitQRCode[3] == "T")
            {
                qrWaiverLatePenalty = true;
            }

            Log.w(TAG, splitQRCode.toString())
            Log.w(TAG, qrDate.toString())
            Log.w(TAG, qrTime.toString())
            Log.w(TAG, qrWaiverLatePenalty.toString())

            // CHECK the QR code isn't older than 10 seconds:
            if(LocalTime.now().isAfter(qrTime.plus(Duration.ofSeconds(10))))
            {
                Toast.makeText(this, "That QR code was created more than 10 seconds ago!", Toast.LENGTH_LONG).show()
                return
            }
            else
            {
                verifySession()
            }
        }
        else
        {
            Toast.makeText(this, "Please scan a valid QuickR code.", Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshStudentDocument() {
        var studentRef =
            FirebaseFirestore.getInstance().collection(USERS_KEY).document(loggedInStudentID)

        studentRef.get().addOnSuccessListener { document ->
            if (document != null) {
                if (document.exists()) {
                    studentDocumentSnapshot = document

                    // refresh the attendance log records now the student document has been refreshed
                    attendedSessionDocuments.clear()
                    retrieveAttendedSessionDocuments(studentDocumentSnapshot.get(ATTENDED_SESSION_LOG_IDS_KEY) as List<Int>)
                }
            }
        }
    }

    private fun checkDuplicateAttendance(attendedSession : DocumentSnapshot) : Boolean
    {
        var alreadyLoggedAttendance = false


        for(document in attendedSessionDocuments) // attendance log count is not 0
        {
            val string = document.get(SESSION_ID_KEY).toString()

            // CHECK if the student has already created an attendance log for the session they just scanned today
            if(document.get(SESSION_ID_KEY)  == attendedSession.get(SESSION_ID_KEY))
            {
                if(document.get(DATE_ATTENDED_KEY).toString() == LocalDate.now().toString())
                {
                    // IF it matches, the user has already logged their attendance for this session once
                    alreadyLoggedAttendance = true
                }
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
            if(document.getString(MODULE_CODE_KEY) == qrModuleCode)
            {
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
                        // ONLY check for duplicate attendance logs if the student has previously attended sessions:
                        if(noAttendedSession)
                        {
                            minutesLate = qrTime.minute - sessionStartTime.minute

                            validSession = true
                            var isLate = false
                            var currentWeek = getCurrentWeek()
                            // ONLY register as being late if 15 minutes have passed and the late penalty has NOT been waivered:
                            if(qrTime.isAfter(sessionStartTime.plus(Duration.ofMinutes(15))) && !qrWaiverLatePenalty){
                                isLate = true
                                Toast.makeText(this, "You are $minutesLate minutes late", Toast.LENGTH_LONG).show()
                            }
                            // IF all checks pass, CREATE a new AttendedSessionLog Document to register the attendance:
                            logAttendance(attendedSession, isLate, currentWeek)

                        }
                        else
                        {
                            // ENSURE the student has not already signed in for this session:
                            if(!checkDuplicateAttendance(attendedSession))
                            {
                                minutesLate = qrTime.minute - sessionStartTime.minute
                                Toast.makeText(this, "You are $minutesLate minutes late", Toast.LENGTH_LONG).show()
                                validSession = true
                                var isLate = false
                                var currentWeek = getCurrentWeek()
                                // ONLY register as being late if 15 minutes have passed and the late penalty has NOT been waivered:
                                if(qrTime.isAfter(sessionStartTime.plus(Duration.ofMinutes(15))) && !qrWaiverLatePenalty){
                                    isLate = true
                                }
                                // IF all checks pass, CREATE a new AttendedSessionLog Document to register the attendance:
                                logAttendance(attendedSession, isLate, currentWeek)
                            }
                            else
                            {
                                validSession = true
                                // Student has already signed in for this session
                                Toast.makeText(this, "You can't check-in to the same session twice!", Toast.LENGTH_LONG).show()

                            }
                        }
                    }
                }
            }
        }

        if(!validSession){
            Toast.makeText(this, "You don't attend this session!", Toast.LENGTH_LONG).show()

        }
        onPause()
    }

    private fun logAttendance(attendedSession: DocumentSnapshot, isLate: Boolean, currentWeek: Long){
        var newAttendanceLog: MutableMap<String, Any> = HashMap()
        var newLogID = generateRandomID(20)

        newAttendanceLog[DATE_ATTENDED_KEY] = qrDate.toString()
        newAttendanceLog[LATE_KEY] = isLate
        newAttendanceLog[MINUTES_LATE_KEY] = minutesLate
        newAttendanceLog[LOG_ID_KEY] = newLogID
        newAttendanceLog[SESSION_ID_KEY] = (attendedSession.get(SESSION_ID_KEY).toString()).toInt()
        newAttendanceLog[STUDENT_ID_KEY] = loggedInStudentID.toInt()
        newAttendanceLog[WEEK_ATTENDED_KEY] = currentWeek.toInt()

        fireStore.collection(ATTENDED_SESSION_LOGS_KEY)
            .add(newAttendanceLog)
            .addOnSuccessListener {
                Toast.makeText(this, "Attendance Successfully Recorded!", Toast.LENGTH_LONG).show()
                noAttendedSession = false
                // update to GUI with a tick icon
                imageViewAttendedIcon.setImageResource(R.drawable.tick)
                // update the students attendance log records
                val studentDocRef = fireStore.collection(USERS_KEY).document(loggedInStudentID)
                studentDocRef.update(ATTENDED_SESSION_LOG_IDS_KEY, FieldValue.arrayUnion(newLogID))
                // update the reference to the student document by refreshing it
                refreshStudentDocument()
            }
            .addOnFailureListener{
                Toast.makeText(this, "Attendance Log Could Not Be Created.", Toast.LENGTH_LONG).show()
            }


        newAttendanceLog.clear()

        onPause()
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
        scannerStarted = true
        scannerview.visibility = View.VISIBLE
        tvTextView.visibility = View.VISIBLE
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
                    showUIElements()
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