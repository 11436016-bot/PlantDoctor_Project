package com.example.plantdoctor

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// --- 將 Request 定義放在這裡，全專案共用 ---
data class LoginRequest(val username: String, val password: String)
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String,
    val full_name: String,
    val role: String = "user"
)
// 放在專案中的 PlantApiService.kt
data class DiaryResponse(
    val status: String,
    val count: Int,
    val data: List<DiaryItem>
)

data class DiaryItem(
    val id: Int,
    val crop_name: String?,
    val status_name: String,
    val image_url: String,
    val created_at: String
)
interface PlantApiService {

    // 1. 註冊帳戶
    @POST("users/register")
    fun register(@Body request: RegisterRequest): Call<ResponseBody>

    // 2. 帳戶登入
    @POST("user/login")
    fun login(@Body request: LoginRequest): Call<ResponseBody>

    // 3. 上傳照片
    @Multipart
    @POST("diaries/upload")
    fun uploadImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part
    ): Call<ResponseBody>

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000/"

        fun create(): PlantApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(PlantApiService::class.java)
        }
    }

    @GET("diaries")
    fun getDiaries(
        @Header("Authorization") token: String
    ): Call<DiaryResponse> // 這裡會自動解析成我們剛定義的 DiaryResponse
}