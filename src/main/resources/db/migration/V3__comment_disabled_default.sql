-- Migration: ensure comment_disabled has a safe default and is NOT NULL
-- Sets existing NULL values to 0 and alters column to have NOT NULL DEFAULT 0

UPDATE profiles SET comment_disabled = 0 WHERE comment_disabled IS NULL;
ALTER TABLE profiles MODIFY COLUMN comment_disabled TINYINT(1) NOT NULL DEFAULT 0;

UPDATE users SET comment_disabled = 0 WHERE comment_disabled IS NULL;
ALTER TABLE users MODIFY COLUMN comment_disabled TINYINT(1) NOT NULL DEFAULT 0;
