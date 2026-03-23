package com.example.app.yt.Service;

import com.example.app.yt.Model.Dto.ParticipantResponseDto;
import com.example.app.yt.Model.Dto.RoomResponseDto;

public interface RoomService {
    RoomResponseDto createRoom(String username);
    ParticipantResponseDto joinRoom(String roomId, String username);
    RoomResponseDto getRoomDetails(String roomId);
    void leaveRoom(String roomId, String userId);
}