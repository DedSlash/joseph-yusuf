package com.josephyusuf.admin.exception;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private Instant timestamp;
    private int status;
    private String message;
}
