-- Task B3 / B4 : bus_pass table (works unchanged on MySQL 8.0.16+ and Apache Derby)
-- MySQL usage:
--   CREATE DATABASE busdb;  USE busdb;  SOURCE schema.sql;
DROP TABLE IF EXISTS bus_pass;   -- (MySQL syntax; the Java helper Db.recreateBusPass() does this portably)

CREATE TABLE bus_pass (
    pass_id        VARCHAR(10)   NOT NULL PRIMARY KEY,
    reg_no         VARCHAR(15)   NOT NULL,
    name           VARCHAR(60)   NOT NULL,
    route_no       INT           NOT NULL,
    boarding_stop  VARCHAR(60)   NOT NULL,
    validity_date  DATE          NOT NULL,
    amount         DECIMAL(8,2)  NOT NULL CHECK (amount >= 0)
);

-- Task B2 : users table for the login / SQL-injection experiment (local TEST database only)
DROP TABLE IF EXISTS users;
CREATE TABLE users (
    username  VARCHAR(30) NOT NULL PRIMARY KEY,
    password  VARCHAR(60) NOT NULL
);
INSERT INTO users VALUES ('admin',   'Admin@123');
INSERT INTO users VALUES ('student', 'pass123');
