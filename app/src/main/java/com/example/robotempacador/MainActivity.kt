package com.example.robotempacador

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myWebView: WebView = findViewById(R.id.visorWeb)

        // CONFIGURACIÓN CRÍTICA
        myWebView.settings.javaScriptEnabled = true
        myWebView.settings.domStorageEnabled = true // Activa el guardado de puntuaciones
        myWebView.settings.databaseEnabled = true
        
        myWebView.webViewClient = WebViewClient()

        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        myWebView.loadUrl("file:///android_asset/juego.html")
    }
}