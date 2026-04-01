# 📡 MeshTalk – BLE Mesh Messaging App

MeshTalk is a Kotlin-based Android application that enables **offline communication** using **Bluetooth Low Energy (BLE)** and **mesh networking**.

Unlike traditional messaging apps, MeshTalk works **without internet or WiFi**, allowing devices to communicate by forming a **decentralized multi-hop network**.

---

## 🚀 Problem Statement

In situations like:
- Network outages
- Remote locations
- Disaster scenarios

Communication becomes difficult due to lack of internet.

MeshTalk solves this by enabling:
> 📶 **Device-to-device communication using BLE mesh networking**

---

## 💡 Solution Overview

Each device in MeshTalk acts as:
- 📤 Sender  
- 📥 Receiver  
- 🔁 Relay Node  

Messages are forwarded across devices:

Device A → Device B → Device C → Device D

Even if Device A is not directly connected to Device D, the message reaches through intermediate devices.

---

## 🔥 Key Features

### 📡 BLE Communication
- Scan nearby BLE devices
- Advertise device presence
- Connect using GATT (if required)

### 💬 Messaging
- Send and receive text messages
- Real-time chat interface
- Message timestamps

### 🔁 Mesh Networking
- Multi-hop message forwarding
- Devices automatically relay messages

### 🚫 Duplicate Prevention
- Unique message ID (senderId + timestamp)
- Local cache to avoid reprocessing

### ⏳ TTL (Time-To-Live)
- Limits how far a message travels
- Prevents infinite loops

### 📦 Local Storage
- Store messages using Room Database
- Maintain chat history

### 📊 Debug / Status Panel
- Show relay activity, TTL values, cache hits, and connected devices

---

## 🧠 How It Works

1. User sends a message  
2. Message is broadcast via BLE  
3. Nearby devices receive it  
4. Each device checks for duplicates  
5. If new → save, display, and forward  
6. TTL decreases until it reaches 0  

---

## 🏗 Architecture

UI Layer → ViewModel → Repository → BLEManager + MeshManager → Room DB

---

## 📦 Message Model

```kotlin
data class MeshMessage(
    val id: String,
    val senderId: String,
    val receiverId: String?,
    val message: String,
    val ttl: Int,
    val timestamp: Long
)
```

---

## ⚙️ Tech Stack

- Kotlin
- Android BLE APIs
- MVVM Architecture
- Jetpack Compose
- Room Database

---

## 🔐 Permissions Required

```xml
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

---

## 🧪 How to Run

1. Clone the repo  
2. Open in Android Studio  
3. Enable Bluetooth & Location  
4. Run on multiple devices  

---

## 🧪 Testing

- Turn OFF internet  
- Run app on 3–4 devices  
- Send message  
- Observe mesh relay  

---

## 🔧 Future Improvements

- End-to-end encryption  
- Group chat  
- Mesh visualization  
- Delivery acknowledgements  

---

## 👨‍💻 Author

GK

---

## 📄 License

MIT License
