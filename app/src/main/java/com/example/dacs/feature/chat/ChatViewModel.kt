package com.example.dacs.feature.chat

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.volley.toolbox.StringRequest
import com.example.dacs.config.CloudinaryConfig
import com.example.dacs.model.Message
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val cloudinaryConfig: CloudinaryConfig
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val message = _messages.asStateFlow()
    private val db = Firebase.database

    fun sendMessage(channelID: String, messageText: String?, image: String? = null) {
        val message = Message(
            db.reference.push().key ?: UUID.randomUUID().toString(),
            Firebase.auth.currentUser?.uid ?: "",
            messageText,
            System.currentTimeMillis(),
            Firebase.auth.currentUser?.displayName ?: "",
            null,
            image
        )
        db.getReference("messages").child(channelID).push().setValue(message)
    }

    fun sendImageMessage(uri: Uri, channelID: String) {
        cloudinaryConfig.uploadImage(
            uri = uri,
            onSuccess = { imageUrl ->
                // When image is successfully uploaded to Cloudinary, save the URL to Firebase
                sendMessage(channelID, null, imageUrl)
            },
            onError = { error ->
                // Handle error
                println("Error uploading image: $error")
            }
        )
    }

    fun listenForMessages(channelID: String) {
        db.getReference("messages").child(channelID).orderByChild("createdAt")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<Message>()
                    snapshot.children.forEach { data ->
                        val message = data.getValue(Message::class.java)
                        message?.let {
                            list.add(it)
                        }
                    }
                    _messages.value = list
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
        subscribeForNotification(channelID)
    }



    private fun subscribeForNotification(channelId: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("group_${channelId}").addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("ChatViewModel", "Subscribed to topic: group_$channelId")
            } else {
                Log.d("ChatViewModel", "Failed to subscribed to topic: group_$channelId")
            }
        }
    }

    fun postNotificationToUsers(channelID: String, senderName: String, messageContext: String) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/PROJECT_ID/messages:send"
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("topic", "group_$channelID")
                put("notification", JSONObject().apply {
                    put("title", "New message in $channelID")
                    put("body", "$senderName: $messageContext")
                })
            })
        }

        val requestBody = jsonBody.toString()

        val request = object : StringRequest()
    }
}