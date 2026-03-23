package com.example.app.yt.Model.Dto;

import com.example.app.yt.Model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponseDto {
    private String id;
    private String username;
    private Role role;
}