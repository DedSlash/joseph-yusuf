package com.josephyusuf.subscription.mapper;

import com.josephyusuf.subscription.dto.SubscriptionResponse;
import com.josephyusuf.subscription.dto.TransactionResponse;
import com.josephyusuf.subscription.entity.Subscription;
import com.josephyusuf.subscription.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    @Mapping(target = "couponApplied", source = "stripeCouponId")
    @Mapping(target = "nextInvoiceAmount", ignore = true)
    SubscriptionResponse toResponse(Subscription subscription);

    TransactionResponse toResponse(Transaction transaction);
}
