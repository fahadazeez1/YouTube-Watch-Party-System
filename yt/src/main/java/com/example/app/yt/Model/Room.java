package com.example.app.yt.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Entity
@Table(name = "rooms")
@Getter
@Setter
@NoArgsConstructor
public class Room implements Persistable<String> {

    @Id
    private String id = generateShortCode();

    private String currentVideoId;

    private boolean isPlaying = false;

    @Column(name = "video_position")
    private double currentTime = 0.0;

    // FIX: Removed @JsonIgnore so the frontend can receive the generated Host ID
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participant> participants = new ArrayList<>();

    @Transient
    @JsonIgnore
    private boolean isNew = true;

    @Override
    @JsonIgnore
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    private String generateShortCode() {
        Random random = new Random();
        int number = random.nextInt(1000000);
        return String.format("%06d", number);
    }
}