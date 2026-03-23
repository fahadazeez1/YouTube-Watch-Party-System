# 🏛️ Architecture Overview: YouTube Watch Party

This application utilizes a **hybrid architecture**, combining standard REST APIs for initial setup with a high-speed WebSocket (STOMP) layer for real-time video synchronization. 

To ensure the system is scalable, fast, and easy to maintain, the architecture relies heavily on **In-Memory Caching** and the **Strategy Design Pattern** for event handling.

---

## 1️⃣ High-Level Components

The backend is split into three main layers:

* **🌐 REST API Layer (Room & User Management):** Handles traditional HTTP requests (e.g., `POST /api/rooms/create`). It generates the 6-digit room code, saves the room to the SQL database, and registers users as `Participants` with specific `Roles` (Host, Moderator, Participant).
  
* **⚡ WebSocket Layer (Real-Time Sync):** Powered by Spring WebSockets and STOMP. Clients connect to a central `/ws` endpoint and subscribe to a specific room's channel (e.g., `/topic/room/123456`). Any message sent to this channel is instantly broadcasted to everyone in that room.
  
* **💾 State Management Layer (Database + Cache):**
  * *Persistent State (SQL Database):* Stores long-term data like Room details and User assignments.
  * *Transient State (In-Memory `ConcurrentHashMap`):* Video playback changes rapidly (e.g., scrubbing a timeline). Saving every `currentTime` update to a SQL database would cause massive lag. Instead, the `RoomSyncService` holds the live video state in memory for zero-latency updates.

---

## 2️⃣ The Core Flow: How Synchronization Works

To understand the system, here is the exact step-by-step flow of what happens when a Host pauses the video:

1. **The Trigger:** The Host clicks "Pause" on their React frontend.
2. **The Message:** The frontend sends a STOMP JSON message of type `"PAUSE"` to the backend destination: `/app/room/{roomId}/sync`.
3. **The Controller:** The `RoomWebSocketController` intercepts this message. Instead of processing the logic itself, it immediately hands the message to the `WebSocketMessageDispatcher`.
4. **The Dispatcher (Strategy Pattern):** The Dispatcher looks at the message type (`"PAUSE"`) and dynamically routes it to the correct handler—in this case, the `VideoControlHandler`.
5. **Validation (Role Cache):** The `VideoControlHandler` checks if the user is allowed to pause. To prevent slowing down the system with a database query, it checks the in-memory `ParticipantRoleCache`. It confirms the user is a `HOST`.
6. **State Update:** The handler updates the `RoomSyncService` to mark `isPlaying = false`. 
7. **The Broadcast:** The backend pushes the updated `SyncMessage` out to `/topic/room/{roomId}`. All connected clients receive this message instantly and pause their local YouTube players.

---

## 3️⃣ Key Design Choices & Bonus Features

During the code walkthrough, you can highlight these specific architectural decisions that make your code professional and production-ready:

### 🎯 A. The Strategy Pattern (OOP Excellence)
Instead of having a giant `if/else` or `switch` statement inside the WebSocket Controller to handle playing, pausing, joining, and kicking users, the code uses an Interface (`WebSocketMessageHandler`). 
* **Why it's good:** Each event has its own dedicated, isolated class (e.g., `ChangeVideoHandler`, `LeaveRoomHandler`). If you want to add a "Chat" feature later, you simply create a new `ChatHandler` without modifying existing code (adhering to the Open-Closed Principle).

### 🚀 B. Two-Tiered State & Caching
* **`ParticipantRoleCache`:** Every time a user tries to play/pause, the system must check their role. Querying the SQL database on every single scrub/seek event would crash the database under heavy load. By caching roles in a `ConcurrentHashMap`, permission validation is instantaneous.
* **`RoomSyncService`:** The exact second the video is at (e.g., `currentTime: 104.5`) is stored in memory. When a new user joins mid-movie, they query this in-memory service to get the exact millisecond the video is currently at, ensuring perfect sync without database read delays. 

### 🧹 C. Fast Deletions
In the `ParticipantRepository`, a custom `@Query` (`deleteParticipantByIdFast`) is used. Standard JPA `deleteById` performs a `SELECT` query before executing the `DELETE`. The custom query forces an immediate delete, saving processing time when a user disconnects or is kicked.
