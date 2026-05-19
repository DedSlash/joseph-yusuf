package com.josephyusuf.support.service;

import com.josephyusuf.support.dto.ArticleDto;
import com.josephyusuf.support.dto.ArticleRequest;
import com.josephyusuf.support.entity.KnowledgeArticle;
import com.josephyusuf.support.enums.TicketCategory;
import com.josephyusuf.support.exception.ArticleNotFoundException;
import com.josephyusuf.support.mapper.ArticleMapper;
import com.josephyusuf.support.mapper.ArticleMapperImpl;
import com.josephyusuf.support.repository.KnowledgeArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock
    private KnowledgeArticleRepository articleRepository;

    @Spy
    private ArticleMapper articleMapper = new ArticleMapperImpl();

    @InjectMocks
    private KnowledgeService knowledgeService;

    private UUID articleId;
    private KnowledgeArticle article;

    @BeforeEach
    void setUp() {
        articleId = UUID.randomUUID();
        article = KnowledgeArticle.builder()
                .id(articleId)
                .title("Comment réinitialiser son mot de passe")
                .content("Cliquer sur ...")
                .category(TicketCategory.ACCOUNT)
                .tags("reset,password")
                .views(3)
                .active(true)
                .build();
    }

    @Test
    void search_returnsEmpty_whenQueryBlank() {
        assertThat(knowledgeService.search(null, "FREE")).isEmpty();
        assertThat(knowledgeService.search("  ", "FREE")).isEmpty();
        verify(articleRepository, never()).search(any());
    }

    @Test
    void search_delegatesToRepository() {
        when(articleRepository.search("mot")).thenReturn(List.of(article));

        List<ArticleDto> result = knowledgeService.search("mot", "FREE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).contains("mot de passe");
    }

    @Test
    void listByCategory_returnsArticles() {
        when(articleRepository.findByCategoryAndActiveTrueOrderByViewsDesc(TicketCategory.ACCOUNT))
                .thenReturn(List.of(article));

        List<ArticleDto> result = knowledgeService.listByCategory(TicketCategory.ACCOUNT, "FREE");

        assertThat(result).hasSize(1);
    }

    @Test
    void listPublic_returnsPaginatedContent() {
        Page<KnowledgeArticle> page = new PageImpl<>(List.of(article));
        when(articleRepository.findByActiveTrueOrderByViewsDesc(any())).thenReturn(page);

        List<ArticleDto> result = knowledgeService.listPublic(0, 20, "FREE");

        assertThat(result).hasSize(1);
    }

    @Test
    void getAndIncrement_incrementsViews_whenActive() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        ArticleDto dto = knowledgeService.getAndIncrement(articleId, "PREMIUM");

        verify(articleRepository).incrementViews(articleId);
        assertThat(dto.getViews()).isEqualTo(4);
    }

    @Test
    void getAndIncrement_skipsIncrement_whenInactive() {
        article.setActive(false);
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        knowledgeService.getAndIncrement(articleId, "FREE");

        verify(articleRepository, never()).incrementViews(any());
    }

    @Test
    void getAndIncrement_throwsNotFound_whenMissing() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> knowledgeService.getAndIncrement(articleId, "FREE"))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    void search_locksArticle_whenFreeUserAndPremiumRequired() {
        article.setRequiredPlan("PREMIUM");
        article.setPreviewContent("Extrait...");
        when(articleRepository.search("mot")).thenReturn(List.of(article));

        List<ArticleDto> result = knowledgeService.search("mot", "FREE");

        assertThat(result.get(0).isLocked()).isTrue();
        assertThat(result.get(0).getContent()).isNull();
        assertThat(result.get(0).getPreviewContent()).isEqualTo("Extrait...");
    }

    @Test
    void search_unlocksArticle_whenPremiumUserAndPremiumRequired() {
        article.setRequiredPlan("PREMIUM");
        article.setPreviewContent("Extrait...");
        when(articleRepository.search("mot")).thenReturn(List.of(article));

        List<ArticleDto> result = knowledgeService.search("mot", "PREMIUM");

        assertThat(result.get(0).isLocked()).isFalse();
        assertThat(result.get(0).getContent()).isNotNull();
    }

    @Test
    void search_premiumPlusUnlocksAll() {
        article.setRequiredPlan("PREMIUM");
        when(articleRepository.search("mot")).thenReturn(List.of(article));

        List<ArticleDto> result = knowledgeService.search("mot", "PREMIUM_PLUS");

        assertThat(result.get(0).isLocked()).isFalse();
    }

    @Test
    void create_persistsArticle_withAdminId() {
        UUID adminId = UUID.randomUUID();
        ArticleRequest req = ArticleRequest.builder()
                .title("T")
                .content("C")
                .category(TicketCategory.TECHNICAL)
                .tags("a,b")
                .active(true)
                .build();
        when(articleRepository.save(any(KnowledgeArticle.class))).thenAnswer(inv -> {
            KnowledgeArticle a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        ArticleDto dto = knowledgeService.create(adminId, req);

        assertThat(dto.getTitle()).isEqualTo("T");
        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void create_defaultsActiveTrue_whenActiveNull() {
        ArticleRequest req = ArticleRequest.builder()
                .title("T")
                .content("C")
                .category(TicketCategory.RULES)
                .build();
        when(articleRepository.save(any(KnowledgeArticle.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticleDto dto = knowledgeService.create(UUID.randomUUID(), req);

        assertThat(dto.isActive()).isTrue();
    }

    @Test
    void update_modifiesFields() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(KnowledgeArticle.class))).thenAnswer(inv -> inv.getArgument(0));

        ArticleRequest req = ArticleRequest.builder()
                .title("Nouveau titre")
                .content("Nouveau contenu")
                .category(TicketCategory.SUBSCRIPTION)
                .tags("new")
                .active(false)
                .build();

        ArticleDto dto = knowledgeService.update(articleId, req);

        assertThat(dto.getTitle()).isEqualTo("Nouveau titre");
        assertThat(dto.getCategory()).isEqualTo(TicketCategory.SUBSCRIPTION);
        assertThat(dto.isActive()).isFalse();
    }

    @Test
    void update_throwsNotFound_whenMissing() {
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        ArticleRequest req = ArticleRequest.builder()
                .title("x").content("y").category(TicketCategory.OTHER).build();

        assertThatThrownBy(() -> knowledgeService.update(articleId, req))
                .isInstanceOf(ArticleNotFoundException.class);
    }

    @Test
    void delete_deletesById() {
        when(articleRepository.existsById(articleId)).thenReturn(true);

        knowledgeService.delete(articleId);

        verify(articleRepository).deleteById(articleId);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(articleRepository.existsById(articleId)).thenReturn(false);

        assertThatThrownBy(() -> knowledgeService.delete(articleId))
                .isInstanceOf(ArticleNotFoundException.class);
    }
}
