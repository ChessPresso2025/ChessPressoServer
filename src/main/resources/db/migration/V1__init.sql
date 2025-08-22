-- Extensions für UUIDs
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
    );


CREATE TABLE IF NOT EXISTS user_stats (
                                          user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    wins    INT NOT NULL DEFAULT 0,
    losses  INT NOT NULL DEFAULT 0,
    draws   INT NOT NULL DEFAULT 0
    );
