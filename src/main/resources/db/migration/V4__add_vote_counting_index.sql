-- Composite index for efficient vote counting by agenda and vote value
CREATE INDEX idx_votes_agenda_vote ON votes(agenda_id, vote_value);

