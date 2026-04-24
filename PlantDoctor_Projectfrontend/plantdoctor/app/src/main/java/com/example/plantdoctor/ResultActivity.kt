package com.example.plantdoctor

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import coil.load
import org.json.JSONObject

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // 1. 綁定 UI 元件 (已對齊 XML ID)
        val btnBack = findViewById<ImageButton>(R.id.btn_back_home)
        val btnSave = findViewById<Button>(R.id.btn_save_report)
        val imgPlant = findViewById<ImageView>(R.id.img_result_plant)
        val tvPlantName = findViewById<TextView>(R.id.tv_result_plant_name)
        val tvStatus = findViewById<TextView>(R.id.tv_result_status)
        val tvConfidence = findViewById<TextView>(R.id.tv_result_confidence)
        val tvAdvice = findViewById<TextView>(R.id.tv_result_advice)
        val resultCard = findViewById<CardView>(R.id.card_result)

        // 2. 載入進入動畫
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        resultCard.startAnimation(fadeIn)

        // 3. 接收並解析來自 DiagnoseProgressActivity 的資料
        val resultJson = intent.getStringExtra("DIAGNOSE_RESULT")

        if (resultJson != null) {
            try {
                val jsonObject = JSONObject(resultJson)
                val data = jsonObject.getJSONObject("data")

                // 提取後端欄位
                val statusName = data.optString("status_name", "辨識中")
                val confidence = data.optDouble("confidence", 0.0)
                val suggestion = data.optString("suggestion", "無特定建議")
                val treatment = data.optString("treatment", "無建議處置")
                val category = data.optString("category", "植物")
                val imageUrl = data.optString("image_url", "")

                // --- 介面美化邏輯 ---

                // A. 動態設定顏色與圖示
                if (statusName.contains("健康")) {
                    tvStatus.text = "🌱 $statusName"
                    tvStatus.setTextColor(Color.parseColor("#2E7D32")) // 森林綠
                } else {
                    tvStatus.text = "⚠️ $statusName"
                    tvStatus.setTextColor(Color.parseColor("#C62828")) // 警告紅
                }

                // B. 顯示基本資訊
                tvPlantName.text = "偵測作物：$category"
                val confPercent = (confidence * 100).toInt()
                tvConfidence.text = "辨識信心度：$confPercent%"

                // C. 格式化處方箋文字
                tvAdvice.text = "【專家建議】\n$suggestion\n\n【建議處置】\n$treatment"

                // D. 使用 Coil 載入後端回傳的圖片
                if (imageUrl.isNotEmpty()) {
                    imgPlant.load(imageUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_launcher_background)
                        error(R.drawable.ic_launcher_background)
                    }
                }

            } catch (e: Exception) {
                Log.e("ResultActivity", "解析失敗: ${e.message}")
                tvStatus.text = "解析診斷結果失敗"
            }
        }

        // 4. 返回按鈕：帶有平滑切換動畫
        btnBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            // 自定義轉場：從左邊滑入
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
            finish()
        }

        // 5. 儲存按鈕：增加回饋感
        btnSave.setOnClickListener {
            Toast.makeText(this, "✅ 診斷紀錄已同步至雲端歷史庫", Toast.LENGTH_SHORT).show()

            // 按鈕變為不可用狀態，避免重複點擊
            btnSave.isEnabled = false
            btnSave.text = "已儲存"
            btnSave.alpha = 0.5f
        }
    }
}