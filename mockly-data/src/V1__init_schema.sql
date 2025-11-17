-- V1__init_schema.sql
-- Создание базовой схемы для пользователей и профилей

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password_hash TEXT NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);

-- Profiles table
CREATE TABLE profiles (
                          user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                          role VARCHAR(20) NOT NULL CHECK (role IN ('CANDIDATE', 'INTERVIEWER')),
                          display_name VARCHAR(100),
                          avatar_url VARCHAR(500),
                          level VARCHAR(50),
                          skills JSONB DEFAULT '[]'::jsonb
);

CREATE INDEX idx_profiles_role ON profiles(role);

-- Sessions table
CREATE TABLE sessions (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          status VARCHAR(20) NOT NULL CHECK (status IN ('SCHEDULED', 'ACTIVE', 'ENDED', 'CANCELED')),
                          starts_at TIMESTAMPTZ,
                          ends_at TIMESTAMPTZ,
                          room_provider VARCHAR(50),
                          room_id VARCHAR(255),
                          recording_id VARCHAR(255),
                          created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sessions_created_by ON sessions(created_by);
CREATE INDEX idx_sessions_status ON sessions(status);
CREATE INDEX idx_sessions_created_at ON sessions(created_at DESC);

-- Session participants
CREATE TABLE session_participants (
                                      id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                      session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
                                      user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                      role_in_session VARCHAR(20) NOT NULL CHECK (role_in_session IN ('CANDIDATE', 'INTERVIEWER')),
                                      joined_at TIMESTAMPTZ,
                                      left_at TIMESTAMPTZ,
                                      UNIQUE(session_id, user_id)
);

CREATE INDEX idx_session_participants_session ON session_participants(session_id);
CREATE INDEX idx_session_participants_user ON session_participants(user_id);

-- Artifacts
CREATE TABLE artifacts (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
                           type VARCHAR(50) NOT NULL CHECK (type IN ('AUDIO_MIXED', 'AUDIO_LEFT', 'AUDIO_RIGHT', 'RAW_WEBRTC')),
                           storage_url TEXT NOT NULL,
                           duration_sec INTEGER,
                           size_bytes BIGINT,
                           created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_artifacts_session ON artifacts(session_id);

-- Transcripts
CREATE TABLE transcripts (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             session_id UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
                             source VARCHAR(20) NOT NULL CHECK (source IN ('CANDIDATE', 'INTERVIEWER', 'MIXED')),
                             text JSONB,
                             words JSONB,
                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transcripts_session ON transcripts(session_id);

-- Reports
CREATE TABLE reports (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         session_id UUID NOT NULL UNIQUE REFERENCES sessions(id) ON DELETE CASCADE,
                         metrics JSONB,
                         summary TEXT,
                         recommendations TEXT,
                         status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSING', 'READY', 'FAILED')),
                         error_message TEXT,
                         created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_reports_session ON reports(session_id);
CREATE INDEX idx_reports_status ON reports(status);

-- Trigger для updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_sessions_updated_at BEFORE UPDATE ON sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON reports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();