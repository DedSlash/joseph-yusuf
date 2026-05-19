-- V3: Add plan restriction columns to knowledge_articles

ALTER TABLE joseph_support.knowledge_articles
    ADD COLUMN required_plan VARCHAR(20) DEFAULT 'FREE',
    ADD COLUMN preview_content TEXT;
