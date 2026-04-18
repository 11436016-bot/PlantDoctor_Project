package com.example.plantdoctor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val btnSubmit = findViewById<Button>(R.id.btn_register_submit)
        val tvBackLogin = findViewById<TextView>(R.id.tv_back_to_login)

        // 點擊「完成註冊」：跳轉到主頁 HomeActivity
        btnSubmit.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish() // 關閉註冊頁
        }

        // 點擊「返回登錄」：回到上一頁
        tvBackLogin.setOnClickListener {
            finish() // 直接結束這一頁，就會回到剛才的登錄頁
        }
    }
}