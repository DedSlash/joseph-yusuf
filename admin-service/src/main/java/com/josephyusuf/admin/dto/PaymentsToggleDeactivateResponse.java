package com.josephyusuf.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsToggleDeactivateResponse {

    private boolean paymentsActive;
    private int usersRestored;
    private int usersExtended;
    private int usersInOriginalTrial;
    private boolean alreadyInactive;
}
