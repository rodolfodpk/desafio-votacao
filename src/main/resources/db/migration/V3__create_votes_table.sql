CREATE TABLE votes (
    id BIGSERIAL PRIMARY KEY,
    agenda_id BIGINT NOT NULL REFERENCES agendas(id),
    cpf VARCHAR(11) NOT NULL,
    vote_value VARCHAR(10) NOT NULL,
    voted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_vote_per_cpf_per_agenda UNIQUE(agenda_id, cpf)
);

CREATE INDEX idx_votes_agenda_id ON votes(agenda_id);
CREATE INDEX idx_votes_cpf ON votes(cpf);

