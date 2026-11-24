#!/bin/sh
# Creates one commit per task, as the assignment requires ("at least one commit for each task").
# Run it ONCE inside the cloned repository, then:  git remote add origin <your-repo-url>; git push -u origin main
# Better still: make these commits on different days while you work and push after each one.
cd "$(dirname "$0")"
[ -d .git ] || git init -b main
git add .gitignore lib compile.sh run_all.sh tools && git commit -m "Project setup: Derby driver, build and run scripts"
git add Task01 && git commit -m "Task01 (B1): PassStore add/find/list, HashMap alternative, line-count and search benchmarks"
git add Task02 && git commit -m "Task02 (B2): SQL-injection login demo and damaged-data tests"
git add Task03 && git commit -m "Task03 (B3): PassLoader Statement/PreparedStatement/batch, insertion benchmark, schema.sql"
git add Task04 && git commit -m "Task04 (B4): migration program, concurrency demo, 800k scale benchmark"
git add README.md report_assets 2>/dev/null; git add -A && git commit -m "README with results, charts and report assets"
