package com.example.dacs.viewmodel.home

import androidx.lifecycle.ViewModel
import com.example.dacs.model.Channel
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(): ViewModel() {

    private val firebaseDatabase = Firebase.database
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _filteredChannels = MutableStateFlow<List<Channel>>(emptyList())
    val filteredChannels = _filteredChannels.asStateFlow()

    init {
        getChannels()
    }

    private fun getChannels() {
        // Listen for channels changes in realtime
        firebaseDatabase.getReference("channel").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Channel>()
                snapshot.children.forEach { data ->
                    val channel = Channel(data.key!!, data.value.toString())
                    list.add(channel)
                }
                _channels.value = list
                filterChannels() // Filter channels whenever the list updates
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterChannels()
    }

    private fun filterChannels() {
        val query = _searchQuery.value.lowercase()
        if (query.isEmpty()) {
            _filteredChannels.value = _channels.value
        } else {
            _filteredChannels.value = _channels.value.filter { channel ->
                channel.name.lowercase().contains(query)
            }
        }
    }

    fun addChannel(name: String) {
        val key = firebaseDatabase.getReference("channel").push().key
        firebaseDatabase.getReference("channel").child(key!!).setValue(name).addOnSuccessListener {
            getChannels()
        }
    }
}