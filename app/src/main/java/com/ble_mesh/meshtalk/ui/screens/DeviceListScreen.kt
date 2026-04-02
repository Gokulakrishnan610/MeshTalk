package com.ble_mesh.meshtalk.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ble_mesh.meshtalk.ble.DiscoveredDevice
import com.ble_mesh.meshtalk.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceListScreen(
    viewModel: MainViewModel,
    onOpenChat: (peerId: String, peerName: String, chatType: String, peerNickname: String?) -> Unit,
    onOpenDebug: () -> Unit
) {
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val bleStatus by viewModel.bleStatus.collectAsStateWithLifecycle()
    val isServiceBound by viewModel.isServiceBound.collectAsStateWithLifecycle()

    // Collect from BLEService when bound
    val bleServiceDevices by produceState<Map<String, DiscoveredDevice>>(emptyMap(), isServiceBound) {
        if (isServiceBound) {
            viewModel.getBleService()?.bleManager?.discoveredDevices?.collect { value = it }
        }
    }
    val serviceStatus by produceState("Idle", isServiceBound) {
        if (isServiceBound) {
            viewModel.getBleService()?.bleManager?.statusFlow?.collect { value = it }
        }
    }

    val deviceList = bleServiceDevices.values.toList()

    val myNickname by viewModel.myNickname.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf("") }

    // Pulse animation for the active scan indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF0A1628))
                )
            )
            .navigationBarsPadding() // Protect from bottom nav bar
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top App Bar ──────────────────────────────────────────────────
            Surface(
                color = Color(0xFF111827).copy(alpha = 0.96f),
                shadowElevation = 12.dp,
                modifier = Modifier.statusBarsPadding() // Protect from status bar
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated BLE icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF7C3AED), Color(0xFF4C1D95))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MeshTalk",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Name: $myNickname",
                                fontSize = 12.sp,
                                color = Color(0xFF6EE7B7)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1E293B))
                                    .clickable {
                                        editNameText = myNickname
                                        showEditDialog = true
                                    }
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Name",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    // Debug button
                    IconButton(onClick = onOpenDebug) {
                        Icon(
                            Icons.Default.Analytics,
                            contentDescription = "Debug",
                            tint = Color(0xFF7C3AED)
                        )
                    }
                }
            }

            // ── Status Banner ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                        .alpha(pulseAlpha)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "BLE Status: $serviceStatus",
                    color = Color(0xFF6EE7B7),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${deviceList.size} device${if (deviceList.size != 1) "s" else ""} found",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }

            // ── Global Chat Button ───────────────────────────────────────────
            Button(
                onClick = { onOpenChat("global", "Global Mesh", "global", null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Forum, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Open Global Mesh Chat", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // ── Section header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nearby Mesh Nodes", color = Color(0xFF6B7280), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text("Tap to chat privately", color = Color(0xFF4B5563), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Sensors, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
            }

            if (deviceList.isEmpty()) {
                EmptyDeviceState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(deviceList, key = { it.address }) { device ->
                        DeviceCard(
                            device = device,
                            onClick = {
                                onOpenChat(device.deviceId, device.name, "dm", device.name)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Nickname", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it.take(8) },
                    label = { Text("Nickname (max 8 chars)", color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF7C3AED)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = Color(0xFF1E293B),
            confirmButton = {
                TextButton(onClick = {
                    if (editNameText.isNotBlank()) {
                        viewModel.updateNickname(editNameText)
                    }
                    showEditDialog = false
                }) {
                    Text("Save", color = Color(0xFF10B981))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color(0xFFEF4444))
                }
            }
        )
    }
}

@Composable
private fun DeviceCard(device: DiscoveredDevice, onClick: () -> Unit) {
    val rssiStrength = when {
        device.rssi > -60 -> "Strong"
        device.rssi > -75 -> "Medium"
        else -> "Weak"
    }
    val rssiColor = when (rssiStrength) {
        "Strong" -> Color(0xFF10B981)
        "Medium" -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
    val signalBars = when (rssiStrength) {
        "Strong" -> 3
        "Medium" -> 2
        else -> 1
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = device.name.take(2).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.address,
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                // Signal bars
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    repeat(3) { i ->
                        val height = (i + 1) * 5
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (i < signalBars) rssiColor else Color(0xFF374151)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${device.rssi} dBm",
                    color = rssiColor,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF4B5563),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun EmptyDeviceState(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f, label = "scale",
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOut), RepeatMode.Reverse)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size((80 * scale).dp)
                .clip(CircleShape)
                .background(Color(0xFF7C3AED).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.BluetoothSearching,
                contentDescription = null,
                tint = Color(0xFF7C3AED),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Scanning for mesh nodes…", color = Color(0xFF9CA3AF), fontSize = 15.sp)
        Text("Make sure other devices have MeshTalk open", color = Color(0xFF4B5563), fontSize = 12.sp)
    }
}
