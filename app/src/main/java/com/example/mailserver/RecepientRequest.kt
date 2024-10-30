package com.example.mailserver

data class RecipientRequest(
    val message: String,
    val recipient: String
)