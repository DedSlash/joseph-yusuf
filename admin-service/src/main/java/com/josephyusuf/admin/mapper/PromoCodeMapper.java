package com.josephyusuf.admin.mapper;

import com.josephyusuf.admin.dto.PromoCodeResponse;
import com.josephyusuf.admin.entity.PromoCode;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PromoCodeMapper {

    PromoCodeResponse toResponse(PromoCode promoCode);
}
