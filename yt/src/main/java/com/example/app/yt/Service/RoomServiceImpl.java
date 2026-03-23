package com.example.app.yt.Service;

import com.example.app.yt.Exception.ResourceNotFoundException;
import com.example.app.yt.Model.Dto.ParticipantResponseDto;
import com.example.app.yt.Model.Dto.RoomResponseDto;
import com.example.app.yt.Model.Dto.RoomSyncState;
import com.example.app.yt.Model.Dto.SyncMessage;
import com.example.app.yt.Model.Participant;
import com.example.app.yt.Model.Role;
import com.example.app.yt.Model.Room;
import com.example.app.yt.Repository.ParticipantRepository;
import com.example.app.yt.Repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final RoomSyncService roomSyncService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository, 
                           ParticipantRepository participantRepository, 
                           RoomSyncService roomSyncService, 
                           SimpMessagingTemplate messagingTemplate) {
        this.roomRepository = roomRepository;
        this.participantRepository = participantRepository;
        this.roomSyncService = roomSyncService;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public RoomResponseDto createRoom(String username) {
        Room room = new Room();
        room.setCurrentVideoId("dQw4w9WgXcQ");

        Participant host = new Participant();
        host.setUsername(username);
        host.setRole(Role.HOST);
        host.setRoom(room);

        room.getParticipants().add(host);
        Room savedRoom = roomRepository.save(room);

        return mapToRoomResponseDto(savedRoom);
    }

    @Override
    @Transactional
    public ParticipantResponseDto joinRoom(String roomId, String username) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        Participant participant = new Participant();
        participant.setUsername(username);
        participant.setRole(Role.PARTICIPANT);
        participant.setRoom(room);

        Participant savedParticipant = participantRepository.save(participant);
        return mapToParticipantResponseDto(savedParticipant);
    }

    @Override
    public RoomResponseDto getRoomDetails(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with ID: " + roomId));

        RoomSyncState liveState = roomSyncService.getRoomState(roomId);
        if (liveState != null) {
            room.setCurrentTime(liveState.getCurrentTime());
            room.setPlaying(liveState.isPlaying());
            if (liveState.getCurrentVideoId() != null) {
                room.setCurrentVideoId(liveState.getCurrentVideoId());
            }
        }

        return mapToRoomResponseDto(room);
    }

    @Override
    @Transactional
    public void leaveRoom(String roomId, String userId) {
        participantRepository.deleteParticipantByIdFast(userId);

        SyncMessage leaveMsg = new SyncMessage();
        leaveMsg.setType("LEAVE_ROOM");
        leaveMsg.setSenderId(userId);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, leaveMsg);
    }


    private RoomResponseDto mapToRoomResponseDto(Room room) {
        List<ParticipantResponseDto> participantDtos = room.getParticipants().stream()
                .map(this::mapToParticipantResponseDto)
                .collect(Collectors.toList());

        return new RoomResponseDto(
                room.getId(),
                room.getCurrentVideoId(),
                room.isPlaying(),
                room.getCurrentTime(),
                participantDtos
        );
    }

    private ParticipantResponseDto mapToParticipantResponseDto(Participant participant) {
        return new ParticipantResponseDto(
                participant.getId(),
                participant.getUsername(),
                participant.getRole()
        );
    }
}