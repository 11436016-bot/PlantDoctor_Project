package com.example.plantdoctor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*

class DiagnoseProgressActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercent: TextView
    private var fakeProgress = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isApiFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_diagnose_progress)

        progressBar = findViewById(R.id.progressBar_horizontal)
        tvPercent = findViewById(R.id.tv_percent)

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val cleanToken = intent.getStringExtra("CLEAN_TOKEN")

        if (imageUriString == null) {
            Toast.makeText(this, "找不到圖片資料", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)

        // 1. 啟動虛擬進度條動畫
        startFakeProgress()

        // 2. 開始執行上傳與診斷
        uploadAndDiagnose(imageUri, cleanToken)
    }

    private fun startFakeProgress() {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (fakeProgress < 85 && !isApiFinished) {
                    fakeProgress += 2
                    handler.post {
                        progressBar.progress = fakeProgress
                        tvPercent.text = "$fakeProgress%"
                    }
                }
            }
        }, 0, 100) // 每 100 毫秒跑一次
    }

    private fun uploadAndDiagnose(uri: Uri, passedToken: String?) {
        val sharedPref = getSharedPreferences("PlantDoctor", Context.MODE_PRIVATE)
        val token = passedToken ?: sharedPref.getString("token", null)

        if (token == null) {
            Toast.makeText(this, "尚未登入", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 準備圖片 Part
        val file = uriToFile(uri)
        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val apiService = PlantApiService.create()

        apiService.uploadImage(token, body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                isApiFinished = true // 標記 API 已結束，停止虛擬進度

                if (response.isSuccessful) {
                    // API 成功，衝向 100%
                    progressBar.progress = 100
                    tvPercent.text = "100%"

                    val resultJson = response.body()?.string()

                    // 延遲一下下讓使用者看清楚 100%，然後跳轉
                    handler.postDelayed({
                        val intent = Intent(this@DiagnoseProgressActivity, ResultActivity::class.java)
                        intent.putExtra("DIAGNOSE_RESULT", resultJson)
                        startActivity(intent)
                        finish()
                    }, 500)

                } else {
                    // 處理失敗 (包含 422 信心度不足)
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        JSONObject(errorBody).optString("detail", "分析失敗")
                    } catch (e: Exception) {
                        "錯誤代碼: ${response.code()}"
                    }

                    Toast.makeText(this@DiagnoseProgressActivity, errorMessage, Toast.LENGTH_LONG).show()
                    finish()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                isApiFinished = true
                Log.e("UploadError", t.message ?: "Unknown error")
                Toast.makeText(this@DiagnoseProgressActivity, "連線失敗", Toast.LENGTH_LONG).show()
                finish()
            }
        })
    }

    private fun uriToFile(uri: Uri): File {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        outputStream.close()
        inputStream?.close()
        return file
    }
}