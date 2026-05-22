package com.josephyusuf.auth.controller;

import com.josephyusuf.auth.dto.WaitlistJoinRequest;
import com.josephyusuf.auth.dto.WaitlistJoinResponse;
import com.josephyusuf.auth.service.WaitlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/waitlist")
@RequiredArgsConstructor
public class WaitlistController {

    private final WaitlistService waitlistService;

    @PostMapping("/join")
    public ResponseEntity<WaitlistJoinResponse> join(@Valid @RequestBody WaitlistJoinRequest request) {
        return ResponseEntity.ok(waitlistService.join(request));
    }
}
