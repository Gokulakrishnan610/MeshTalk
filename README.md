<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="MeshTalk Logo" width="120" />
  <h1>📡 MeshTalk</h1>
  <p><strong>A fully decentralized BLE Mesh Messaging App for Android</strong></p>
</div>

MeshTalk is an advanced Kotlin-based Android application that enables **offline communication** using **Bluetooth Low Energy (BLE)** and true **mesh networking**.

Unlike traditional messaging apps, MeshTalk works entirely **without internet, cellular service, or WiFi**. Devices communicate by forming an automatic, self-healing **decentralized multi-hop network**.

---

## 🚀 Problem Statement

In situations like:
- Concerts and crowded events with cellular congestion
- Remote locations (hiking, camping)
- Natural disaster scenarios

Communication becomes difficult or impossible due to a lack of centralized infrastructure. 

MeshTalk solves this by enabling:
> 📶 **Device-to-device "Global" communication routing securely via a BLE mesh network.**

---

## 💡 Solution Overview

Every device running MeshTalk inherently acts as all three pillars of a network:
- 📤 **Sender** 
- 📥 **Receiver** 
- 🔁 **Relay Node** 

Messages are forwarded across devices infinitely (until TTL expires):
`Device A → Device B → Device C → Device D`

Even if Device A is not directly within Bluetooth range of Device D, the message seamlessly hops through intermediate devices in the background to reach its destination.

---

## 🔥 Key Features

### 📡 Active BLE Mesh Engine
- **Background Persistence:** Runs a dedicated Android Foreground Service to keep the mesh alive even when your phone is locked or the app is minimized.
- **Auto-Discovery:** Scans and dynamically prunes stale peers (12-second TTL) to ensure the network topology is always accurate.
- **Global Mesh Chat:** Avoids OS-level MAC randomization bugs by utilizing a unified, multi-hop global chat channel.

### 🔔 Native Push Notifications
- Built-in integration with Android's `NotificationManager`.
- Receives messages in the background and triggers **High-Priority Heads-Up Alerts** so you never miss an offline text.
- Fully supports Android 13+ `POST_NOTIFICATIONS` runtime permissions.

### 🚫 LRU Duplicate Prevention
- Employs a blazing fast **LRU (Least Recently Used) cache algorithm**.
- Maps unique message IDs (`senderId + timestamp`) in `O(1)` time.
- Crushes infinite network loops before they touch the local Rom database.

### 🎨 Modern UI/UX
- Built entirely in **Jetpack Compose**.
- Fully immersive **Edge-to-Edge UI** layout with transparent system status bars and dynamic padding.
- Real-time animated chat bubbling that correctly delegates Incoming (Left) and Outgoing (Right) message flows.

### 📦 Local Storage
- Stores all historical message data locally using an asynchronous **Room Database** and Kotlin Coroutines.

---

## 🧠 How It Works

1. User sends a message over the Global Chat.
2. The message is serialized into JSON and broadcast via GATT characteristics.
3. Nearby devices intercept the Bluetooth broadcast.
4. The background `MeshManager` checks the `LRU Cache` for duplicates.
5. If new: it saves to the local database, triggers a Push Notification, and forwards it back out into the airwaves.
6. The `TTL` (Time-to-Live) decreases by 1 on each hop until it hits 0 to prevent ghosting.

---

## 🏗 Architecture

`UI (Compose)` ↔️ `ViewModel` ↔️ `MessageRepository` ↔️ `Room DB` ↔️ `MeshManager` ↔️ `BLE Foreground Service`

### 📦 Message Model

```kotlin
@Entity(tableName = "messages")
data class MeshMessage(
    @PrimaryKey val id: String,
    val senderId: String,
    val receiverId: String?,
    val message: String,
    val ttl: Int,
    val timestamp: Long,
    val isOutgoing: Boolean,
    val status: MessageStatus
)
```

---

## ⚙️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Local DB:** Room Database
- **Concurrency:** Kotlin Coroutines & Flows
- **Network:** Android Bluetooth Low Energy (BLE) APIs

---

## 🔐 Permissions Required

Because MeshTalk requires robust background BLE routing, it requests the following on first launch:
```xml
<!-- Android 13+ Notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<!-- BLE Setup API 31+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE"/>

<!-- Location & Background execution -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
```

---

## 🧪 How to Run

1. Clone the repository.
2. Open in **Android Studio**.
3. Enable Bluetooth & Location Services on your physical Android devices.
4. Run the app on at least **2 (preferably 3+) devices** to witness the multi-hop mesh in action.

---

## 🔧 Future Improvements

- End-to-end AES-256 Payload Encryption  
- Private DMs (1-on-1 addressing over the mesh)
- Real-time animated Mesh Topology Visualizer  
- Delivery Acknowledgements (ACK returns)

---

## 👨‍💻 Author

**Gokulakrishnan610**

---

## 📄 License

MIT License
