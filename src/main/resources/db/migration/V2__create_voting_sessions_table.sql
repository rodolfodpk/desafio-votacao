CREATE TABLE voting_sessions (
    id BIGSERIAL PRIMARY KEY,
    agenda_id BIGINT NOT NULL REFERENCES agendas(id),
    duration_minutes INTEGER NOT NULL,
    end_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_voting_sessions_agenda_id ON voting_sessions(agenda_id);

