PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT COLLATE NOCASE UNIQUE,
  password_hash TEXT,
  password_salt TEXT,
  handle TEXT NOT NULL COLLATE NOCASE UNIQUE,
  city TEXT NOT NULL DEFAULT '',
  bio TEXT NOT NULL DEFAULT '',
  avatar_url TEXT,
  created_at INTEGER NOT NULL DEFAULT (unixepoch()),
  CHECK (
    (email IS NULL AND password_hash IS NULL AND password_salt IS NULL)
    OR (email IS NOT NULL AND password_hash IS NOT NULL AND password_salt IS NOT NULL)
  )
);

CREATE TABLE IF NOT EXISTS gems (
  id TEXT PRIMARY KEY,
  author_id TEXT REFERENCES users(id) ON DELETE SET NULL,
  title TEXT NOT NULL,
  city TEXT NOT NULL,
  neighborhood TEXT NOT NULL DEFAULT '',
  category TEXT NOT NULL,
  tags TEXT NOT NULL DEFAULT '',
  tip TEXT NOT NULL DEFAULT '',
  image_url TEXT,
  latitude REAL,
  longitude REAL,
  source TEXT NOT NULL DEFAULT 'user',
  created_at INTEGER NOT NULL DEFAULT (unixepoch()),
  updated_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE TABLE IF NOT EXISTS follows (
  follower_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  following_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (follower_id, following_id),
  CHECK (follower_id <> following_id)
);

CREATE TABLE IF NOT EXISTS saved_gems (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  gem_id TEXT NOT NULL REFERENCES gems(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (user_id, gem_id)
);

CREATE TABLE IF NOT EXISTS visited_gems (
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  gem_id TEXT NOT NULL REFERENCES gems(id) ON DELETE CASCADE,
  created_at INTEGER NOT NULL DEFAULT (unixepoch()),
  PRIMARY KEY (user_id, gem_id)
);

CREATE TABLE IF NOT EXISTS sessions (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  access_hash TEXT NOT NULL UNIQUE,
  refresh_hash TEXT NOT NULL UNIQUE,
  access_expires_at INTEGER NOT NULL,
  refresh_expires_at INTEGER NOT NULL,
  created_at INTEGER NOT NULL DEFAULT (unixepoch())
);

CREATE INDEX IF NOT EXISTS idx_gems_city_category ON gems(city, category);
CREATE INDEX IF NOT EXISTS idx_gems_author ON gems(author_id);
CREATE INDEX IF NOT EXISTS idx_gems_created ON gems(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sessions_access ON sessions(access_hash);
CREATE INDEX IF NOT EXISTS idx_sessions_refresh ON sessions(refresh_hash);
