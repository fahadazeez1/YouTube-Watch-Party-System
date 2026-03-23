package com.example.app.yt.Service;

import com.example.app.yt.Model.Dto.RoomSyncState;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomSyncService {

    private final ConcurrentHashMap<String, RoomSyncState> activeRooms = new ConcurrentHashMap<>();

    public RoomSyncState getRoomState(String roomId) {
        return activeRooms.computeIfAbsent(roomId, id -> {
            RoomSyncState newState = new RoomSyncState();
            newState.setRoomId(id);
            newState.setCurrentVideoId("dQw4w9WgXcQ"); // Default video
            newState.setPlaying(false);
            newState.setCurrentTime(0.0);
            return newState;
        });
    }

    public void updateRoomState(String roomId, String videoId, boolean isPlaying, double currentTime) {
        RoomSyncState state = getRoomState(roomId);
        if (videoId != null && !videoId.isEmpty()) {
            state.setCurrentVideoId(videoId);
        }
        state.setPlaying(isPlaying);
        state.setCurrentTime(currentTime);
        
        activeRooms.put(roomId, state);
    }

    public void clearRoomState(String roomId) {
        activeRooms.remove(roomId);
    }
}