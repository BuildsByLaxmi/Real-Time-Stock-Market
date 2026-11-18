-- Real-Time Stock Market Dashboard - database schema (MySQL 8+)
-- Owner: Member 1 (Authentication & Database)
--
-- NOTE: the Spring Boot app creates/updates these tables automatically
-- (spring.jpa.hibernate.ddl-auto=update). This file documents the schema and
-- lets you create the database manually if you prefer.

CREATE DATABASE IF NOT EXISTS stockmarket
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE stockmarket;

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  email         VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,          -- BCrypt hash, never plain text
  full_name     VARCHAR(100) NOT NULL,
  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS watchlist (
  id        BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id   BIGINT      NOT NULL,
  symbol    VARCHAR(20) NOT NULL,
  added_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_watchlist_user_symbol UNIQUE (user_id, symbol),
  CONSTRAINT fk_watchlist_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_watchlist_user ON watchlist(user_id);
