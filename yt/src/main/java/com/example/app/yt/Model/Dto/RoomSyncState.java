package com.example.app.yt.Model.Dto;

import lombok.Data;

@Data
public class RoomSyncState {
    private String roomId;
    private String currentVideoId;
    private boolean isPlaying;
    private double currentTime;
}