package com.example.app.yt.Repository;

import com.example.app.yt.Model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, String> {

    // FAST DELETE: Skips the SELECT count(*) and SELECT * overhead
    @Modifying
    @Transactional
    @Query("DELETE FROM Participant p WHERE p.id = :id")
    void deleteParticipantByIdFast(@Param("id") String id);
}