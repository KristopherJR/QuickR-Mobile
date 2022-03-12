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
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.model.Document

private const val USER_ID_KEY = "users"
private const val PASSWORD_KEY = "password"

private const val STUDENT_ID_KEY = "student_id"

// AUTHOR: Kristopher J Randle
// VERSION: 1.18
class MainActivity : AppCompatActivity()
{
    private lateinit var loginButton: Button
    private lateinit var studentIDText: TextView
    private lateinit var passwordText: TextView

    private var idValid: Boolean = false
    private var passwordValid: Boolean = false

    private lateinit var mDocRef : DocumentReference

    companion object
    {
        private lateinit var studentDoc : DocumentSnapshot

        fun getStudentDocumentSnapshot(): DocumentSnapshot
        {
            return studentDoc
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)
        loginButton = findViewById(R.id.login_button)
        studentIDText = findViewById(R.id.textbox_studentID)
        passwordText = findViewById(R.id.textbox_password)

        loginButton.setOnClickListener{
            //val intent = Intent(this,QRScannerActivity::class.java)
            //intent.putExtra("student_id", "18008890")
            //startActivity(intent)

            // MAKE SURE TO RE-ENABLE THE LINE BELOW AFTER SCANNER FUNCTIONALITY IS SET-UP
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
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
            // query database to retrieve the student id
            mDocRef = FirebaseFirestore.getInstance().collection(USER_ID_KEY).document(studentIDText.text.toString())

            mDocRef.get().addOnSuccessListener{ document ->
                    if (document != null)
                    {
                        if(document.exists())
                        {
                            // query database to retrieve the password associated with the id
                            if(passwordText.text.toString() == document.getString(PASSWORD_KEY).toString())
                            {
                                studentDoc = document
                                // load the QR Scanner screen
                                val intent = Intent(this,QRScannerActivity::class.java)
                                startActivity(intent)
                            }

                            else
                            {
                                Toast.makeText(this, "Password Incorrect.", Toast.LENGTH_SHORT).show()
                            }

                        }
                        else
                        {
                            Toast.makeText(this, "Student ID does not exist in database.", Toast.LENGTH_SHORT).show()
                        }

                    }
                }
        }
    }
}
