package com.example.mailserver

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mailserver.ui.theme.MailServerTheme
import androidx.compose.foundation.layout.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.room.Delete
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MailServerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MailingApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MailingApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val directoryModel: DirectoryModel = viewModel()

    NavHost(navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                directoryModel = directoryModel,
                navController = navController,
                toMailList = { navController.navigate("mailList") },
                toRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                directoryModel = directoryModel,
                 navController = navController, modifier = modifier
            )
        }

        composable("messagesList/{userId}/{enteredName}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val enteredName = backStackEntry.arguments?.getString("enteredName") ?: return@composable
            MessagesScreen(userId = userId, userName = enteredName, directoryModel = directoryModel, navController = navController)
        }


        composable("createMessage/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            CreateMessageScreen(
                userId = userId,
                directoryModel = directoryModel,
                navController = navController
            )
        }

        composable("messageDetail/{messageId}?userId={userId}&userName={userName}") { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: ""
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            val userName = backStackEntry.arguments?.getString("userName") ?: ""

            MessageDetailScreen(
                messageId = messageId,
                userId = userId,
                userName = userName,
                directoryModel = directoryModel,
                navController = navController
            )
        }



    }
}

@Composable
fun LoginScreen(
    directoryModel: DirectoryModel,
    toMailList: () -> Unit,
    navController: NavController,
    toRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) } // State for error message

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Login", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Show error message if it exists
        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(onClick = {
            directoryModel.login(username, password, { messages, userId, enteredName ->
                // Navigate to the messages screen and pass the userId and enteredName
                navController.navigate("messagesList/$userId/$enteredName") {
                    popUpTo("login") { inclusive = true }  // Close the login screen
                }
                // Store the messages in the ViewModel if necessary
                directoryModel.messages.value = messages
            }, { error ->
                errorMessage = error // Update error message state
            })
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = toRegister) {
            Text("Don't have an account? Register")
        }
    }
}



@Composable
fun RegisterScreen(
    directoryModel: DirectoryModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val userId = remember { mutableStateOf<String?>(null) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(20.dp)
            .fillMaxSize()
    ) {
        Text(
            text = "Create an Account",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 20.dp)
        )


        TextField(
            value = username.value,
            onValueChange = { username.value = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Password Field
        TextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(20.dp))

        // Register Button
        Button(
            onClick = {
                directoryModel.newUser(username.value, password.value) { uuid ->
                    userId.value = uuid
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Register")
        }

        Spacer(modifier = Modifier.height(16.dp))


        userId.value?.let {
            Text("User ID: $it", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Already have an account Button
        TextButton(onClick = { navController.navigate("login") }) {
            Text("Already have an account? Log in")
        }


        userId.value?.let {
            Button(
                onClick = {
                    navController.navigate("login")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MessagesScreen(
    userId: String,
    userName: String,
    directoryModel: DirectoryModel,
    navController: NavController
) {
    val messages = directoryModel.messages.value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages List") },
                actions = {
                    IconButton(onClick = {

                        navController.navigate("createMessage/$userId")
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Message")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Your username : $userName", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
            Spacer(modifier = Modifier.height(16.dp).padding(5.dp))

            Text("Note: If you dont have any message this section will be empty!", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(10.dp).padding(16.dp))

            LazyColumn {
                items(messages) { message ->
                    MessageItem(message = message) {

                        navController.navigate("messageDetail/${message.messageId}?userId=$userId&userName=$userName")
                    }
                }
            }
        }
    }
}


@Composable
fun MessageItem(message: Message, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),

        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Subject: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = message.subject,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))


            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sender: ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = message.senderName,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateMessageScreen(
    userId: String,
    directoryModel: DirectoryModel,
    navController: NavController
) {
    var recipient by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Create New Message", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 32.dp))
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = recipient,
            onValueChange = { recipient = it },
            label = { Text("Recipient (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = subject,
            onValueChange = { subject = it },
            label = { Text("Subject") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Message Body") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(vertical = 8.dp),
            maxLines = 6,
            minLines = 5,
            visualTransformation = VisualTransformation.None,
            shape = MaterialTheme.shapes.medium,
            colors = TextFieldDefaults.outlinedTextFieldColors()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            isLoading = true
            errorMessage = null

            // Collect the recipients from the input
            val recipients = recipient.split(",").map { it.trim() }

            // Create and send the message
            directoryModel.createAndSendMessage(subject, body, userId, recipients) { messageId, error ->
                isLoading = false  // Reset loading state

                if (error != null) {
                    errorMessage = error
                    return@createAndSendMessage
                }

                if (messageId == null) {
                    errorMessage = "Failed to create message."
                    return@createAndSendMessage
                }

                // Navigate back on success
                navController.popBackStack()
            }
        }) {
            Text("Send Message")
        }


        errorMessage?.let {
            Text(it, color = Color.Red)
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(
    messageId: String,
    userId: String,
    userName: String,
    directoryModel: DirectoryModel,
    navController: NavController
) {
    val messageDetail = remember { mutableStateOf<MessageDetail?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }


    LaunchedEffect(messageId) {
        isLoading.value = true
        val response = directoryModel.fetchMessageDetail(messageId) // Correctly implemented now
        if (response.isSuccessful) {
            messageDetail.value = response.body()
        } else {
            errorMessage.value = "Failed to load message details."
        }
        isLoading.value = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Message Details") },
                actions = {
                    IconButton(onClick = {

                        directoryModel.deleteMessage(userId, messageId) { success ->
                            if (success) {
                                messageDetail.value = null
                                navController.popBackStack()
                            } else {
                                errorMessage.value = "Failed to delete the message."
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Message",
                            tint = Color.Red
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            if (isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage.value != null) {

                Text(
                    text = errorMessage.value ?: "Unknown error",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {

                messageDetail.value?.let { message ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        Text(
                            text ="Subject: ${message.subject}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )


                        Text(
                            text = "From: ${message.sender}",
                            style = MaterialTheme.typography.bodyLarge,

                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sent on: ${message.sent}",
                            style = MaterialTheme.typography.bodySmall,

                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )




                        Text(
                            text = message.body,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}


