-- Ensure reviews table allows either movie_id or episode_id to be null.
-- This prevents failures when saving a review for a movie (episode_id should be NULL) or for an episode (movie_id should be NULL).

ALTER TABLE reviews
  MODIFY COLUMN episode_id BIGINT NULL;

ALTER TABLE reviews
  MODIFY COLUMN movie_id BIGINT NULL;
