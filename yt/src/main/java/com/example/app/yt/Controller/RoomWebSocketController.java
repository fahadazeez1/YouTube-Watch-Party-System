package com.example.app.yt.Controller;

import com.example.app.yt.Model.Dto.SyncMessage;
import com.example.app.yt.Model.Participant;
import com.example.app.yt.Model.Role;
import com.example.app.yt.Model.Room;
import com.example.app.yt.Repository.ParticipantRepository;
import com.example.app.yt.Repository.RoomRepository;
import com.example.app.yt.Service.RoomSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class RoomWebSocketController {

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomSyncService roomSyncService;

    private final ConcurrentHashMap<String, Role> roleCache = new ConcurrentHashMap<>();

    @MessageMapping("/room/{roomId}/sync")
    @SendTo("/topic/room/{roomId}")
    public SyncMessage handleSyncEvent(@DestinationVariable String roomId, @Payload SyncMessage message) {

        if ("LEAVE_ROOM".equals(message.getType())) {
            participantRepository.deleteParticipantByIdFast(message.getSenderId());
            roleCache.remove(message.getSenderId());
            return message;
        }

        Role senderRole = roleCache.computeIfAbsent(message.getSenderId(), id -> {
            Optional<Participant> p = participantRepository.findById(id);
            return p.map(Participant::getRole).orElse(null);
        });

        if (senderRole == null) {
            throw new RuntimeException("User not found");
        }

        if ("USER_JOINED".equals(message.getType())) {
            return message;
        }

        if ("REMOVE_PARTICIPANT".equals(message.getType())) {
            if (senderRole != Role.HOST) {
                throw new RuntimeException("Only the Host can remove participants");
            }
            participantRepository.deleteParticipantByIdFast(message.getTargetUserId());
            roleCache.remove(message.getTargetUserId());
            return message;
        }

        if ("ASSIGN_ROLE".equals(message.getType())) {
            if (senderRole != Role.HOST) {
                throw new RuntimeException("Only the Host can assign roles");
            }

            Optional<Participant> targetOpt = participantRepository.findById(message.getTargetUserId());
            if (targetOpt.isPresent()) {
                Participant target = targetOpt.get();
                Role newRole = Role.valueOf(message.getNewRole());
                target.setRole(newRole);
                participantRepository.save(target);

                roleCache.put(message.getTargetUserId(), newRole);
            }
            return message;
        }

        if (senderRole == Role.PARTICIPANT) {
            throw new RuntimeException("Participants cannot control the video");
        }

        if ("PLAY".equals(message.getType()) || "SEEK".equals(message.getType())) {
            roomSyncService.updateRoomState(roomId, null, true, message.getCurrentTime());
        } else if ("PAUSE".equals(message.getType())) {
            roomSyncService.updateRoomState(roomId, null, false, message.getCurrentTime());
        } else if ("CHANGE_VIDEO".equals(message.getType())) {
            // Update high-speed RAM
            roomSyncService.updateRoomState(roomId, message.getVideoId(), false, 0.0);

            // Persist major changes to DB
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                room.setCurrentVideoId(message.getVideoId());
                roomRepository.save(room);
            }
        }

        return message;
    }
}