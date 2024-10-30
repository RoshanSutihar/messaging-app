package com.example.mailserver

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DirectoryModel: ViewModel() {
    private lateinit var restInterface: DirectoryApiService
    val key = mutableStateOf("")

    val messages = mutableStateOf<List<Message>>(emptyList())

    init {
        val retrofit: Retrofit = Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://cmsc106.net/mail/")
            .build()
        restInterface = retrofit.create(DirectoryApiService::class.java)
    }

    fun newUser(name: String, password: String, onSuccess: (String) -> Unit) {
        val newUser = User(name, password)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = restInterface.newUser(newUser)
                withContext(Dispatchers.Main) {
                    key.value = result
                    if (result.isNotEmpty()) {
                        onSuccess(result)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun login(
        name: String,
        password: String,
        onSuccess: (List<Message>, String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userId = restInterface.login(name, password)
                withContext(Dispatchers.Main) {
                    if (userId.isNotEmpty()) {
                        fetchMessages(userId) { messages ->

                            onSuccess(messages, userId, name)
                        }
                    } else {
                        onError("Username or password is incorrect.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onError("Username or password is wrong")
                }
            }
        }
    }



    fun fetchMessages(userId: String, onSuccess: (List<Message>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Call the API to fetch messages
                val response = restInterface.getMessages(userId)  // API call via Retrofit
                val messages = response.map {
                    Message(
                        messageId = it.messageId,
                        receiverId = it.receiverId,
                        senderName = it.senderName,
                        subject = it.subject
                    )
                }
                withContext(Dispatchers.Main) {
                    onSuccess(messages) // Pass the list of messages to the success callback
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createAndSendMessage(
        subject: String,
        body: String,
        senderId: String,
        recipients: List<String>,
        onComplete: (String?, String?) -> Unit // Updated callback
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val createMessageRequest = MessageRequest(subject, body, senderId)
                val createMessageResponse = restInterface.createMessage(createMessageRequest)

                if (createMessageResponse.isSuccessful) {
                    val messageId = createMessageResponse.body() // Ensure this is correctly typed

                    // Proceed with sending to recipients
                    sendRecipients(recipients, messageId ?: "") { success ->
                        // Move this block to Main thread
                        viewModelScope.launch(Dispatchers.Main) {
                            // Call onComplete based on the success of sending recipients
                            if (success) {
                                onComplete(messageId, null)
                            } else {
                                onComplete(null, "Failed to send to some recipients.")
                            }
                        }
                    }
                } else {
                    onComplete(null, "Failed to create message.") // Handle failure to create message
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null, "An error occurred: ${e.message}") // Handle exceptions
            }
        }
    }


    fun sendRecipients(recipients: List<String>, messageId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            var allSent = true  // Flag to track if all messages were sent successfully

            for (recipient in recipients) {
                val recipientRequest = RecipientRequest(
                    message = messageId,
                    recipient = recipient
                )

                val response = restInterface.sendRecipient(recipientRequest)
                val responseBody = response.body() ?: "Unknown response"

                if (responseBody.contains("success", ignoreCase = true)) {
                    Log.d("sendRecipients", "Successfully sent to $recipient")
                } else {
                    Log.e("sendRecipients", "Failed to send to $recipient: $responseBody")
                    allSent = false  // Set the flag to false if any recipient fails
                }
            }

            // Call onComplete once after the loop ends, based on the allSent flag
            onComplete(allSent)
        }
    }


    // send message logging

//    fun sendRecipients(recipients: List<String>, messageId: String, onComplete: (Boolean) -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                for (recipient in recipients) {
//                    val recipientRequest = RecipientRequest(
//                        message = messageId,
//                        recipient = recipient
//                    )
//
//                    // Send the recipient request
//                    val response = restInterface.sendRecipient(recipientRequest)
//
//                    // Log the HTTP status code and response body
//                    Log.d("sendRecipients", "Status code for $recipient: ${response.code()}, Body: ${response.body() ?: "Unknown response"}")
//
//                    // Check if the response indicates success
//                    if (response.isSuccessful && response.body()?.contains("success", ignoreCase = true) == true) {
//                        Log.d("sendRecipients", "Successfully sent to $recipient")
//                    } else {
//                        Log.e("sendRecipients", "Failed to send to $recipient: Status code: ${response.code()}, Body: ${response.body() ?: "Unknown response"}")
//                        onComplete(false) // Stop on first failure
//                        return@launch
//                    }
//                }
//                onComplete(true) // All recipients sent successfully
//            } catch (e: Exception) {
//                Log.e("sendRecipients", "Exception occurred while sending recipients: ${e.message}")
//                e.printStackTrace()
//                onComplete(false) // Handle exceptions by signaling failure
//            }
//        }
//    }






    suspend fun fetchMessageDetail(messageId: String): Response<MessageDetail> {
        return try {
            restInterface.getMessageDetails(messageId)
        } catch (e: Exception) {
            Response.error(500, ResponseBody.create(MediaType.parse("application/json"), "Error"))
        }
    }


    fun deleteMessage(userId: String, messageId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = restInterface.deleteMessage(userId, messageId)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {

                        messages.value = messages.value.filter { it.messageId != messageId }
                        onComplete(true)
                    } else {
                        onComplete(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onComplete(false)
                }
            }
        }
    }




}

