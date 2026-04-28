CREATE DATABASE IF NOT EXISTS mediconnect_db;
CREATE DATABASE IF NOT EXISTS keycloak_db;

GRANT ALL PRIVILEGES ON mediconnect_db.* TO 'mediconnect_user'@'%';
GRANT ALL PRIVILEGES ON keycloak_db.* TO 'mediconnect_user'@'%';
FLUSH PRIVILEGES;+