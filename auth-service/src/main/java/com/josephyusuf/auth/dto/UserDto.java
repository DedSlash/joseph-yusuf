package com.josephyusuf.auth.dto;

import com.josephyusuf.auth.entity.Plan;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDto {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private Plan plan;
    private boolean enabled;
    private Instant createdAt;
}
