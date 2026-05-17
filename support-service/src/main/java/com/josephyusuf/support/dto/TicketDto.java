package com.josephyusuf.support.dto;

import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.enums.TicketPriority;
import com.josephyusuf.support.enums.TicketStatus;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDto {

    private UUID id;
    private UUID userId;
    private String subject;
    private String message;
    private TicketCategory category;
    private TicketStatus status;
    private TicketPriority priority;
    private boolean aiHandled;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private List<TicketResponseDto> responses;
}
