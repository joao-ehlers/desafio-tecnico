ALTER TABLE messages
    ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE messages
    ADD CONSTRAINT ck_messages_attempts_non_negative
        CHECK (attempts >= 0);

CREATE INDEX idx_messages_due_retries
    ON messages (next_attempt_at, id)
    WHERE status = 'FAILED'
      AND next_attempt_at IS NOT NULL;