CREATE TABLE IF NOT EXISTS fanpages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    page_code TEXT NOT NULL UNIQUE,
    page_name TEXT NOT NULL,
    page_url TEXT,
    niche TEXT,
    default_video_count INTEGER NOT NULL DEFAULT 6,
    start_date TEXT NOT NULL DEFAULT (date('now')),
    active INTEGER NOT NULL DEFAULT 1,
    created_time TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS sources (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fanpage_id INTEGER NOT NULL,
    source_code TEXT NOT NULL UNIQUE,
    source_name TEXT NOT NULL,
    source_type TEXT NOT NULL,
    source_url TEXT NOT NULL,
    channel_name TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (fanpage_id) REFERENCES fanpages(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_sources_one_active_per_fanpage
ON sources(fanpage_id)
WHERE active = 1;

CREATE TABLE IF NOT EXISTS download_batches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    fanpage_id INTEGER NOT NULL,
    source_id INTEGER NOT NULL,
    plan_date TEXT NOT NULL DEFAULT (date('now')),
    requested_count INTEGER NOT NULL,
    downloaded_count INTEGER NOT NULL DEFAULT 0,
    thread_count INTEGER NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'PLANNED',
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (fanpage_id) REFERENCES fanpages(id),
    FOREIGN KEY (source_id) REFERENCES sources(id)
);

CREATE TABLE IF NOT EXISTS video_batches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_code TEXT NOT NULL UNIQUE,
    fanpage_id INTEGER NOT NULL,
    source_id INTEGER NOT NULL,
    download_batch_id INTEGER,
    batch_date TEXT NOT NULL DEFAULT (date('now')),
    batch_index INTEGER NOT NULL,
    video_count INTEGER NOT NULL DEFAULT 0,
    raw_folder_path TEXT,
    edited_folder_path TEXT,
    status TEXT NOT NULL DEFAULT 'DOWNLOADED',
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (fanpage_id) REFERENCES fanpages(id),
    FOREIGN KEY (source_id) REFERENCES sources(id),
    FOREIGN KEY (download_batch_id) REFERENCES download_batches(id)
);

CREATE TABLE IF NOT EXISTS videos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id INTEGER,
    source_id INTEGER NOT NULL,
    fanpage_id INTEGER NOT NULL,
    platform_video_id TEXT NOT NULL,
    title TEXT,
    caption TEXT,
    channel_name TEXT,
    original_url TEXT,
    duration_seconds INTEGER,
    downloaded_time TEXT,
    uploaded_time TEXT,
    status TEXT NOT NULL DEFAULT 'DOWNLOADED',
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (batch_id) REFERENCES video_batches(id),
    FOREIGN KEY (source_id) REFERENCES sources(id),
    FOREIGN KEY (fanpage_id) REFERENCES fanpages(id),

    UNIQUE(source_id, platform_video_id)
);

CREATE INDEX IF NOT EXISTS idx_videos_source_id
ON videos(source_id);

CREATE INDEX IF NOT EXISTS idx_videos_fanpage_id
ON videos(fanpage_id);

CREATE INDEX IF NOT EXISTS idx_videos_batch_id
ON videos(batch_id);

CREATE INDEX IF NOT EXISTS idx_videos_status
ON videos(status);

CREATE TABLE IF NOT EXISTS video_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_id INTEGER NOT NULL,
    file_type TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_exists INTEGER NOT NULL DEFAULT 1,
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE INDEX IF NOT EXISTS idx_video_files_video_id
ON video_files(video_id);

CREATE TABLE IF NOT EXISTS upload_batches (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    video_batch_id INTEGER,
    fanpage_id INTEGER NOT NULL,
    source_id INTEGER NOT NULL,
    upload_date TEXT NOT NULL DEFAULT (date('now')),
    video_count INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'UPLOADED_MARKED',
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (video_batch_id) REFERENCES video_batches(id),
    FOREIGN KEY (fanpage_id) REFERENCES fanpages(id),
    FOREIGN KEY (source_id) REFERENCES sources(id)
);

CREATE TABLE IF NOT EXISTS upload_batch_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    upload_batch_id INTEGER NOT NULL,
    video_id INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'UPLOADED_MARKED',
    created_time TEXT NOT NULL DEFAULT (datetime('now')),

    FOREIGN KEY (upload_batch_id) REFERENCES upload_batches(id),
    FOREIGN KEY (video_id) REFERENCES videos(id)
);

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL
);

INSERT OR IGNORE INTO app_settings(setting_key, setting_value)
VALUES ('backup_interval_days', '3');

INSERT OR IGNORE INTO app_settings(setting_key, setting_value)
VALUES ('default_video_count', '6');

INSERT OR IGNORE INTO app_settings(setting_key, setting_value)
VALUES ('default_download_thread_count', '1');

INSERT OR IGNORE INTO app_settings(setting_key, setting_value)
VALUES ('video_per_folder_batch', '6');

CREATE TABLE IF NOT EXISTS source_content_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,

    source_id INTEGER NOT NULL,
    source_type TEXT NOT NULL,

    platform_post_id TEXT NOT NULL,
    url TEXT,
    title TEXT,

    content_type TEXT NOT NULL DEFAULT 'UNKNOWN',
    check_status TEXT NOT NULL DEFAULT 'OK',

    checked_time TEXT NOT NULL DEFAULT (datetime('now')),
    last_seen_time TEXT NOT NULL DEFAULT (datetime('now')),

    message TEXT,

    UNIQUE(source_id, platform_post_id)
);

CREATE INDEX IF NOT EXISTS idx_source_content_cache_source_type
ON source_content_cache(source_id, content_type);