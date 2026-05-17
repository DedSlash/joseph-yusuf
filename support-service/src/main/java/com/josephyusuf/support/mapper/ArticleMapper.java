package com.josephyusuf.support.mapper;

import com.josephyusuf.support.dto.ArticleDto;
import com.josephyusuf.support.entity.KnowledgeArticle;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ArticleMapper {

    ArticleDto toDto(KnowledgeArticle article);
}
