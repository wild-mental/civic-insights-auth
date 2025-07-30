-- src/test/resources/schema.sql
-- H2 Database compatible schema for testing

DROP TABLE IF EXISTS user_profiles;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password VARCHAR(255) NULL,
  name VARCHAR(255) NOT NULL,
  provider VARCHAR(50) NOT NULL,
  provider_id VARCHAR(255) NULL,
  role VARCHAR(50) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (email)
);

CREATE TABLE user_profiles (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  bio TEXT NULL,
  location VARCHAR(255) NULL,
  website VARCHAR(255) NULL,
  phone_number VARCHAR(50) NULL,
  avatar_url VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE (user_id),
  CONSTRAINT fk_user_profiles_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
); 