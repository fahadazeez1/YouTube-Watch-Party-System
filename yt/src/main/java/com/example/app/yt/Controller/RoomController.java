package com.example.app.yt.Controller;

import com.example.app.yt.Model.Dto.ParticipantResponseDto;
import com.example.app.yt.Model.Dto.RoomResponseDto;
import com.example.app.yt.Service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public ResponseEntity<RoomResponseDto> createRoom(@RequestParam String username) {
        RoomResponseDto response = roomService.createRoom(username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<ParticipantResponseDto> joinRoom(
            @PathVariable String roomId,
            @RequestParam String username) {

        ParticipantResponseDto response = roomService.joinRoom(roomId, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponseDto> getRoomDetails(@PathVariable String roomId) {
        RoomResponseDto response = roomService.getRoomDetails(roomId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomId,
            @RequestParam String userId) {

        roomService.leaveRoom(roomId, userId);
        return ResponseEntity.ok().build();
    }
}