CREATE DATABASE IF NOT EXISTS csa09_assignment5 CHARACTER SET utf8mb4;
USE csa09_assignment5;

CREATE TABLE IF NOT EXISTS bus_pass (
    pass_id VARCHAR(24) PRIMARY KEY,
    reg_no VARCHAR(24) NOT NULL,
    name VARCHAR(120) NOT NULL,
    route_no INT NOT NULL CHECK (route_no > 0),
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS campus_bus_pass (
    pass_id VARCHAR(24) PRIMARY KEY,
    reg_no VARCHAR(24) NOT NULL,
    name VARCHAR(120) NOT NULL,
    route_no INT NOT NULL CHECK (route_no > 0),
    amount DECIMAL(10, 2) NOT NULL CHECK (amount >= 0),
    boarding_stop VARCHAR(120) NOT NULL,
    valid_to DATE NOT NULL,
    INDEX route_idx (route_no)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS assignment5_users (
    id VARCHAR(40) PRIMARY KEY,
    pwd VARCHAR(100) COLLATE utf8mb4_bin NOT NULL
) ENGINE=InnoDB;

INSERT IGNORE INTO assignment5_users VALUES ('demo', 'sample-password');
