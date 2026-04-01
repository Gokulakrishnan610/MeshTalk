package com.ble_mesh.meshtalk.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ble_mesh.meshtalk.ble.BLEService
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus
import com.ble_mesh.meshtalk.viewmodel.DebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: DebugViewModel,
    bleService: BLEService?,
    onBack: () -> Unit
) {
    val allMessages by viewModel.allMessages.collectAsStateWithLifecycle()
    val relayedCount by viewModel.relayedCount.collectAsStateWithLifecycle()
    val cacheHits by viewModel.cacheHits.collectAsStateWithLifecycle()
    val bleStatus by viewModel.bleStatus.collectAsStateWithLifecycle()

    // Poll peer count from BLE service
    val peerCount by produceState(0, bleService) {
        while (true) {
            value = bleService?.bleManager?.getConnectedPeerCount() ?: 0
            kotlinx.coroutines.delay(2000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF050A14))
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ───────────────────────────────────────────────────────
            Surface(color = Color(0xFF111827).copy(alpha = 0.95f), shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Icon(Icons.Default.Terminal, null, tint = Color(0xFF10B981), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Debug Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Mesh network diagnostics", color = Color(0xFF6B7280), fontSize = 11.sp)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Stat Cards Grid ──────────────────────────────────────────
                item {
                    Text("Network Stats", color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Connected",
                            value = "$peerCount",
                            icon = Icons.Default.Hub,
                            color = Color(0xFF6366F1)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Relayed",
                            value = "$relayedCount",
                            icon = Icons.Default.Repeat,
                            color = Color(0xFF10B981)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Cache Hits",
                            value = "$cacheHits",
                            icon = Icons.Default.Block,
                            color = Color(0xFFF59E0B)
                        )
                        StatCard(
                            modifier = Modifier.weight(1f),
                            label = "Total Msgs",
                            value = "${allMessages.size}",
                            icon = Icons.Default.Message,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }

                // ── BLE Status ───────────────────────────────────────────────
                item {
                    DebugInfoCard(label = "BLE Status", value = bleStatus, color = Color(0xFF10B981))
                }

                // ── Message Log ──────────────────────────────────────────────
                item {
                    Text("Message Log", color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                if (allMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No messages captured yet", color = Color(0xFF4B5563), fontSize = 13.sp)
                        }
                    }
                } else {
                    items(allMessages.take(50)) { msg ->
                        MessageLogRow(msg)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .padding(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFF6B7280), fontSize = 12.sp)
        }
    }
}

@Composable
private fun DebugInfoCard(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = Color(0xFF9CA3AF), fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MessageLogRow(msg: MeshMessage) {
    val statusColor = when (msg.status) {
        MessageStatus.SENT -> Color(0xFF6366F1)
        MessageStatus.RELAYED -> Color(0xFF10B981)
        MessageStatus.RECEIVED -> Color(0xFF9CA3AF)
    }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF162032))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = msg.status.name,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TTL:${msg.ttl}",
                    color = Color(0xFFF59E0B),
                    fontSize = 10.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = msg.message,
                color = Color(0xFFE2E8F0),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "From: ${msg.senderId.take(12)}…",
                color = Color(0xFF4B5563),
                fontSize = 10.sp
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeFormatter.format(Date(msg.timestamp)),
                color = Color(0xFF4B5563),
                fontSize = 10.sp
            )
        }
    }
}
