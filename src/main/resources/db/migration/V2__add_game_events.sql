-- Add game events table for audit logging
CREATE TABLE IF NOT EXISTS game_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type TEXT NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_game_events_user_id ON game_events(user_id);
CREATE INDEX IF NOT EXISTS idx_game_events_created_at ON game_events(created_at);
CREATE INDEX IF NOT EXISTS idx_game_events_type ON game_events(type);
