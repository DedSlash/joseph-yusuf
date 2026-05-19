package com.josephyusuf.support.dto;

import com.josephyusuf.support.enums.TicketCategory;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleDto {

    private UUID id;
    private String title;
    private String content;
    private TicketCategory category;
    private String tags;
    private String language;
    private String requiredPlan;
    private String previewContent;
    private boolean locked;
    private int views;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
