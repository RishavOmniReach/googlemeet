package com.example.googlemeet.repository;

import com.example.googlemeet.dto.MeetingRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRequestRepository extends JpaRepository<MeetingRequest, Long> {
    Optional<MeetingRequest> findTopByOrganiserAndStatusOrderByCreatedAtDesc(String organiser, String status);
}
