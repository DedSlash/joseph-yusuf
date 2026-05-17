package com.josephyusuf.support.dto;

import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTicketRequest {

    @NotBlank
    @Size(max = 255)
    private String subject;

    @NotBlank
    private String message;

    @NotNull
    private TicketCategory category;

    private TicketPriority priority;
}
