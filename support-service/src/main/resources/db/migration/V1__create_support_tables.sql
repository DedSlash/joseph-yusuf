CREATE SCHEMA IF NOT EXISTS joseph_support;

CREATE TABLE joseph_support.tickets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    subject      VARCHAR(255) NOT NULL,
    message      TEXT         NOT NULL,
    category     VARCHAR(50)  NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    priority     VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    ai_handled   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    closed_at    TIMESTAMPTZ
);

CREATE INDEX idx_tickets_user     ON joseph_support.tickets(user_id);
CREATE INDEX idx_tickets_status   ON joseph_support.tickets(status);
CREATE INDEX idx_tickets_category ON joseph_support.tickets(category);
CREATE INDEX idx_tickets_created  ON joseph_support.tickets(created_at DESC);

CREATE TABLE joseph_support.ticket_responses (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id       UUID         NOT NULL REFERENCES joseph_support.tickets(id) ON DELETE CASCADE,
    responder_id    UUID         NOT NULL,
    responder_type  VARCHAR(10)  NOT NULL,
    message         TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_responses_ticket  ON joseph_support.ticket_responses(ticket_id);
CREATE INDEX idx_responses_created ON joseph_support.ticket_responses(created_at);

CREATE TABLE joseph_support.knowledge_articles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NOT NULL,
    category    VARCHAR(50)  NOT NULL,
    tags        VARCHAR(500),
    views       INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by  UUID,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_articles_category ON joseph_support.knowledge_articles(category);
CREATE INDEX idx_articles_active   ON joseph_support.knowledge_articles(active);
CREATE INDEX idx_articles_title    ON joseph_support.knowledge_articles(LOWER(title));
