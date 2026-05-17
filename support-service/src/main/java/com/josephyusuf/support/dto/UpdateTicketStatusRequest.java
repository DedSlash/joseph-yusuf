package com.josephyusuf.support.dto;

import com.josephyusuf.support.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTicketStatusRequest {

    @NotNull
    private TicketStatus status;
}
