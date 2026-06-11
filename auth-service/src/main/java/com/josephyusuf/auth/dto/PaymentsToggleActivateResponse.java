package com.josephyusuf.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsToggleActivateResponse {

    private boolean paymentsActive;
    private int usersNotified;
    private int usersInOriginalTrial;
    private int usersInGrace24h;
    private boolean alreadyActive;
}
