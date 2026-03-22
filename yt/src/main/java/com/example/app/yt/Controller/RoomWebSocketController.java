package com.example.app.yt.Controller;

import com.example.app.yt.Model.Dto.SyncMessage;
import com.example.app.yt.Model.Participant;
import com.example.app.yt.Model.Role;
import com.example.app.yt.Model.Room;
import com.example.app.yt.Repository.ParticipantRepository;
import com.example.app.yt.Repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class RoomWebSocketController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @MessageMapping("/room/{roomId}/sync")
    @SendTo("/topic/room/{roomId}")
    public SyncMessage handleSyncEvent(@DestinationVariable String roomId, @Payload SyncMessage message) {

        if ("LEAVE_ROOM".equals(message.getType())) {
            if (participantRepository.existsById(message.getSenderId())) {
                participantRepository.deleteById(message.getSenderId());
            }
            return message;
        }

        Optional<Participant> senderOpt = participantRepository.findById(message.getSenderId());
        if (senderOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        Participant sender = senderOpt.get();

        if ("USER_JOINED".equals(message.getType())) {
            return message;
        }

        if ("REMOVE_PARTICIPANT".equals(message.getType())) {
            if (sender.getRole() != Role.HOST) {
                throw new RuntimeException("Only the Host can remove participants");
            }
            participantRepository.deleteById(message.getTargetUserId());
            return message;
        }

        if ("ASSIGN_ROLE".equals(message.getType())) {
            if (sender.getRole() != Role.HOST) {
                throw new RuntimeException("Only the Host can assign roles");
            }

            Optional<Participant> targetOpt = participantRepository.findById(message.getTargetUserId());
            if (targetOpt.isPresent()) {
                Participant target = targetOpt.get();
                target.setRole(Role.valueOf(message.getNewRole()));
                participantRepository.save(target);
            }
            return message;
        }

        if (sender.getRole() == Role.PARTICIPANT) {
            throw new RuntimeException("Participants cannot control the video");
        }

        Optional<Room> roomOpt = roomRepository.findById(roomId);
        if (roomOpt.isPresent()) {
            Room room = roomOpt.get();
            if ("PLAY".equals(message.getType()) || "SEEK".equals(message.getType())) {
                room.setPlaying(true);
                room.setCurrentTime(message.getCurrentTime());
            } else if ("PAUSE".equals(message.getType())) {
                room.setPlaying(false);
                room.setCurrentTime(message.getCurrentTime());
            } else if ("CHANGE_VIDEO".equals(message.getType())) {
                room.setCurrentVideoId(message.getVideoId());
                room.setPlaying(false);
                room.setCurrentTime(0.0);
            }
            roomRepository.save(room);
        }

        return message;
    }
}