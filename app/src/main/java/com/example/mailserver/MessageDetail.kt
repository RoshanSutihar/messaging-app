package com.example.mailserver

data class MessageDetail(
    val subject: String,
    val body: String,
    val sent: String,
    val sender: String,
    val idmessage: String
)