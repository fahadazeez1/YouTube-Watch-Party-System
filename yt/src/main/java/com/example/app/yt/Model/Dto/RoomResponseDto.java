package com.example.app.yt.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseDto {
    private String id;
    private String currentVideoId;
    private boolean isPlaying;
    private double currentTime;
    private List<ParticipantResponseDto> participants;
}