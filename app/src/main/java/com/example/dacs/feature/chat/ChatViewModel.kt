package com.example.dacs.feature.chat

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.dacs.R
import com.example.dacs.config.CloudinaryConfig
import com.example.dacs.model.Message
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val cloudinaryConfig: CloudinaryConfig,
    @ApplicationContext private val context: Context
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
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    message.senderName?.let { it1 ->
                        postNotificationToUsers(channelID,
                            it1, messageText ?: "")
                    }
                }
            }
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
        registerUserIdToChannel(channelID)
    }

    fun getAllUserEmail(channelID: String, callBack: (List<String>) -> Unit) {
        val ref = db.reference.child("channels").child(channelID).child("users")
        val userIds = mutableListOf<String>()
        ref.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    userIds.add(it.value.toString())
                }
                callBack.invoke(userIds)
            }

            override fun onCancelled(error: DatabaseError) {
                callBack.invoke(emptyList())
            }
        })
    }

    fun registerUserIdToChannel(channelID: String) {
        val currentUser = Firebase.auth.currentUser;
        val ref = db.reference.child("channels").child(channelID).child("users")
        ref.child(currentUser?.uid ?: "").addListenerForSingleValueEvent(
            object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        ref.child(currentUser?.uid ?: "").setValue(currentUser?.email)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            }
        )
    }

    private fun subscribeForNotification(channelId: String) {
        FirebaseMessaging.getInstance().subscribeToTopic("group_${channelId}")
            .addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d("ChatViewModel", "Subscribed to topic: group_$channelId")
            } else {
                Log.d("ChatViewModel", "Failed to subscribed to topic: group_$channelId")
            }
        }
    }

    fun postNotificationToUsers(channelID: String, senderName: String, messageContext: String) {
        val fcmUrl = "https://fcm.googleapis.com/v1/projects/dacs-d1f95/messages:send"
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

        val request = object : StringRequest(Method.POST, fcmUrl, Response.Listener {
            Log.d("ChatViewModel", "Notification sent successfully")
        }, Response.ErrorListener {
            Log.e("ChatViewModel", "Failed to send notification")
        }) {
            override fun getBody(): ByteArray {
                return requestBody.toByteArray()
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer ${getAccessToken()}"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }

        val queue = Volley.newRequestQueue(context)
        queue.add(request)
    }

    fun getAccessToken() : String{
        val inputStream = context.resources.openRawResource(R.raw.dacs_key)
        val googleCreds = GoogleCredentials.fromStream(inputStream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        return googleCreds.refreshAccessToken().tokenValue
    }
}