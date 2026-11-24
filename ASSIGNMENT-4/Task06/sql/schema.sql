-- Campus Event Registration - MySQL 8.0 script (CSA09 Assignment 4, Task 6)
-- Run:  mysql -u root -p < sql/schema.sql
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4;
USE campus_events;

DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS events;

CREATE TABLE events (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(80) NOT NULL,
    event_date  DATE        NOT NULL
);

CREATE TABLE registrations (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    student_id    CHAR(9)      NOT NULL,
    student_name  VARCHAR(50)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    event_id      INT          NOT NULL,
    registered_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT uq_student_event UNIQUE (student_id, event_id),   -- duplicate-registration guard
    INDEX idx_event (event_id)                                    -- speeds up the event filter
);

INSERT INTO events(name, event_date) VALUES
 ('Hackathon 2026','2026-12-05'), ('Robotics Workshop','2026-12-12'),
 ('Cultural Night','2026-12-20'), ('Guest Lecture: AI','2026-12-28'), ('Sports Meet','2027-01-10');

-- ---- sample data: 3 normal rows ----
INSERT INTO registrations(student_id, student_name, email, event_id) VALUES
 ('192525001','Ravi Kumar','ravi@campus.edu',1),
 ('192525002','Meena Iyer','meena@campus.edu',2),
 ('192525003','Arjun Das','arjun@campus.edu',1);

-- ---- tests to demonstrate in the report ----
-- Duplicate (must fail with error 1062 Duplicate entry):
--   INSERT INTO registrations(student_id, student_name, email, event_id) VALUES ('192525001','Ravi Kumar','ravi@campus.edu',1);
-- Same student, different event (must succeed):
--   INSERT INTO registrations(student_id, student_name, email, event_id) VALUES ('192525001','Ravi Kumar','ravi@campus.edu',3);

-- ---- OPTIONAL: bulk load 100,000 rows for the paging comparison ----
-- SET SESSION cte_max_recursion_depth = 100000;
-- INSERT INTO registrations(student_id, student_name, email, event_id)
-- WITH RECURSIVE seq AS (SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < 100000)
-- SELECT CAST(192000000 + n AS CHAR), CONCAT('Student ', n), CONCAT('s', n, '@campus.edu'), 1 + (n % 5) FROM seq;

-- ---- queries used by JdbcRegistrationDao ----
-- Page 3 (rows 101-150) of all registrations:
--   SELECT ... FROM registrations r JOIN events e ON e.id = r.event_id ORDER BY r.id LIMIT 50 OFFSET 100;
-- Filter by event with paging:
--   SELECT ... WHERE r.event_id = 1 ORDER BY r.id LIMIT 50 OFFSET 0;
