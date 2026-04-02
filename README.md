<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="MeshTalk Logo" width="120" />
  <h1>📡 MeshTalk</h1>
  <p><strong>A fully decentralized, encrypted BLE Mesh Messaging App for Android</strong></p>
  
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org)
  [![Compose](https://img.shields.io/badge/Jetpack-Compose-green.svg)](https://developer.android.com/jetpack/compose)
</div>

MeshTalk is a cutting-edge Android application that enables **offline communication** using **Bluetooth Low Energy (BLE)** and a high-performance **decentralized mesh network**. 

Unlike traditional messaging apps, MeshTalk works entirely **without internet, cellular service, or WiFi**. Devices form an automatic, self-healing network where every user acts as a node, relaying messages across long distances through multiple intermediate hops.

---

## 🚀 The Mission

In environments like music festivals, remote hiking trails, or natural disaster zones, traditional communication infrastructure fails. MeshTalk provides a reliable, **infrastructure-free** alternative that is:
- 🛡️ **Private**: End-to-end encrypted personal chats.
- 🔗 **Resilient**: Decentralized multi-hop routing ensures messages find a path.
- 📱 **Modern**: A premium, wow-factor UI built with Jetpack Compose.

---

## 🔥 Key Features

### 🔒 Personal 1-on-1 Encrypted Chat
- **AES-256-GCM Encryption**: All private messages are encrypted at the source using a unified 256-bit symmetric key, ensuring only the intended recipient can read the content.
- **Hardware-ID Routing**: Messages are routed using immutable 8-character hardware identifiers, bypassing Android's MAC randomization for 100% reliable delivery.

### 📡 Advanced BLE Mesh Engine
- **Decentralized Multi-Hop**: Messages propagate through the network (`A -> B -> C -> D`), extending range far beyond standard Bluetooth limits.
- **Manufacturer Data Broadcasting**: Utilizes BLE `ManufacturerSpecificData` to broadcast identity/nicknames, circumventing the 31-byte advertising limit and ensuring fast discovery.
- **Background Persistence**: A dedicated Android Foreground Service keeps the mesh alive even when the app is closed or the screen is off.

### 📛 Customizable Identity
- **Dynamic Nicknames**: Change your mesh persona instantly! Update your nickname in the app, and it's immediately broadcasted to all nearby peers.
- **Auto-Discovery**: Real-time scanning with a 12-second TTL pruning mechanism ensures your peer list is always fresh and accurate.

### 🎨 Premium UI/UX
- **Modern Aesthetics**: A dark-mode focused, glassmorphic design with vibrant gradients and smooth micro-animations.
- **Edge-to-Edge Layout**: Fully immersive experience utilizing the latest Android `WindowInsets` APIs for status bar and navigation bar transparency.
- **Reactive Navigation**: Seamlessly switch between Global Mesh Chat and individual Private DMs.

### 🔔 Smart Notifications
- **Heads-Up Alerts**: Instant native notifications for incoming messages even in the background.
- **Relay Statistics**: A debug dashboard to monitor mesh health, cache hits, and forwarded message counts.

---

## 🧠 Technical Architecture

MeshTalk uses a clean, layered architecture for maximum reliability:

`UI (Compose)` ↔️ `ViewModel` ↔️ `MessageRepository` ↔️ `Room DB` ↔️ `MeshManager` ↔️ `BLE Service`

### 💾 The Message Model
```kotlin
@Entity(tableName = "messages")
data class MeshMessage(
    @PrimaryKey val id: String,         // Generated: senderId_timestamp
    val senderId: String,               // 8-char hardware ID
    val receiverId: String?,            // null for Global, ID for DM
    val message: String,                // Plaintext or [Encrypted] tag
    val ttl: Int,                       // Time-To-Live (hops)
    val timestamp: Long,
    val isOutgoing: Boolean,
    val isPrivate: Boolean,             // Personal Chat flag
    val encryptedPayload: String?       // Base64 AES Ciphertext
)
```

---

## 🏗 Setup & Installation

### Prerequisites
- Android Studio Hedgehog (or newer)
- 2+ Physical Android Devices (BLE Mesh doesn't work on emulators)
- Android 8.0 (API 26) or higher

### Steps
1. Clone the repository: `git clone https://github.com/Gokulakrishnan610/MeshTalk.git`
2. Open the project in Android Studio.
3. Grant permissions for **Bluetooth**, **Location**, and **Notifications** on first launch.
4. **Important**: Since MeshTalk uses BLE, ensure Bluetooth is toggled ON.

---

## 👨‍💻 Author

**Gokulakrishnan610**  
*Lead Developer & Mesh Architect*

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
