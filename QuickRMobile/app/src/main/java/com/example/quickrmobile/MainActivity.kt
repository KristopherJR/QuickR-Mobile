package com.example.quickrmobile

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import java.util.jar.Manifest

private const val STUDENT_ID_KEY = "users"
private const val PASSWORD_KEY = "password"

class MainActivity : AppCompatActivity()
{
    private lateinit var loginButton: Button
    private lateinit var studentIDText: TextView
    private lateinit var passwordText: TextView

    private var idValid: Boolean = false
    private var passwordValid: Boolean = false

    private lateinit var mDocRef : DocumentReference

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)
        loginButton = findViewById(R.id.login_button)
        studentIDText = findViewById(R.id.textbox_studentID)
        passwordText = findViewById(R.id.textbox_password)


        loginButton.setOnClickListener{
            checkLoginDetails()
        }
    }

    private fun checkLoginDetails(){


        if(studentIDText.text.count() == 8 && studentIDText.text.matches("^[0-9]+\$".toRegex())){
            idValid = true
        }
        else{
            idValid = false
            Toast.makeText(this, "Please Enter a valid ID.", Toast.LENGTH_SHORT).show()
        }

        if(passwordText.text.toString() != ""){
            passwordValid = true
        }
        else{
            passwordValid = false
            Toast.makeText(this, "Please Enter a Password.", Toast.LENGTH_SHORT).show()
        }

        if(idValid && passwordValid){

            mDocRef = FirebaseFirestore.getInstance().collection("users").document(studentIDText.text.toString())

            mDocRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                       if(passwordText.text == document.getString(PASSWORD_KEY)){
                           val intent = Intent(this,QRScannerActivity::class.java)
                           startActivity(intent)
                           // GOT TO HERE!!! WORKING ON GETTING DATABASE PASSWORD TO CHANGE TO NEW WINDOW
                       }

                    else {
                        Toast.makeText(this, "Student Does Not Exist!", Toast.LENGTH_SHORT).show()
                    }
                }
            }



             Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
            // query database to retrieve the student id
            // query database to retrieve the password associated with the id
            // check that the id and password match
            // if they do, store the username and password locally in the sqlite database
            // load the QR Scanner screen
        }

    }




}