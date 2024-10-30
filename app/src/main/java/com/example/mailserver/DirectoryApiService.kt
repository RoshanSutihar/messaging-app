package com.example.mailserver

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query

interface DirectoryApiService {
    @POST("user")
    suspend fun newUser(@Body user : User) : String

    @GET("user")
    suspend fun login(@Query("user") user : String,@Query("password") password : String) : String

    @GET("handle")
    suspend fun getMessages(@Query("receiver") userId: String): List<MessageResponse>

    @POST("message")
    suspend fun createMessage(@Body request: MessageRequest): Response<String>

    @POST("recipient")
    suspend fun sendRecipient(
        @Body request: RecipientRequest
    ): Response<String>

    @GET("message/{messageId}")
    suspend fun getMessageDetails(@Path("messageId") messageId: String): Response<MessageDetail>

    @DELETE("recipient/{userId}/message/{messageId}")
    suspend fun deleteMessage(
        @Path("userId") userId: String,
        @Path("messageId") messageId: String
    ): Response<Unit>


}