package com.example.mailserver

data class MessageRequest(
    val subject: String,
    val body: String,
    val sender: String
)

data class CreateMessageResponse(
    val id: String // ID of the created message
)