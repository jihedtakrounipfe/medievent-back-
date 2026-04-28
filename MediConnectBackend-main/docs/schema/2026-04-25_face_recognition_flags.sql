ALTER TABLE users
    ADD COLUMN face_enabled TINYINT(1) NOT NULL DEFAULT 0 AFTER two_factor_enabled,
    ADD COLUMN face_enrolled TINYINT(1) NOT NULL DEFAULT 0 AFTER face_enabled;
