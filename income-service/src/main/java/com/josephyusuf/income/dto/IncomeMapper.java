package com.josephyusuf.income.dto;

import com.josephyusuf.income.entity.IncomeEntry;
import com.josephyusuf.income.entity.IncomeSource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IncomeMapper {

    IncomeSourceDto toSourceDto(IncomeSource source);

    @Mapping(target = "incomeSourceId", source = "incomeSource.id")
    @Mapping(target = "incomeSourceName", source = "incomeSource.name")
    @Mapping(target = "currency", source = "incomeSource.currency")
    IncomeEntryDto toEntryDto(IncomeEntry entry);
}
