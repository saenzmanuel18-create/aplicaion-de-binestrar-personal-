package com.example.aplicaciondeprueba

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class BaserowRitual(
    val id: Int? = null,
    @SerializedName("Nombre") val name: String,
    @SerializedName("Notas") val duration: String,
    @SerializedName("Activo") val isDone: Boolean = false
)

data class BaserowUser(
    val id: Int? = null,
    @SerializedName("Nombre") val email: String
)

data class BaserowResponse<T>(
    val results: List<T>
)

interface BaserowService {
    @GET("api/database/rows/table/925178/?user_field_names=true")
    suspend fun getRituals(
        @Header("Authorization") token: String
    ): BaserowResponse<BaserowRitual>

    @POST("api/database/rows/table/925178/?user_field_names=true")
    suspend fun addRitual(
        @Header("Authorization") token: String,
        @Body ritual: BaserowRitual
    ): BaserowRitual

    @PATCH("api/database/rows/table/925178/{rowId}/?user_field_names=true")
    suspend fun updateRitual(
        @Header("Authorization") token: String,
        @Path("rowId") rowId: Int,
        @Body ritual: BaserowRitual
    ): BaserowRitual

    @POST("api/database/rows/table/925213/?user_field_names=true")
    suspend fun registerUser(
        @Header("Authorization") token: String,
        @Body user: BaserowUser
    ): BaserowUser
}

object BaserowClient {
    private const val BASE_URL = "https://api.baserow.io/"
    
    val service: BaserowService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BaserowService::class.java)
    }
}
