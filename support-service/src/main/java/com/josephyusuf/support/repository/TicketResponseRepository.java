package com.josephyusuf.support.repository;

import com.josephyusuf.support.entity.TicketResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketResponseRepository extends JpaRepository<TicketResponse, UUID> {

    List<TicketResponse> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
