package com.example.app.yt.Model.Dto;

import com.example.app.yt.Model.Participant;
import lombok.Data;
import java.util.List;

@Data
public class SyncMessage {
    private String type;
    private double currentTime;
    private String videoId;

    private String senderId;
    private String senderUsername;

    private String targetUserId;
    private String newRole;

    private List<Participant> participants;
}