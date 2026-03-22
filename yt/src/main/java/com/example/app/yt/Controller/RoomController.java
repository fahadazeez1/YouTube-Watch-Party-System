package com.example.app.yt.Controller;

import com.example.app.yt.Model.Participant;
import com.example.app.yt.Model.Role;
import com.example.app.yt.Model.Room;
import com.example.app.yt.Model.Dto.SyncMessage;
import com.example.app.yt.Repository.ParticipantRepository;
import com.example.app.yt.Repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/create")
    public ResponseEntity<Room> createRoom(@RequestParam String username) {
        Room room = new Room();
        room.setCurrentVideoId("dQw4w9WgXcQ");
        room = roomRepository.save(room);

        Participant host = new Participant();
        host.setUsername(username);
        host.setRole(Role.HOST);
        host.setRoom(room);

        host = participantRepository.save(host);
        room.getParticipants().add(host);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<Participant> joinRoom(@PathVariable String roomId, @RequestParam String username) {
        Optional<Room> roomOpt = roomRepository.findById(roomId);

        if (roomOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Participant participant = new Participant();
        participant.setUsername(username);
        participant.setRole(Role.PARTICIPANT);
        participant.setRoom(roomOpt.get());

        Participant savedParticipant = participantRepository.save(participant);
        return ResponseEntity.ok(savedParticipant);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<Room> getRoomDetails(@PathVariable String roomId) {
        return roomRepository.findById(roomId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId, @RequestParam String userId) {
        if (participantRepository.existsById(userId)) {
            participantRepository.deleteById(userId);

            SyncMessage leaveMsg = new SyncMessage();
            leaveMsg.setType("LEAVE_ROOM");
            leaveMsg.setSenderId(userId);
            messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMsg);
        }
        return ResponseEntity.ok().build();
    }
}