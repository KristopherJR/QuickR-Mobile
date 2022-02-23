package com.example.quickrmobile

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
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
import java.util.jar.Manifest


private const val CAMERA_REQUEST_CODE = 101

// AUTHOR: Kristopher J Randle
// VERSION: 1.13
class QRScannerActivity : AppCompatActivity()
{
    private lateinit var tvtextview: TextView
    private lateinit var scanbutton: Button
    private lateinit var scannerview: CodeScannerView
    private lateinit var codeScanner: CodeScanner

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvtextview = findViewById(R.id.tv_textView)
        scanbutton = findViewById(R.id.scan_button)
        scannerview = findViewById(R.id.scanner_view)
        scannerview.visibility = View.GONE

        codeScanner = CodeScanner(this, scannerview)

        scanbutton.setOnClickListener{

            startScanner()
        }
    }

    private fun startScanner()
    {
        setupPermissions()
        codeScanner()
    }

    private fun codeScanner()
    {
        scannerview.visibility = View.VISIBLE
        tvtextview.visibility = View.VISIBLE
        codeScanner.apply {
            camera = CodeScanner.CAMERA_BACK
            formats = CodeScanner.ALL_FORMATS

            autoFocusMode = AutoFocusMode.SAFE
            scanMode = ScanMode.CONTINUOUS
            isAutoFocusEnabled = true
            isFlashEnabled = false

            decodeCallback = DecodeCallback {
                runOnUiThread{
                    tvtextview.text = it.text
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