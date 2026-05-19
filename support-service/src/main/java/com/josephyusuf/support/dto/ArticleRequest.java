package com.josephyusuf.support.dto;

import com.josephyusuf.support.enums.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private TicketCategory category;

    @Size(max = 500)
    private String tags;

    @Size(max = 20)
    private String requiredPlan;

    private String previewContent;

    private Boolean active;
}
