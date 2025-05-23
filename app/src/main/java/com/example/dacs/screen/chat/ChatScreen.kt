package com.example.dacs.screen.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dacs.R
import com.example.dacs.viewmodel.chat.ChatViewModel
import com.example.dacs.screen.home.ChannelItem
import com.example.dacs.model.Message
import com.example.dacs.ui.theme.DarkGrey
import com.example.dacs.ui.theme.Purple
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.zegocloud.uikit.prebuilt.call.invite.widget.ZegoSendCallInvitationButton
import com.zegocloud.uikit.service.defines.ZegoUIKitUser
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(navController: NavController, channelId: String, channelName: String) {
    val chooserDialog = remember {
        mutableStateOf(false)
    }

    val cameraImageUri = remember {
        mutableStateOf<Uri?>(null)
    }

    val viewModel: ChatViewModel = hiltViewModel()

    val cameraImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri.value?.let {
                viewModel.sendImageMessage(it, channelId)
            }
        }
    }

    val imageLaucher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImageMessage(it, channelId)
        }
    }

    fun createImageUri() : Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = navController.context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return FileProvider.getUriForFile(
            navController.context,
            "${navController.context.packageName}.provider",
            File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
                cameraImageUri.value = Uri.fromFile(this)
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraImageLauncher.launch(createImageUri())
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            // Channel title at the top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
            ) {
                ChannelItem(
                    channelName = channelName,
                    modifier =  Modifier,
                    shouldShowCallButton = true,
                    onclick = { },
                    onCall = { callButton->
                        viewModel.getAllUserEmail(channelId) {
                            val list: MutableList<ZegoUIKitUser> = mutableListOf()
                            it.forEach { email ->
                                Firebase.auth.currentUser?.email?.let { em ->
                                    if(email != em){
                                        list.add(
                                            ZegoUIKitUser(
                                                email, email
                                            )
                                        )
                                    }
                                }
                            }
                            callButton.setInvitees(list)
                        }
                    })
            }

            LaunchedEffect(key1 = true) {
                viewModel.listenForMessages(channelId)
            }

            val messages = viewModel.message.collectAsState()

            ChatMessages(
                messages = messages.value,
                channelId = channelId,
                onSendMessage = { message -> viewModel.sendMessage(channelId, message) },
                onImageClicked = {
                    chooserDialog.value = true
                },
                channelName = channelName,
                viewModel = viewModel
            )
        }
    }

    if (chooserDialog.value) {
        ContentSelectionDialog(onCameraSelected = {
            chooserDialog.value = false
            if (navController.context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraImageLauncher.launch(createImageUri())
            } else {
                // request permisson
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }, onGallerySelected = {
            chooserDialog.value = false
            imageLaucher.launch("image/*")
        })
    }
}

@Composable
fun ContentSelectionDialog(onCameraSelected: () -> Unit, onGallerySelected: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = onCameraSelected ){
                Text(text = "Camera", color = Color.Black)
            }},
        dismissButton = {
            TextButton(onClick = onGallerySelected ){
                Text(text = "Gallery", color = Color.Black)
            }
        },
        title = { Text(text = "Select your source ?") },
        text = { Text(text = "Would you like to pick image from the gallery or uses the camera") }
    )
}

@Composable
fun ChatMessages(
    channelName: String,
    channelId: String,
    messages: List<Message>,
    onSendMessage: (String) -> Unit,
    onImageClicked: () -> Unit,
    viewModel: ChatViewModel
) {
    val hideKeyBoardController = LocalSoftwareKeyboardController.current
    val msg = remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                ChatBubble(message = message)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGrey)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                onImageClicked()
            }) {
                Image(painter = painterResource(id = R.drawable.attach), contentDescription = "Send")
            }

            TextField(
                value = msg.value,
                onValueChange = { msg.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(text = "Type a message") },
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        hideKeyBoardController?.hide()
                    }
                ),
                colors = TextFieldDefaults.colors().copy(
                    focusedContainerColor = DarkGrey,
                    unfocusedContainerColor = DarkGrey,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    unfocusedPlaceholderColor = Color.White
                )
            )

            IconButton(onClick = {
                if (msg.value.isNotEmpty()) {
                    onSendMessage(msg.value)
                    msg.value = ""
                }
            }) {
                Image(painter = painterResource(id = R.drawable.send), contentDescription = "Send")
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    val isCurrentUser = message.senderId == Firebase.auth.currentUser?.uid
    val bubbleColor = if (isCurrentUser) {
        Purple
    } else {
        DarkGrey
    }
    
    // Format timestamp
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeString = dateFormat.format(Date(message.createdAt))
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        val alignment = if (!isCurrentUser) Alignment.CenterStart else Alignment.CenterEnd
        Column(
            modifier = Modifier
                .padding(8.dp)
                .align(alignment),
            horizontalAlignment = if (!isCurrentUser) Alignment.Start else Alignment.End
        ) {
            // Display sender name
            if (!isCurrentUser) {
                Text(
                    text = message.senderName ?: "Unknown",
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCurrentUser) {
                    Image(
                        painter = painterResource(id = R.drawable.friend),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column(
                    horizontalAlignment = if (!isCurrentUser) Alignment.Start else Alignment.End
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = bubbleColor, shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        if (message.imageUrl != null) {
                            AsyncImage(
                                model = message.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.size(200.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(text = message.message?.trim() ?: "", color = Color.White)
                        }
                    }
                    // Display timestamp
                    Text(
                        text = timeString,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CallButton(isVideoCall: Boolean, onClick: (ZegoSendCallInvitationButton) -> Unit) {
    AndroidView(factory = {context -> val button = ZegoSendCallInvitationButton(context)
        button.setIsVideoCall(isVideoCall)
        button.resourceID = "zego_data"
        button
    }, modifier = Modifier.size(50.dp)) {zegoCallButton ->
        zegoCallButton.setOnClickListener{_ -> onClick(zegoCallButton)}
    }
}
