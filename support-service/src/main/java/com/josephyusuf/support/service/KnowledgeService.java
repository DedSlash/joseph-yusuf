package com.josephyusuf.support.service;

import com.josephyusuf.support.dto.ArticleDto;
import com.josephyusuf.support.dto.ArticleRequest;
import com.josephyusuf.support.entity.KnowledgeArticle;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.exception.ArticleNotFoundException;
import com.josephyusuf.support.mapper.ArticleMapper;
import com.josephyusuf.support.repository.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KnowledgeArticleRepository articleRepository;
    private final ArticleMapper articleMapper;

    @Transactional(readOnly = true)
    public List<ArticleDto> search(String query, String userPlan) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return articleRepository.search(query.trim()).stream()
                .map(a -> toLockedDto(a, userPlan))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleDto> listByCategory(TicketCategory category, String userPlan) {
        return articleRepository.findByCategoryAndActiveTrueOrderByViewsDesc(category).stream()
                .map(a -> toLockedDto(a, userPlan))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArticleDto> listPublic(int page, int size, String userPlan) {
        Pageable pageable = PageRequest.of(page, size);
        Page<KnowledgeArticle> result = articleRepository.findByActiveTrueOrderByViewsDesc(pageable);
        return result.getContent().stream()
                .map(a -> toLockedDto(a, userPlan))
                .toList();
    }

    @Transactional
    public ArticleDto getAndIncrement(UUID id, String userPlan) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article introuvable : " + id));
        if (article.isActive()) {
            articleRepository.incrementViews(id);
            article.setViews(article.getViews() + 1);
        }
        return toLockedDto(article, userPlan);
    }

    @Transactional
    public ArticleDto create(UUID adminId, ArticleRequest request) {
        KnowledgeArticle article = KnowledgeArticle.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .tags(request.getTags())
                .requiredPlan(request.getRequiredPlan())
                .previewContent(request.getPreviewContent())
                .active(request.getActive() == null || request.getActive())
                .createdBy(adminId)
                .build();
        return articleMapper.toDto(articleRepository.save(article));
    }

    @Transactional
    public ArticleDto update(UUID id, ArticleRequest request) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new ArticleNotFoundException("Article introuvable : " + id));
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setCategory(request.getCategory());
        article.setTags(request.getTags());
        article.setRequiredPlan(request.getRequiredPlan());
        article.setPreviewContent(request.getPreviewContent());
        if (request.getActive() != null) {
            article.setActive(request.getActive());
        }
        return articleMapper.toDto(articleRepository.save(article));
    }

    @Transactional
    public void delete(UUID id) {
        if (!articleRepository.existsById(id)) {
            throw new ArticleNotFoundException("Article introuvable : " + id);
        }
        articleRepository.deleteById(id);
    }

    private ArticleDto toLockedDto(KnowledgeArticle article, String userPlan) {
        ArticleDto dto = articleMapper.toDto(article);
        dto.setLanguage(article.getLanguage());
        dto.setRequiredPlan(article.getRequiredPlan());
        dto.setPreviewContent(article.getPreviewContent());

        if (article.getRequiredPlan() != null && !"FREE".equals(article.getRequiredPlan())) {
            boolean hasAccess = planHasAccess(userPlan, article.getRequiredPlan());
            dto.setLocked(!hasAccess);
            if (!hasAccess) {
                dto.setContent(null);
            }
        }
        return dto;
    }

    private boolean planHasAccess(String userPlan, String requiredPlan) {
        if (requiredPlan == null || "FREE".equals(requiredPlan)) return true;
        if ("PREMIUM_PLUS".equals(userPlan)) return true;
        if ("PREMIUM".equals(userPlan)) return "PREMIUM".equals(requiredPlan);
        return false;
    }
}
