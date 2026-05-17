package com.josephyusuf.support.dto;

import com.josephyusuf.support.enums.ResponderType;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponseDto {

    private UUID id;
    private UUID ticketId;
    private UUID responderId;
    private ResponderType responderType;
    private String message;
    private Instant createdAt;
}
