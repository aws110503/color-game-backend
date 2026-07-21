-- ============================================
-- Color Game — Script de création de la base de données
-- PostgreSQL 14+
-- ============================================

-- Créer la base de données (à exécuter séparément si elle n'existe pas déjà)
-- CREATE DATABASE color_game_db;

-- Se connecter à color_game_db avant d'exécuter la suite

-- ============================================
-- Table : users
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- Table : grid_history
-- ============================================
CREATE TABLE IF NOT EXISTS grid_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    row_index INTEGER NOT NULL,
    col_index INTEGER NOT NULL,
    color VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- Table : security_audit_log
-- ============================================
CREATE TABLE IF NOT EXISTS security_audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    user_id BIGINT,
    ip_address VARCHAR(45) NOT NULL,
    endpoint VARCHAR(255) NOT NULL,
    details TEXT,
    occurred_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_event_type ON security_audit_log(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON security_audit_log(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_ip_address ON security_audit_log(ip_address);

-- ============================================
-- Table : refresh_tokens
-- ============================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_refresh_token ON refresh_tokens(token);

-- ============================================
-- Table : jwt_blacklist
-- ============================================
CREATE TABLE IF NOT EXISTS jwt_blacklist (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    blacklisted_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_jwt_blacklist_jti ON jwt_blacklist(jti);

-- ============================================
-- Données initiales : compte ADMIN
-- Mot de passe hashé en BCrypt (généré via bcrypt-generator.com)
-- Remplacez le hash ci-dessous par le vôtre si vous régénérez le projet
-- ============================================
INSERT INTO users (username, email, password, role, created_at)
VALUES (
    'admin',
    'admin@colorgame.com',
    '$2a$12$8epvt1Rs2U4Vuzp8QNEL5.PxarMyxCUZauNtSKV4Q9U0TYvKRsWyu',
    'ADMIN',
    NOW()
)
ON CONFLICT (username) DO NOTHING;