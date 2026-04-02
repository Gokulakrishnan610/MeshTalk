package com.ble_mesh.meshtalk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus
import com.ble_mesh.meshtalk.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: String,
    peerName: String,
    chatType: String = "global",
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val isPrivate = chatType == "dm"
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF060D1A))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top Bar ──────────────────────────────────────────────────────
            Surface(
                color = Color(0xFF111827).copy(alpha = 0.96f),
                shadowElevation = 12.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF4F46E5))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            peerName.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(peerName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            if (isPrivate) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Encrypted DM",
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            if (isPrivate) "🔒 Private encrypted chat" else "Global mesh broadcast",
                            color = if (isPrivate) Color(0xFF6EE7B7) else Color(0xFF9CA3AF),
                            fontSize = 11.sp
                        )
                    }
                    // Broadcast button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText.trim(), null) // null = broadcast
                                inputText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Podcasts, "Broadcast", tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    }
                }
            }

            // ── Message List ─────────────────────────────────────────────────
            if (messages.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Forum, null, tint = Color(0xFF374151), modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No messages yet", color = Color(0xFF6B7280), fontSize = 15.sp)
                        Text("Send a message to start the mesh!", color = Color(0xFF4B5563), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            isOutgoing = msg.senderId == viewModel.deviceId
                        )
                    }
                }
            }

            // ── Input Row ────────────────────────────────────────────────────
            Surface(
                color = Color(0xFF111827),
                shadowElevation = 16.dp,
                modifier = Modifier.navigationBarsPadding() // Protect from bottom nav/gestures
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Message…", color = Color(0xFF6B7280))
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText.trim(), peerId)
                                inputText = ""
                                keyboardController?.hide()
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (inputText.isNotBlank())
                                    Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))
                                else
                                    Brush.linearGradient(listOf(Color(0xFF374151), Color(0xFF374151)))
                            )
                            .clickable(enabled = inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText.trim(), peerId)
                                inputText = ""
                                keyboardController?.hide()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank()) Color.White else Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MeshMessage, isOutgoing: Boolean) {
    val bubbleColor = if (isOutgoing)
        Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF6366F1)))
    else
        Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))

    val textColor = Color.White
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (isOutgoing) 18.dp else 4.dp,
                        bottomEnd = if (isOutgoing) 4.dp else 18.dp
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            Column {
                if (!isOutgoing) {
                    Text(
                        text = message.senderNickname ?: "via mesh",
                        fontSize = 9.sp,
                        color = Color(0xFF6EE7B7),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = message.message,
                    color = textColor,
                    fontSize = 15.sp
                )
            }
        }

        // Timestamp + status
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = timeFormatter.format(Date(message.timestamp)),
                fontSize = 10.sp,
                color = Color(0xFF6B7280)
            )
            if (isOutgoing) {
                val statusIcon = when (message.status) {
                    MessageStatus.SENT -> Icons.Default.Check
                    MessageStatus.RELAYED -> Icons.Default.DoneAll
                    MessageStatus.RECEIVED -> Icons.Default.DoneAll
                }
                val statusColor = when (message.status) {
                    MessageStatus.SENT -> Color(0xFF9CA3AF)
                    MessageStatus.RELAYED -> Color(0xFF6EE7B7)
                    MessageStatus.RECEIVED -> Color(0xFF7C3AED)
                }
                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
            }
            if (message.status == MessageStatus.RELAYED) {
                Text("TTL:${message.ttl}", fontSize = 9.sp, color = Color(0xFFF59E0B))
            }
        }
    }
}
