# YouTube Watch Party

A real-time, synchronized YouTube video viewing platform. This application allows users to create watch rooms, invite friends, and watch YouTube videos together with perfectly synchronized play, pause, and seek controls. It features a robust Role-Based Access Control (RBAC) system to manage room permissions.

## 🚀 Live Demo
* **Frontend Application:** https://web3task-xi.vercel.app
* **Backend API/WebSocket:** https://web3task-ytsys.onrender.com

## 🛠️ Tech Stack
* **Frontend:** React, YouTube IFrame API
* **Backend:** Java Spring Boot, Spring WebSockets (STOMP)
* **Database:** MySQL (via Spring Data JPA)
* **Containerization:** Docker & Docker Compose
* **Deployment:** Vercel (Frontend), Render (Backend)

## ✨ Core Features
* **Real-time Synchronization:** WebSockets ensure all participants see the exact same video state (Play, Pause, Seek, and Change Video).
* **Role-Based Access Control (RBAC):**
  * **Host:** Full control over playback, video selection, participant roles, and the ability to kick users.
  * **Moderator:** Can control playback and change videos.
  * **Participant:** Watch-only access.
* **Dynamic UI & Room Management:**
  * **Adaptive Controls:** Video control panels automatically hide for "Participant" roles.
  * **Live Participant List:** Real-time visual list showing who is in the room along with their role badges.
  * **Quick Invites:** 1-click "Copy to Clipboard" feature for the Room ID.
  * **Host Moderation:** Hosts can promote users to Moderators or kick disruptive users (with confirmation prompts). Target users receive browser alerts when promoted or removed.
  * **Graceful Disconnects:** Users are automatically removed from the active participant list if they close their browser or click "Leave".

## 🏃‍♂️ Local Setup & Installation

### Prerequisites
* **Java 17+** (for manual backend setup)
* **Node.js & npm** (for manual frontend setup)
* **MySQL** (for database)
* **Docker & Docker Compose** (optional, for containerized setup)

### 1. Environment Configuration (Required)
Whether you run the app manually or via Docker, the backend requires specific environment variables to connect to the database and configure CORS. 

Create a `.env` file in the backend root directory (or configure your system environment variables) with the following details(Example):

DB_URL=jdbc:mysql://localhost:3306/your_database_name
DB_USERNAME=root
DB_PASSWORD=your_secure_password
FRONTEND_URL=http://localhost:5173(Your_frontend_url_1)
FRONTEND_URL2=http://127.0.0.1:5173(Your_frontend_url_2 )[if not available keep same as url 1]

### 2. Running Manually (Without Docker)

**Backend (Spring Boot):**
1. Ensure your local MySQL server is running and the database referenced in your `DB_URL` is created.
2. Clone the repository: `git clone https://github.com/fahadazeez1/YouTube-Watch-Party-System`
3. Navigate to the backend directory (where the `pom.xml` is located).
4. Run the application using the Maven wrapper: `./mvnw spring-boot:run`
   *(Note: The backend will start on port 8080 and expose the `/ws` WebSocket endpoint).*

**Frontend (React):**
1. Open a new terminal and navigate to the frontend directory.
2. Install the necessary dependencies: `npm install`
3. Start the Vite development server: `npm run dev`
4. Access the application in your browser at `http://localhost:5173`.

### 3. Running with Docker (Alternative)
If you prefer to run the application using containers, ensure your `.env` file is created as shown in Step 1. Then, from the root directory, run:

docker-compose --env-file .env up --build

This will spin up the backend, frontend, and database containers automatically.

---

## 📡 WebSocket Event Architecture
The backend utilizes Spring WebSockets with STOMP messaging. Clients connect via the `/ws` endpoint and subscribe to `/topic/room/{roomId}`.

**Supported Event Types (Routed via `WebSocketMessageDispatcher`):**

* **Playback Controls (`VideoControlHandler`)**
  * `PLAY`, `PAUSE`, `SEEK`: Synchronizes the video state. Validated on the backend so only Hosts and Moderators can trigger these.
* **Video Management (`ChangeVideoHandler`)**
  * `CHANGE_VIDEO`: Updates the `currentVideoId` for the room and broadcasts the new video to all users.
* **Room & User Management (`RoleManagementHandler` & `UserJoinedHandler`)**
  * `USER_JOINED`: Broadcasts to notify the room of a new arrival.
  * `LEAVE_ROOM`: Cleans up the user session and notifies the room.
  * `ASSIGN_ROLE`: Host-only action to promote/demote users.
  * `REMOVE_PARTICIPANT`: Host-only action to kick users from the session.
 

 
