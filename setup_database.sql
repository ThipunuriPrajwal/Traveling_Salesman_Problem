-- ============================================================
--  TSP Optimizer — MySQL Database Setup
--  Run this once before starting the application:
--    mysql -u root -p < setup_database.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS tsp_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE tsp_db;

-- ---------------------------------------------------------
-- Table 1: Session log — one row per successful solve
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS tsp_sessions (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    session_time  DATETIME     NOT NULL COMMENT 'When the solve ran',
    num_nodes     INT          NOT NULL COMMENT 'Number of cities',
    optimal_cost  INT          NOT NULL COMMENT 'Total route distance',
    optimal_path  VARCHAR(500) NOT NULL COMMENT 'City sequence, e.g. 0→1→3→2→0',
    solve_time_ms BIGINT       NOT NULL COMMENT 'Wall-clock ms for Held-Karp',
    status        VARCHAR(50)  DEFAULT 'SUCCESS'
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Table 2: Distance matrix — one row per edge per session
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS tsp_distance_matrix (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    session_id  INT NOT NULL,
    from_node   INT NOT NULL,
    to_node     INT NOT NULL,
    distance    INT NOT NULL,
    FOREIGN KEY (session_id) REFERENCES tsp_sessions(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Table 3: Error log — validation & DB errors
-- ---------------------------------------------------------
CREATE TABLE IF NOT EXISTS tsp_error_logs (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    log_time      DATETIME     NOT NULL,
    error_type    VARCHAR(100),
    error_message VARCHAR(1000)
) ENGINE=InnoDB;

-- ---------------------------------------------------------
-- Optional: create a dedicated application user
-- ---------------------------------------------------------
-- CREATE USER IF NOT EXISTS 'tsp_user'@'localhost' IDENTIFIED BY 'tsp_pass_2024';
-- GRANT ALL PRIVILEGES ON tsp_db.* TO 'tsp_user'@'localhost';
-- FLUSH PRIVILEGES;

SELECT 'Database setup complete.' AS Status;
