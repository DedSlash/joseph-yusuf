package com.josephyusuf.income.savings.dto;

import com.josephyusuf.income.savings.entity.SavingsContribution;
import com.josephyusuf.income.savings.entity.SavingsGoal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SavingsMapper {

    @Mapping(target = "progressPercent", ignore = true)
    @Mapping(target = "projectedCompletionDate", ignore = true)
    SavingsGoalDto toGoalDto(SavingsGoal goal);

    SavingsContributionDto toContributionDto(SavingsContribution contribution);
}
