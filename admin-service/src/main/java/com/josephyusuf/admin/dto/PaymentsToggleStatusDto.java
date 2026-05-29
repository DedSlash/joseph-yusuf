package com.josephyusuf.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsToggleStatusDto {

    private boolean paymentsActive;
    private long usersInTrialExtension;
}
