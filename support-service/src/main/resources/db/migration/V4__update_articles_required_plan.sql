-- V4: Set required_plan on premium articles

UPDATE joseph_support.knowledge_articles
SET required_plan = 'PREMIUM',
    preview_content = LEFT(content, 200) || '…'
WHERE title ILIKE '%import%historique%'
   OR title ILIKE '%import%income%'
   OR title ILIKE '%import%history%'
   OR title ILIKE '%règle Joseph%'
   OR title ILIKE '%configurer%Règle Joseph%'
   OR title ILIKE '%Joseph Rule%'
   OR title ILIKE '%configure%Joseph Rule%'
   OR title ILIKE '%recommandation%épargne%'
   OR title ILIKE '%savings recommendation%'
   OR title ILIKE '%rapport PDF%'
   OR title ILIKE '%PDF report%';
