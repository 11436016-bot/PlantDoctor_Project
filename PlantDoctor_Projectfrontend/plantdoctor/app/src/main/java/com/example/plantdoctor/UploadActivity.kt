package com.example.plantdoctor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class UploadActivity : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private lateinit var imgPreview: ImageView

    // 1. 照片選取器回傳處理
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    // 取得永久讀取權限，避免下一頁讀取失敗
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                selectedImageUri = uri
                imgPreview.setImageURI(uri)
                imgPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                Toast.makeText(this, "照片已成功選取！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        // 綁定元件
        imgPreview = findViewById(R.id.img_preview)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_home)
        val btnCamera = findViewById<Button>(R.id.btn_camera)
        val btnAlbum = findViewById<Button>(R.id.btn_album)
        val btnAnalyze = findViewById<Button>(R.id.btn_analyze)

        // 設定點擊事件
        imgPreview.setOnClickListener { openAlbum() }
        btnAlbum.setOnClickListener { openAlbum() }
        btnBack.setOnClickListener { finish() }

        btnCamera.setOnClickListener {
            Toast.makeText(this, "相機功能開發中，請先使用相簿選取", Toast.LENGTH_SHORT).show()
        }

        // 2. 核心邏輯：點擊「開始分析」
        btnAnalyze.setOnClickListener {
            if (selectedImageUri != null) {
                // 從小本本取出 Token
                val sharedPref = getSharedPreferences("PlantDoctor", Context.MODE_PRIVATE)
                val rawToken = sharedPref.getString("token", "") ?: ""

                // --- 重要：Token 深度清理 ---
                // 確保沒有換行、回車或前後空格，避免 Header 報錯
                val cleanToken = rawToken.replace("\n", "")
                    .replace("\r", "")
                    .trim()

                if (cleanToken.isEmpty()) {
                    Toast.makeText(this, "登入已失效，請重新登入", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    return@setOnClickListener
                }

                // 3. 準備跳轉到進度條頁面，並帶上圖片路徑與 Token
                val intent = Intent(this, DiagnoseProgressActivity::class.java)
                intent.putExtra("IMAGE_URI", selectedImageUri.toString())
                intent.putExtra("CLEAN_TOKEN", cleanToken)
                startActivity(intent)

            } else {
                Toast.makeText(this, "請先選擇一張照片再進行分析", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openAlbum() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        selectImageLauncher.launch(intent)
    }
}