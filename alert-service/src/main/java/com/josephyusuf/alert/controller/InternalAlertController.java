package com.josephyusuf.alert.controller;

import com.josephyusuf.alert.dto.AlertDto;
import com.josephyusuf.alert.dto.InternalAlertRequest;
import com.josephyusuf.alert.entity.Alert;
import com.josephyusuf.alert.mapper.AlertMapper;
import com.josephyusuf.alert.service.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts/internal")
@RequiredArgsConstructor
public class InternalAlertController {

    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    private final AlertService alertService;
    private final AlertMapper alertMapper;

    @Value("${app.internal.token:}")
    private String expectedToken;

    @PostMapping
    public ResponseEntity<AlertDto> create(@Valid @RequestBody InternalAlertRequest request,
                                           HttpServletRequest httpRequest) {
        if (expectedToken == null || expectedToken.isBlank()
                || !expectedToken.equals(httpRequest.getHeader(HEADER_INTERNAL_TOKEN))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Alert alert = alertService.createInternal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(alertMapper.toDto(alert));
    }
}
