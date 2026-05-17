package com.josephyusuf.support.mapper;

import com.josephyusuf.support.dto.TicketDto;
import com.josephyusuf.support.dto.TicketResponseDto;
import com.josephyusuf.support.entity.Ticket;
import com.josephyusuf.support.entity.TicketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {

    @Mapping(target = "responses", ignore = true)
    TicketDto toDto(Ticket ticket);

    TicketResponseDto toResponseDto(TicketResponse response);

    List<TicketResponseDto> toResponseDtoList(List<TicketResponse> responses);
}
