package com.example.app.yt.Model.Dto;

import lombok.Data;

@Data
public class SyncMessage {
    private String type;
    private double currentTime;
    private String videoId;
    private String senderId;

    private String targetUserId;
    private String newRole;
}