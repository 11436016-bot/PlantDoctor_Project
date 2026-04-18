package com.example.plantdoctor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    // 注意：我刪除了這裡原本的 data class LoginRequest，因為它已經存在於 PlantApiService.kt 了

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 1. 綁定 UI (確保 ID 與你的 activity_login.xml 一致)
        val etUsername = findViewById<EditText>(R.id.et_username)
        val etPassword = findViewById<EditText>(R.id.et_password)
        val btnLogin = findViewById<Button>(R.id.btn_login_submit)
        val tvRegister = findViewById<TextView>(R.id.tv_go_to_register)

        // 2. 登入邏輯
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "請輸入帳號和密碼", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 建立 API 服務
            val apiService = PlantApiService.create()

            // 這裡會自動使用 PlantApiService.kt 裡面定義的 LoginRequest
            val loginData = LoginRequest(username, password)

            apiService.login(loginData).enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        val jsonObject = JSONObject(responseBody ?: "")

                        // 取得 Token
                        val token = jsonObject.optString("access_token")

                        // 儲存 Token (供之後上傳圖片使用)
                        val sharedPref = getSharedPreferences("PlantDoctor", Context.MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("token", "Bearer $token")
                            apply()
                        }

                        Toast.makeText(this@LoginActivity, "登入成功！", Toast.LENGTH_SHORT).show()

                        // 跳轉到主頁
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // 伺服器回傳 400 或 500
                        val errorDetail = response.errorBody()?.string()
                        Log.e("LoginActivity", "Login Failed: $errorDetail")
                        Toast.makeText(this@LoginActivity, "登入失敗：帳號或密碼錯誤", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    // 完全連不上伺服器 (IP 不對或沒開 host 0.0.0.0)
                    Log.e("LoginActivity", "Network Error: ${t.message}")
                    Toast.makeText(this@LoginActivity, "連線失敗，請檢查電腦後端是否開啟", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 跳轉註冊
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}