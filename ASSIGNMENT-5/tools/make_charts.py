#!/usr/bin/env python3
"""Charts built from the CSV files written by the benchmark programs."""
import csv, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

OUT = "report_assets"
os.makedirs(OUT, exist_ok=True)
C = ["#1f4e9c", "#d9822b", "#2e8b57", "#8e44ad", "#c0392b"]

# --- line count
rows = list(csv.DictReader(open("Task01/output/linecount_results.csv")))
methods = ["unbuffered FileInputStream", "BufferedInputStream(8KB)", "BufferedReader.readLine", "Files.readAllLines", "byte[] 8 KB", "byte[] 64 KB"]
fig, ax = plt.subplots(figsize=(7.2, 4))
for i, m in enumerate(methods):
    pts = [(int(r["sizeMB"]), float(r["timeMs"])) for r in rows if r["method"] == m]
    ax.plot([p[0] for p in pts], [p[1] for p in pts], marker="o", label=m, color=C[i % len(C)] if i < 5 else "#555")
ax.set_xscale("log"); ax.set_yscale("log")
ax.set_xticks([1, 10, 50]); ax.set_xticklabels(["1 MB", "10 MB", "50 MB"])
ax.set_ylabel("time (ms, log scale)"); ax.set_title("Line counting: time vs file size")
ax.grid(True, which="both", alpha=.3); ax.legend(fontsize=8)
fig.tight_layout(); fig.savefig(f"{OUT}/chart_linecount.png", dpi=170); plt.close(fig)

# --- search
rows = list(csv.DictReader(open("Task01/output/search_results.csv")))
n = [int(r["n"]) for r in rows]
fig, ax = plt.subplots(1, 2, figsize=(9, 3.8))
ax[0].plot(n, [float(r["scanAvgMs"]) for r in rows], marker="o", color=C[0], label="scan, mean per search")
ax[0].plot(n, [float(r["scanWorstMs"]) for r in rows], marker="s", color=C[1], label="scan, worst (last record)")
ax[0].set_xlabel("records in file"); ax[0].set_ylabel("ms per search"); ax[0].set_title("One search = O(n)")
ax[0].grid(alpha=.3); ax[0].legend(fontsize=8)
q = 100
ax[1].plot(n, [float(r["scanQTotalMs"]) if "scanQTotalMs" in r else float(r["scanAvgMs"]) * q for r in rows], marker="o", color=C[0], label="100 scans  O(q·n)")
ax[1].plot(n, [float(r["mapLoadMs"]) + float(r["mapQueryTotalMs"]) for r in rows], marker="o", color=C[2], label="HashMap load + 100 lookups  O(n+q)")
ax[1].set_xlabel("records in file"); ax[1].set_ylabel("total ms"); ax[1].set_title("100 searches: scan vs HashMap")
ax[1].grid(alpha=.3); ax[1].legend(fontsize=8)
fig.tight_layout(); fig.savefig(f"{OUT}/chart_search.png", dpi=170); plt.close(fig)

# --- insert
rows = list(csv.DictReader(open("Task03/output/insert_results.csv")))
methods = []
for r in rows:
    if r["method"] not in methods: methods.append(r["method"])
fig, ax = plt.subplots(figsize=(7.2, 4))
for i, m in enumerate(methods):
    pts = [(int(r["rows"]), float(r["timeMs"])) for r in rows if r["method"] == m]
    ax.plot([p[0] for p in pts], [p[1] for p in pts], marker="o", label=m, color=C[i % len(C)])
ax.set_xscale("log"); ax.set_yscale("log"); ax.set_xlabel("rows inserted"); ax.set_ylabel("time (ms, log scale)")
ax.set_title("JDBC insertion time (Apache Derby embedded)"); ax.grid(True, which="both", alpha=.3); ax.legend(fontsize=8)
fig.tight_layout(); fig.savefig(f"{OUT}/chart_insert.png", dpi=170); plt.close(fig)

# --- scale: lookup per search
fig, ax = plt.subplots(figsize=(7.2, 3.6))
labels = ["Text file\nscan", "Text file\nHashMap", "Database\nPK lookup"]
vals = [228.3, 0.32 / 200, 0.119]
b = ax.bar(labels, vals, color=[C[3], C[2], C[0]])
ax.set_yscale("log"); ax.set_ylabel("ms per search (log scale)")
ax.set_title("800,000 passes: cost of ONE search")
for rect, v in zip(b, vals):
    ax.text(rect.get_x() + rect.get_width() / 2, v * 1.15, f"{v:.4g} ms", ha="center", fontsize=9)
ax.grid(axis="y", alpha=.3)
fig.tight_layout(); fig.savefig(f"{OUT}/chart_scale.png", dpi=170); plt.close(fig)
print("charts ok")
