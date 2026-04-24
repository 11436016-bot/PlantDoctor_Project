package com.example.plantdoctor

import android.content.Context
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// 1. 資料模型：對接後端 JSON 並保留展開狀態
data class HistoryItem(
    val id: Int,
    val plantName: String,
    val date: String,
    val status: String,
    val imageUrl: String,
    val advice: String = "建議：保持環境通風，避免過度澆水，並觀察病灶是否擴散。",
    var isExpanded: Boolean = false
)

class HistoryListActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private val historyList = mutableListOf<HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_list)

        // 返回按鈕
        val btnBack = findViewById<ImageButton>(R.id.btn_back_home)
        btnBack.setOnClickListener { finish() }

        // 初始化 RecyclerView
        rvHistory = findViewById(R.id.rv_history_list)
        rvHistory.layoutManager = LinearLayoutManager(this)

        // 先建立空的 Adapter
        adapter = HistoryAdapter(historyList)
        rvHistory.adapter = adapter

        // 從 API 獲取資料
        fetchHistoryData()
    }

    private fun fetchHistoryData() {
        val sharedPref = getSharedPreferences("PlantDoctor", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""

        if (token.isEmpty()) {
            Toast.makeText(this, "請先登入", Toast.LENGTH_SHORT).show()
            return
        }

        // 呼叫 API
        val apiService = PlantApiService.create()
        apiService.getDiaries(token).enqueue(object : Callback<DiaryResponse> {
            override fun onResponse(call: Call<DiaryResponse>, response: Response<DiaryResponse>) {
                if (response.isSuccessful) {
                    val remoteData = response.body()?.data ?: emptyList()

                    // 清空舊資料並轉換新資料
                    historyList.clear()
                    remoteData.forEach {
                        historyList.add(HistoryItem(
                            id = it.id,
                            plantName = it.crop_name ?: "未知植物",
                            date = it.created_at,
                            status = "診斷結果：${it.status_name}",
                            imageUrl = it.image_url
                        ))
                    }

                    // 通知 Adapter 更新畫面
                    adapter.notifyDataSetChanged()

                    if (historyList.isEmpty()) {
                        Toast.makeText(this@HistoryListActivity, "目前尚無診斷紀錄", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@HistoryListActivity, "伺服器回報錯誤", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<DiaryResponse>, t: Throwable) {
                Log.e("HistoryList", "連線失敗: ${t.message}")
                Toast.makeText(this@HistoryListActivity, "網路連線失敗，請檢查後端狀態", Toast.LENGTH_SHORT).show()
            }
        })
    }
}

// 2. Adapter 部分：整合圖片載入與展開動畫
class HistoryAdapter(private val historyList: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_history_name)
        val tvDate: TextView = view.findViewById(R.id.tv_history_date)
        val tvStatus: TextView = view.findViewById(R.id.tv_history_status)
        val tvAdvice: TextView = view.findViewById(R.id.tv_history_advice)
        val layoutDetail: LinearLayout = view.findViewById(R.id.layout_detail)
        val imgArrow: ImageView = view.findViewById(R.id.img_arrow)
        val imgPlant: ImageView = view.findViewById(R.id.img_history_plant) // 記得 XML 要有這個 ID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        // 填入文字資料
        holder.tvName.text = item.plantName
        holder.tvDate.text = item.date
        holder.tvStatus.text = item.status
        holder.tvAdvice.text = item.advice

        // 使用 Coil 載入植物照片
        holder.imgPlant.load(item.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        // 展開/縮起邏輯與動畫
        holder.layoutDetail.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        holder.imgArrow.rotation = if (item.isExpanded) 180f else 0f

        holder.itemView.setOnClickListener {
            item.isExpanded = !item.isExpanded
            // 開始平滑過渡動畫
            TransitionManager.beginDelayedTransition(holder.itemView as ViewGroup)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = historyList.size
}