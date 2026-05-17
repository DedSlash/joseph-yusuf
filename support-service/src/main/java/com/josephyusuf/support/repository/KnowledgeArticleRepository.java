package com.josephyusuf.support.repository;

import com.josephyusuf.support.entity.KnowledgeArticle;
import com.josephyusuf.support.enums.TicketCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, UUID> {

    Page<KnowledgeArticle> findByActiveTrueOrderByViewsDesc(Pageable pageable);

    List<KnowledgeArticle> findByCategoryAndActiveTrueOrderByViewsDesc(TicketCategory category);

    @Query("""
            SELECT a FROM KnowledgeArticle a
            WHERE a.active = true
              AND (LOWER(a.title) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(a.content) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(COALESCE(a.tags, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY a.views DESC
            """)
    List<KnowledgeArticle> search(@Param("q") String query);

    @Modifying
    @Query("UPDATE KnowledgeArticle a SET a.views = a.views + 1 WHERE a.id = :id")
    void incrementViews(@Param("id") UUID id);
}
