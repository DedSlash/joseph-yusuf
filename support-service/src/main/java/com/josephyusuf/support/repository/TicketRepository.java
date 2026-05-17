package com.josephyusuf.support.repository;

import com.josephyusuf.support.entity.Ticket;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByStatus(TicketStatus status);

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:category IS NULL OR t.category = :category)
            ORDER BY t.createdAt DESC
            """)
    Page<Ticket> searchTickets(@Param("status") TicketStatus status,
                               @Param("category") TicketCategory category,
                               Pageable pageable);
}
