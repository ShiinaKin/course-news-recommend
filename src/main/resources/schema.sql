CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS user_tags (
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, tag_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS articles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content CLOB NOT NULL,
    source VARCHAR(128),
    publish_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS article_tags (
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    weight DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS knowledge_entities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL,
    external_id VARCHAR(128),
    name VARCHAR(255),
    description CLOB,
    modality VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (entity_type, external_id)
);
CREATE INDEX IF NOT EXISTS idx_knowledge_entities_type ON knowledge_entities (entity_type);

CREATE TABLE IF NOT EXISTS knowledge_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    modality VARCHAR(32) DEFAULT '',
    weight DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (source_id) REFERENCES knowledge_entities (id) ON DELETE CASCADE,
    FOREIGN KEY (target_id) REFERENCES knowledge_entities (id) ON DELETE CASCADE,
    CHECK (source_id <> target_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_knowledge_relations_unique
    ON knowledge_relations (source_id, target_id, relation_type, modality);
CREATE INDEX IF NOT EXISTS idx_knowledge_relations_type ON knowledge_relations (relation_type);

CREATE TABLE IF NOT EXISTS user_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    event_type VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (article_id) REFERENCES articles (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_user_events_user_id ON user_events (user_id);
CREATE INDEX IF NOT EXISTS idx_user_events_article_id ON user_events (article_id);

CREATE TABLE IF NOT EXISTS media_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(16) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    status VARCHAR(16) NOT NULL,
    result_text CLOB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_media_jobs_user_status ON media_jobs (user_id, status);
