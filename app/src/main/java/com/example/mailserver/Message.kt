package com.example.mailserver

import com.google.gson.annotations.SerializedName

data class Message(
    val messageId: String,
    val receiverId: String,
    val senderName: String,
    val subject: String
)
data class MessageResponse(
    @SerializedName("message") val messageId: String,
    @SerializedName("receiver") val receiverId: String,
    @SerializedName("sender") val senderName: String,
    @SerializedName("subject") val subject: String
)