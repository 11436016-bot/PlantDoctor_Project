package com.example.plantdoctor

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 1. 綁定綠色返回箭頭
        val btnBack = findViewById<ImageButton>(R.id.btn_back_home)
        btnBack.setOnClickListener {
            // 返回上一頁 (Home)
            finish()
        }

        // 2. 獲取輸入框引用
        val etUsername = findViewById<EditText>(R.id.et_username)
        val etGmail = findViewById<EditText>(R.id.et_gmail)

        // 3. 忘記密碼點擊
        findViewById<TextView>(R.id.tv_forgot_password).setOnClickListener {
            Toast.makeText(this, "正在導向密碼重設頁面...", Toast.LENGTH_SHORT).show()
        }

        // 4. 背景顏色點擊
        findViewById<TextView>(R.id.tv_color_select).setOnClickListener {
            Toast.makeText(this, "功能開發中：背景顏色切換", Toast.LENGTH_SHORT).show()
        }
    }
}