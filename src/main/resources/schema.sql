-- Runs on every start after Hibernate (ddl-auto: update) has created the tables.
-- Only things Hibernate cannot generate from the entities: partial indexes.

CREATE INDEX IF NOT EXISTS notifications_retry ON notifications (created_at) WHERE status = 'QUEUED';
