package com.josephyusuf.income.tips.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyTipDto {

    private String id;
    private String title;
    private String description;
    private String icon;
    private String method;
    private List<String> countries;
    private String requiredPlan;
    private boolean locked;
    private String actionUrl;
    private String actionLabel;
}
