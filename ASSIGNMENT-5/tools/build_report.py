#!/usr/bin/env python3
"""Builds Assignment5_CO5_Report.docx (Part A design + Part B evidence). Numbers come from Task*/output/*.csv."""
import csv, glob, os, sys
from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor, Cm

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
os.chdir(ROOT)
OUT = sys.argv[1] if len(sys.argv) > 1 else "Assignment5_CO5_Report.docx"
BLUE = RGBColor(0x1F, 0x4E, 0x9C)

doc = Document()
sec = doc.sections[0]
sec.page_width, sec.page_height = Cm(21.0), Cm(29.7)
sec.left_margin = sec.right_margin = Cm(2.2)
sec.top_margin = sec.bottom_margin = Cm(2.0)
TEXT_W = 21.0 - 4.4  # cm

st = doc.styles
st["Normal"].font.name = "Calibri"
st["Normal"].font.size = Pt(11)
st["Normal"].element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")
for name, size in (("Heading 1", 18), ("Heading 2", 14), ("Heading 3", 12)):
    s = st[name]
    s.font.name = "Calibri"; s.font.size = Pt(size); s.font.bold = True; s.font.color.rgb = BLUE
    s.element.rPr.rFonts.set(qn("w:eastAsia"), "Calibri")
    s.paragraph_format.space_before = Pt(14 if name != "Heading 3" else 10)
    s.paragraph_format.space_after = Pt(6)
    s.paragraph_format.keep_with_next = True

shot_no = [0]
tab_no = [0]


def shade(cell, hex_fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear"); shd.set(qn("w:color"), "auto"); shd.set(qn("w:fill"), hex_fill)
    tcPr.append(shd)


def para(text="", bold=False, italic=False, size=None, align=None, color=None, after=6, style=None):
    p = doc.add_paragraph(style=style)
    if text:
        add_runs(p, text, bold, italic, size, color)
    p.paragraph_format.space_after = Pt(after)
    if align: p.alignment = align
    return p


def add_runs(p, text, bold=False, italic=False, size=None, color=None):
    """supports **bold** inline markup"""
    parts = text.split("**")
    for i, part in enumerate(parts):
        if not part: continue
        r = p.add_run(part)
        r.bold = bold or (i % 2 == 1)
        r.italic = italic
        if size: r.font.size = Pt(size)
        if color: r.font.color.rgb = color


def bullets(items, style="List Bullet"):
    for it in items:
        p = doc.add_paragraph(style=style)
        add_runs(p, it)
        p.paragraph_format.space_after = Pt(2)


def h1(t): doc.add_heading(t, 1)
def h2(t): doc.add_heading(t, 2)
def h3(t): doc.add_heading(t, 3)


def table(rows, widths=None, header=True, size=9.5, caption=None, align_right_cols=()):
    if caption:
        tab_no[0] += 1
        cp = para(f"Table {tab_no[0]}. {caption}", italic=True, size=9.5, after=3)
        cp.paragraph_format.keep_with_next = True
    t = doc.add_table(rows=len(rows), cols=len(rows[0]))
    t.style = "Table Grid"
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            c = t.cell(ri, ci)
            c.text = ""
            p = c.paragraphs[0]
            add_runs(p, str(val), bold=(header and ri == 0), size=size)
            p.paragraph_format.space_after = Pt(1)
            if ci in align_right_cols and ri > 0: p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
            if header and ri == 0: shade(c, "D9E2F3")
    if widths:
        for row in t.rows:
            for ci, w in enumerate(widths):
                row.cells[ci].width = Cm(w)
    doc.add_paragraph().paragraph_format.space_after = Pt(4)
    return t


def code(lines, size=8.5, title=None):
    """boxed monospace block (pseudocode / layouts)"""
    if title:
        p = para(title, bold=True, size=10, after=2)
        p.paragraph_format.keep_with_next = True
    t = doc.add_table(rows=1, cols=1)
    t.style = "Table Grid"
    c = t.cell(0, 0)
    shade(c, "F4F6F8")
    c.text = ""
    first = True
    for ln in lines:
        p = c.paragraphs[0] if first else c.add_paragraph()
        first = False
        r = p.add_run(ln if ln else " ")
        r.font.name = "Courier New"; r.font.size = Pt(size)
        r._element.rPr.rFonts.set(qn("w:eastAsia"), "Courier New")
        p.paragraph_format.space_after = Pt(0)
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.line_spacing = 1.0
    doc.add_paragraph().paragraph_format.space_after = Pt(4)


def picture(path, caption, width_cm=None):
    shot_no[0] += 1
    width_cm = width_cm or TEXT_W
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(path, width=Cm(width_cm))
    cp = para(f"Output screenshot {shot_no[0]}. {caption}", italic=True, size=9.5, align=WD_ALIGN_PARAGRAPH.CENTER, after=10)


def figure(path, caption, width_cm=None):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.keep_with_next = True
    p.add_run().add_picture(path, width=Cm(width_cm or TEXT_W * 0.92))
    para(caption, italic=True, size=9.5, align=WD_ALIGN_PARAGRAPH.CENTER, after=10)


def pagebreak():
    doc.add_paragraph().add_run().add_break(WD_BREAK.PAGE)


def read_csv(path):
    return list(csv.DictReader(open(path, encoding="utf-8")))


# ---------------------------------------------------------------- data
lc = read_csv("Task01/output/linecount_results.csv")
sr = read_csv("Task01/output/search_results.csv")
ins = read_csv("Task03/output/insert_results.csv")


def lcv(mb, method, key="timeMs"):
    for r in lc:
        if int(r["sizeMB"]) == mb and r["method"] == method:
            return float(r[key])
    raise KeyError((mb, method))


def f1(x): return f"{x:,.1f}"
def f0(x): return f"{x:,.0f}"


# ================================================================= COVER
for _ in range(3): para("")
para("ASSIGNMENT 5 – CO5", bold=True, size=30, color=BLUE, align=WD_ALIGN_PARAGRAPH.CENTER, after=4)
para("DATA PERSISTENCE ENGINEERING", bold=True, size=20, color=RGBColor(0x2E, 0x8B, 0x8B), align=WD_ALIGN_PARAGRAPH.CENTER, after=2)
para("Files and Databases", size=16, align=WD_ALIGN_PARAGRAPH.CENTER, after=26)
para("Programming in Java (CSA09)", size=14, align=WD_ALIGN_PARAGRAPH.CENTER, after=2)
para("Individual Assignment  |  Total: 50 Marks  |  Part A (25) + Part B (25)", size=11, align=WD_ALIGN_PARAGRAPH.CENTER, after=30)
table([
    ["Student name", "<YOUR NAME>"],
    ["Register number", "<YOUR REGISTER NUMBER>"],
    ["Course / Section", "Programming in Java (CSA09)"],
    ["GitHub repository", "https://github.com/<your-username>/CSA09-<RegisterNumber>-Assignment5"],
    ["Java version", "OpenJDK 21.0.13 (Java 17+ required: uses records)"],
    ["Database used", "Apache Derby 10.16.1.1 embedded (portable SQL; MySQL switch documented in README)"],
], widths=[4.5, 12.1], header=False, size=11)
para("")
para("Workflow followed:  UNDERSTAND → DESIGN → PROTECT → IMPLEMENT → MEASURE → CHALLENGE → INTERPRET",
     italic=True, size=10, align=WD_ALIGN_PARAGRAPH.CENTER)
pagebreak()

# ================================================================= CONTENTS
h1("Contents")
for line in [
    "0.  Scope, environment and how to read this report",
    "PART A – Persistence Design & Pseudocode",
    "     A1  Design a file format (bus-pass records)",
    "     A2  Buffered I/O",
    "     A3  Data-access design pattern (generic DAO)",
    "     A4  Serialization design",
    "     A5  Correctness and safety (transactions, SQL injection)",
    "PART B – Implementation & Experimental Investigation",
    "     B1  File store (+ alternative design: scan vs HashMap)",
    "     B2  Adversarial input",
    "     B3  JDBC performance comparison, transactions and alternatives",
    "     B4  Final engineering decision",
    "Conclusion, repository map and limitations",
]:
    para(line, after=1, size=11, bold=line.startswith("PART"))
pagebreak()

# ================================================================= 0
h1("0. Scope, environment and how to read this report")
para("This report answers **Part A** (design, record layouts and pseudocode) and **Part B** (working Java implementation, adversarial tests, measurements and the final engineering decision) of Assignment 5 – CO5. The complete source code, input files, SQL script, raw program output, CSV measurement files and screenshots are in the GitHub repository whose link is on the cover page. Every number quoted in Part B was produced by the programs in the repository, not estimated.")
table([
    ["Item", "Value"],
    ["Operating environment", "Linux container, 2 vCPU, 8 GB RAM, OpenJDK 21.0.13"],
    ["Database", "Apache Derby 10.16.1.1 (embedded JDBC driver 10.16.1.1, JDBC 4.2)"],
    ["Why not MySQL here", "No MySQL server was available in the build environment. All SQL is portable (see Task03/schema.sql) and Db.java switches to MySQL with -Ddb.url=jdbc:mysql://…. Relative conclusions are unchanged; absolute times will differ on MySQL (network round trips)."],
    ["Pseudocode style", "ALGORITHM / INPUT / OUTPUT / BEGIN … END, numbered steps, upper-case control keywords (IF … THEN … ELSE … END IF, FOR EACH … END FOR, WHILE … END WHILE, TRY … CATCH … FINALLY). The Standard for Algorithmic Pseudocode is applied consistently; adapt the heading block if your faculty template differs."],
    ["Repository map", "Task01 = B1, Task02 = B2, Task03 = B3, Task04 = B4 (Part A has no code; it lives in this report)."],
], widths=[4.2, 12.4], caption="Environment and conventions")

# ================================================================= PART A
pagebreak()
h1("PART A – Persistence Design & Pseudocode")

# ---------------------------------------------------------------- A1
h2("A1. Design a File Format")
para("**Scenario.** The transport office stores about 8,000 bus-pass records a year. Each record has: pass ID, register number, name, route number, boarding stop, validity date and amount paid.")
h3("A1.1 Record layout – delimited text file")
para("One record per line, UTF-8, fields separated by a comma, line terminated by LF. The line is the unit of damage: a bad line can be skipped without losing its neighbours.")
table([
    ["#", "Field", "Java type", "Text form / rule", "Example"],
    ["1", "passId", "String", "'P' + digits, unique", "P1003"],
    ["2", "regNo", "String", "≤ 15 characters", "22CS103"],
    ["3", "name", "String", "≤ 60 chars, must not contain ','", "Meena S"],
    ["4", "route", "int", "positive integer", "15"],
    ["5", "stop", "String", "≤ 60 chars, must not contain ','", "Station Road"],
    ["6", "validity", "LocalDate", "ISO yyyy-MM-dd", "2027-03-31"],
    ["7", "amount", "BigDecimal", "decimal, ≥ 0, 2 places", "1800.00"],
], widths=[0.8, 2.4, 2.5, 6.4, 4.5], caption="Text record layout (exactly 7 fields)")
code(["P1003,22CS103,Meena S,15,Station Road,2027-03-31,1800.00<LF>",
      "|     |       |       |  |            |          |",
      "f1    f2      f3      f4 f5           f6         f7"],
     title="Example line")
para("A line is **valid** only if (a) it splits into exactly 7 fields, (b) the ID matches P+digits, (c) route is an integer, (d) validity parses as a date and (e) amount is a non-negative decimal. The implemented size is ≈ 63 bytes per record, so one year (8,000 records) ≈ 0.5 MB.")

h3("A1.2 Record layout – binary file with DataOutputStream")
para("A small file header followed by length-prefixed, checksummed records. The length prefix lets a reader skip a record without understanding it; the CRC-32 detects damage inside a record.")
code([
    "FILE HEADER (once)                           bytes",
    "  magic        int    0x42505331 ('BPS1')       4",
    "  version      short  1                         2",
    "",
    "RECORD (repeated, appended)",
    "  payloadLen   int    length of payload         4",
    "  ---- payload (payloadLen bytes) ---------------",
    "  passId       writeUTF  (2-byte length + UTF-8) 2+n",
    "  regNo        writeUTF                          2+n",
    "  name         writeUTF                          2+n",
    "  route        writeInt                          4",
    "  stop         writeUTF                          2+n",
    "  validity     writeInt  LocalDate.toEpochDay()  4",
    "  amountPaise  writeLong amount x 100            8",
    "  ----------------------------------------------",
    "  crc32        int    CRC-32 of the payload      4",
], title="Binary layout")
para("Typical record ≈ 4 + (2+5) + (2+7) + (2+7) + 4 + (2+12) + 4 + 8 + 4 ≈ 63 bytes – the same order as text, but fields are not human-readable and a one-byte error can desynchronise a reader unless the length prefix and CRC are checked. Money is stored as integer paise to avoid floating-point rounding.")

h3("A1.3 Pseudocode – add, find and list")
code([
    "ALGORITHM AddPass(file, pass)",
    "INPUT : file  - path of the pass file; pass - a validated BusPass",
    "OUTPUT: pass appended to the end of file",
    "BEGIN",
    "  1. IF pass.name OR pass.stop CONTAINS ',' THEN REJECT 'delimiter in data'",
    "  2. line <- JOIN(',', passId, regNo, name, route, stop, validity, amount)",
    "  3. needNewline <- FALSE",
    "  4. IF file EXISTS AND SIZE(file) > 0 THEN",
    "  5.     needNewline <- (LAST BYTE of file != LF)        // torn write from a power cut",
    "  6. END IF",
    "  7. OPEN file IN APPEND MODE AS writer",
    "  8. IF needNewline THEN WRITE LF TO writer              // isolate the torn line",
    "  9. WRITE line + LF TO writer                           // one write call per record",
    " 10. FLUSH writer; FORCE data to disk (fsync); CLOSE writer",
    "END",
], title="Pseudocode 1 – AddPass")
code([
    "ALGORITHM FindPass(file, id)",
    "INPUT : file - path; id - the pass ID to find",
    "OUTPUT: the matching BusPass, or NULL",
    "BEGIN",
    "  1. IF file DOES NOT EXIST THEN WARN 'missing file'; RETURN NULL",
    "  2. OPEN file AS reader (buffered)",
    "  3. WHILE (line <- reader.READLINE) IS NOT END-OF-FILE DO",
    "  4.     fields <- SPLIT(line, ',')",
    "  5.     IF COUNT(fields) != 7 OR NOT VALID(fields) THEN",
    "  6.         damaged <- damaged + 1; CONTINUE             // skip, never abort",
    "  7.     END IF",
    "  8.     IF fields[1] = id THEN CLOSE reader; RETURN BusPass(fields)",
    "  9. END WHILE",
    " 10. CLOSE reader; RETURN NULL",
    "END",
], title="Pseudocode 2 – FindPass")
code([
    "ALGORITHM ListPassesOnRoute(file, route)",
    "INPUT : file - path; route - route number",
    "OUTPUT: list of valid passes whose route field equals route",
    "BEGIN",
    "  1. result <- EMPTY LIST",
    "  2. IF file DOES NOT EXIST THEN RETURN result",
    "  3. OPEN file AS reader",
    "  4. WHILE (line <- reader.READLINE) IS NOT END-OF-FILE DO",
    "  5.     IF line IS DAMAGED THEN damaged <- damaged + 1; CONTINUE",
    "  6.     p <- PARSE(line)",
    "  7.     IF p.route = route THEN APPEND p TO result",
    "  8. END WHILE",
    "  9. CLOSE reader; RETURN result",
    "END",
], title="Pseudocode 3 – ListPassesOnRoute")

h3("A1.4 Detecting and handling damage")
table([
    ["Failure", "How it is detected", "How it is handled"],
    ["**Damaged line** (wrong field count, non-numeric amount, empty line, bad date)",
     "Validate every line: field count = 7, ID pattern, route int, ISO date, amount decimal ≥ 0. Binary variant: CRC-32 mismatch or impossible payloadLen.",
     "Skip the line, increment a damaged-line counter, log the line number and reason, continue with the next line. Never abort the whole scan. Report the counter to the operator."],
    ["**Missing file**",
     "Files.exists() before reading, or catch NoSuchFileException.",
     "Read operations: warn and treat as an empty store (return null / empty list). First add(): create the file. Distinguish 'missing' from other IOExceptions (permissions, disk) which must be reported, not hidden."],
    ["**Incomplete write after power failure**",
     "Last byte of the file is not LF, or the last line has fewer than 7 fields (binary: payloadLen runs past end-of-file or CRC fails).",
     "Prevent: write each record with one write call, flush and fsync. Repair: before appending, test the last byte and write a LF so the torn line is isolated; the validation step then drops it. Rewrites (update/delete) go to a temporary file followed by an atomic move, so the original is never half-written. Keep periodic backups."],
], widths=[3.6, 6.0, 7.0], caption="Damage detection and handling")

h3("A1.5 Text file versus fixed-length records in RandomAccessFile")
para("A fixed-length layout makes every record exactly R bytes (for example 96 bytes: passId 10, regNo 15, name 30, route 2, stop 20, validity 4, amount 4, status 1, padding 10). The record with index i starts at byte i × R, so it can be read or overwritten with seek().")
table([
    ["Criterion", "Delimited text file", "Fixed-length RandomAccessFile"],
    ["Find by ID", "O(n) scan (measured 27 ms avg at 100,000 records)", "O(1) if the ID encodes the index; O(log n) binary search if sorted by ID; O(n) otherwise"],
    ["Update / delete in place", "Hard – line lengths differ, whole file rewritten", "Easy – seek and overwrite one record; delete = status byte"],
    ["Readability / debugging", "Excellent – open in any editor, easy to diff", "Poor – needs a tool; padding wastes space"],
    ["Variable-length data", "Natural", "Truncation or wasted space (names padded to 30)"],
    ["Damage isolation", "One bad line affects only itself", "One shifted byte misaligns every later record unless length is checked"],
    ["Size for 8,000 records", "≈ 0.5 MB", "8,000 × 96 B = 0.77 MB"],
    ["Concurrency", "Append is simple; no locking built in", "Needs FileLock for safe in-place updates"],
    ["Best use", "Interchange, import/export, small stores", "Fast keyed access to a stable record structure"],
], widths=[3.6, 6.5, 6.5], caption="Text file vs RandomAccessFile")

# ---------------------------------------------------------------- A2
h2("A2. Buffered I/O")
para("**Scenario.** A program reads a 50 MB attendance log one byte at a time to count lines.")
h3("A2.1 Prediction")
para("50 MB = 50 × 1,048,576 = **52,428,800 bytes**. FileInputStream.read() returns one byte per call, so the loop makes one call per byte plus one final call that returns −1: **52,428,801 read calls**. Each call crosses from Java into native code and then into the operating system (a read system call for a single byte), which costs a few hundred nanoseconds even when the data is already in the OS page cache. The work is therefore dominated by call overhead, not by disk speed. This prediction was checked in Part B (Output screenshot 2): the counter reported exactly 52,428,801 calls.")
h3("A2.2 Redesign with a buffer")
code([
    "ALGORITHM CountLinesBuffered(file)",
    "INPUT : file - path of the attendance log",
    "OUTPUT: number of lines",
    "BEGIN",
    "  1. buffer <- NEW BYTE ARRAY OF SIZE 65536        // 64 KB",
    "  2. lines <- 0",
    "  3. OPEN file AS in",
    "  4. WHILE (n <- in.READ(buffer)) > 0 DO              // one call fills up to 64 KB",
    "  5.     FOR i <- 0 TO n - 1 DO",
    "  6.         IF buffer[i] = LF THEN lines <- lines + 1",
    "  7.     END FOR",
    "  8. END WHILE",
    "  9. CLOSE in; RETURN lines",
    "END",
], title="Pseudocode 4 – buffered line counter")
para("Calls with a 64 KB buffer = ⌈52,428,800 / 65,536⌉ + 1 = 800 + 1 = **801** (measured 801). With Java's default BufferedInputStream (8 KB) = 6,400 + 1 = **6,401** (measured 6,401).")
h3("A2.3 How buffering reduces calls to the operating system")
para("A buffer is a block of memory that is filled by one large read. Later requests for the next byte are satisfied from memory with no system call, until the buffer is empty and the next block is fetched. The number of system calls falls from n (one per byte) to n / B (B = buffer size), and each larger read lets the OS use read-ahead and sequential disk access. The same applies to output: a BufferedWriter groups many small writes into a few large ones.")
h3("A2.4 A situation where a buffer gives little or no benefit")
bullets([
    "**The program already reads in large blocks.** In the measurement, an 8 KB byte[] took 21.6 ms and a 64 KB byte[] 14.6 ms for 50 MB: system calls were already negligible, so a bigger buffer, or wrapping the stream in a BufferedInputStream again (double buffering), adds copying and no speed-up.",
    "**Random access.** Seeking to a different position each time discards the buffer contents; reading 8 KB to use 60 bytes wastes work.",
    "**Slow sources** (network, a terminal): throughput is limited by the source, not by call overhead.",
])
h3("A2.5 Buffered reading versus Files.readAllLines()")
table([
    ["Method (50 MB, 1,638,400 lines)", "Time (ms)", "Heap retained", "Time complexity", "Space complexity"],
    ["BufferedReader.readLine() loop", f1(lcv(50, "BufferedReader.readLine")), "≈ 0 (one line at a time)", "O(n)", "O(L) – longest line"],
    ["Files.lines().count() (streaming)", f1(lcv(50, "Files.lines.count")), "≈ 0", "O(n)", "O(L)"],
    ["Files.readAllLines()", f1(lcv(50, "Files.readAllLines")), f"{lcv(50, 'readAllLines_heapMB'):.1f} MB (2.4× the file size)", "O(n)", "O(n) – every line is a String"],
], widths=[5.6, 2.0, 4.0, 2.4, 3.0], caption="Measured comparison on the 50 MB log (see Part B, Output screenshot 2)", align_right_cols=(1,))
para("**Interpretation.** readAllLines() is the shortest code but holds every line in memory at once: it needed ≈ 120 MB for a 50 MB file and fails with OutOfMemoryError once the file exceeds the heap. For counting, searching or any single pass, line-by-line reading uses constant memory and is ~4× faster here. readAllLines() is appropriate only when the whole content is needed in memory (sorting, random access to lines) and the file is known to be small.")

# ---------------------------------------------------------------- A3
h2("A3. Data-Access Design Pattern")
para("**Scenario.** StudentDB, CourseDB and MarksDB repeat similar JDBC operations.")
h3("A3.1 Common steps and the parts that differ")
table([
    ["Common in all three classes (move to a base class)", "Different in each class (supplied by the subclass)"],
    ["1. Obtain a Connection\n2. Build the SQL text\n3. Create a PreparedStatement\n4. Bind the parameters\n5. execute / executeQuery / executeUpdate\n6. Loop over the ResultSet\n7. Close ResultSet, Statement, Connection (try-with-resources)\n8. Convert SQLException into a meaningful failure",
     "• table name and column list\n• the SQL strings\n• how an entity's fields are bound to the ? placeholders\n• how a ResultSet row is mapped to an object (Student / Course / Marks)\n• the primary-key type: String regNo (student), String courseCode (course), composite (regNo, courseCode) for marks"],
], widths=[8.3, 8.3], caption="Common pattern analysis")
h3("A3.2 Generic Dao<T, K> interface")
code([
    "INTERFACE Dao<T, K>                        // T = entity type, K = primary-key type",
    "  insert  (T entity)     -> void",
    "  findById(K id)         -> Optional<T>     // empty when the key does not exist",
    "  findAll ()             -> List<T>",
    "  update  (T entity)     -> boolean         // true when exactly one row changed",
    "  delete  (K id)         -> boolean",
    "END INTERFACE",
], title="Design 1 – the interface")
code([
    "ABSTRACT CLASS JdbcDao<T, K> IMPLEMENTS Dao<T, K>     // Template Method pattern",
    "  ABSTRACT  insertSql(), selectByIdSql(), selectAllSql(), updateSql(), deleteSql()",
    "  ABSTRACT  bindInsert(ps, T), bindUpdate(ps, T), bindKey(ps, K), map(resultSet) -> T",
    "",
    "  METHOD insert(entity)",
    "     TRY (con <- dataSource.GETCONNECTION; ps <- con.PREPARE(insertSql()))",
    "         bindInsert(ps, entity); ps.EXECUTEUPDATE()",
    "     CATCH SQLException e  -> THROW DataAccessException(e)",
    "  METHOD findById(id)",
    "     TRY (con; ps <- con.PREPARE(selectByIdSql()))",
    "         bindKey(ps, id); rs <- ps.EXECUTEQUERY()",
    "         IF rs.NEXT THEN RETURN Optional.OF(map(rs)) ELSE RETURN Optional.EMPTY",
    "  METHOD findAll()  -> LIST of map(rs) for every row",
    "  METHOD update(e)  -> bindUpdate(ps, e); RETURN ps.EXECUTEUPDATE() = 1",
    "  METHOD delete(id) -> bindKey(ps, id);  RETURN ps.EXECUTEUPDATE() = 1",
    "END CLASS",
], title="Design 2 – shared JDBC steps written once")
code([
    "CLASS StudentDao EXTENDS JdbcDao<Student, String>     // K = String (regNo)",
    "  insertSql()     = 'INSERT INTO student(reg_no,name,department,year_of_study,email,cgpa) VALUES (?,?,?,?,?,?)'",
    "  selectByIdSql() = 'SELECT * FROM student WHERE reg_no = ?'",
    "  selectAllSql()  = 'SELECT * FROM student ORDER BY reg_no'",
    "  updateSql()     = 'UPDATE student SET name=?, department=?, year_of_study=?, email=?, cgpa=? WHERE reg_no=?'",
    "  deleteSql()     = 'DELETE FROM student WHERE reg_no = ?'",
    "  bindInsert(ps,s): ps.SET(1,s.regNo) SET(2,s.name) SET(3,s.department) SET(4,s.year) SET(5,s.email) SET(6,s.cgpa)",
    "  bindKey(ps,k)   : ps.SET(1,k)",
    "  map(rs)         : RETURN NEW Student(rs.regNo, rs.name, rs.department, rs.year, rs.email, rs.cgpa)",
    "END CLASS",
], title="Design 3 – StudentDao implements the generic interface")
h3("A3.3 Object-to-table mapping (Student)")
para("The assignment does not list the Student fields; the commonly used set below is assumed. Replace it with your own class if different.", italic=True, size=10)
table([
    ["Student field (Java)", "Column", "SQL type", "Constraint"],
    ["regNo : String", "reg_no", "VARCHAR(15)", "**PRIMARY KEY** (unique, never changes)"],
    ["name : String", "name", "VARCHAR(60)", "NOT NULL"],
    ["department : String", "department", "VARCHAR(30)", "NOT NULL"],
    ["year : int", "year_of_study", "INT", "CHECK (year_of_study BETWEEN 1 AND 5)"],
    ["email : String", "email", "VARCHAR(80)", "UNIQUE"],
    ["cgpa : double", "cgpa", "DECIMAL(3,2)", "CHECK (cgpa BETWEEN 0 AND 10)"],
], widths=[4.5, 3.2, 3.2, 5.7], caption="Student object → student table")
h3("A3.4 Generic DAO versus SQL inside service or GUI classes")
table([
    ["Aspect", "Generic DAO", "SQL written in service / GUI classes"],
    ["Duplication", "JDBC boilerplate written once; each entity adds only SQL + mapping", "Same boilerplate pasted in every screen; fixes must be repeated"],
    ["Change of database or schema", "One class changes", "Every GUI / service class that contains SQL changes"],
    ["Testing", "Business logic tested against a fake Dao; DAO tested alone", "Cannot test logic without a database and the GUI"],
    ["Security", "All queries are PreparedStatement in one reviewable place", "Easy for one screen to concatenate input into SQL"],
    ["Transactions", "Service decides the boundary; DAO takes a shared Connection", "Mixed with UI code; easy to forget commit/rollback"],
    ["Cost", "More classes up front; slight indirection", "Fastest for a 100-line prototype"],
], widths=[3.4, 6.6, 6.6], caption="Generic DAO vs inline SQL")

# ---------------------------------------------------------------- A4
h2("A4. Serialization Design")
para("**Scenario.** A Hostel contains Room objects; each Room contains Resident objects. A Resident has a name, register number, password and a live database Connection reference.")
h3("A4.1 Which fields must be transient")
table([
    ["Resident field", "Decision", "Reason"],
    ["name : String", "serialize", "Ordinary data, needed after reload."],
    ["registerNumber : String", "serialize", "Identity of the resident."],
    ["password : String", "**transient**", "A secret must not be written to a file that can be copied or read; after reload the user logs in again. If a credential must persist, store a salted hash in the database, not the clear text in an object file."],
    ["connection : Connection", "**transient**", "A live Connection wraps an operating-system socket/session and is **not Serializable**: leaving it non-transient makes writeObject() throw NotSerializableException half-way through, leaving a corrupt file. Even if it could be written, a restored connection would be dead. Re-open it lazily after reading."],
], widths=[3.8, 2.4, 10.4], caption="Transient-field decisions")
para("Hostel, Room and Resident must implement java.io.Serializable (collections such as ArrayList already do). After deserialization, transient fields hold default values (null / 0), so code must handle a null password and a null connection.")
h3("A4.2 What happens to the complete object graph when Hostel is serialized")
para("Serialization follows references. writeObject(hostel) writes the Hostel's non-transient fields; for the List<Room> it writes the list and then every Room, and for each Room its List<Resident> and every Resident, recursively. The result is the **whole reachable graph**, not only the Hostel object. Key properties:")
bullets([
    "**Each object is written once.** A handle table records objects already written; a second reference to the same Room or Resident is written as a back-reference, so sharing and cycles (Room → Hostel back-pointer) are preserved on read.",
    "**Every object in the graph must be Serializable**, otherwise NotSerializableException is thrown and the output is incomplete. A non-serializable superclass needs an accessible no-argument constructor.",
    "**static and transient fields are skipped.**",
    "**File size grows with the graph:** serializing the Hostel pulls in every Room and Resident, even if the program only needed one.",
    "**Deserialization rebuilds the graph** without calling the constructors of Serializable classes; object identity (the same Room referenced from two places) is restored.",
])
h3("A4.3 Adding a phone-number field after old files were saved")
table([
    ["Situation", "What happens", "Result"],
    ["Class has **no explicit** serialVersionUID", "The JVM computes a UID from the class structure. Adding a field changes the structure, so the computed UID changes.", "Reading an old file throws **InvalidClassException** (local class incompatible: stream classdesc serialVersionUID = … , local class serialVersionUID = …). Old files become unreadable."],
    ["Class declares **private static final long serialVersionUID = 1L**", "Adding a field is a *compatible* change. The stream has no value for phone.", "Old files load; phone = null (default). Code must treat null as 'not provided'."],
    ["Incompatible change (remove a field, change a field's type, change the class hierarchy)", "Stream and class no longer match even with the same UID.", "Increase the UID, or add readObject()/writeObject() (or a migration program) to convert old files."],
], widths=[4.4, 6.2, 6.0], caption="Effect of adding a field")
h3("A4.4 Role of serialVersionUID and comparison with CSV / JSON")
para("serialVersionUID is the version label written with every serialized class. On reading, Java compares the label in the stream with the one in the loaded class; a mismatch means 'the writer and reader do not agree on the class' and reading is refused. Declaring it explicitly lets the developer decide when versions are compatible instead of leaving it to an automatic hash of the class layout.")
table([
    ["Criterion", "Java serialization", "CSV", "JSON"],
    ["Human readable / editable", "No (binary)", "Yes", "Yes"],
    ["Nested object graphs, shared references", "Yes, automatic", "No – flat rows only", "Nested trees yes; shared references / cycles need extra work"],
    ["Other languages / tools", "Java only", "Everything (Excel, SQL import)", "Everything"],
    ["Versioning", "serialVersionUID; fragile", "Add a column; old readers may break", "Unknown fields can be ignored; most flexible"],
    ["Size / speed (800,000 passes, measured)", "96.6 MB; write 3.6 s, read+filter 1.5 s", "48.3 MB text; scan 228 ms per search", "Typically larger than CSV; parser needed"],
    ["Security", "**Deserializing untrusted data is dangerous** (gadget-chain attacks); use ObjectInputFilter", "Low risk, validate fields", "Low risk, validate schema"],
    ["Best use", "Short-lived caches/sessions between trusted Java programs", "Import/export, reports", "APIs, configuration, data exchange"],
], widths=[3.8, 4.6, 4.0, 4.2], caption="Serialization vs CSV vs JSON")

# ---------------------------------------------------------------- A5
h2("A5. Correctness and Safety")
h3("A5.1 Transaction – transfer ₹500 from a hostel wallet to a canteen wallet")
para("The two updates must succeed or fail **together**. If the debit is committed and a failure happens before the credit, ₹500 disappears; if the credit is committed alone, ₹500 is created.")
code([
    "ALGORITHM TransferMoney(fromWallet, toWallet, amount = 500)",
    "INPUT : fromWallet, toWallet - wallet IDs; amount - rupees",
    "OUTPUT: SUCCESS, INSUFFICIENT_FUNDS or ERROR; total money is unchanged in every case",
    "BEGIN",
    "  1. con <- GETCONNECTION",
    "  2. oldMode <- con.GETAUTOCOMMIT; con.SETAUTOCOMMIT(FALSE)          // begin transaction",
    "  3. TRY",
    "  4.     rows1 <- EXECUTE 'UPDATE wallet SET balance = balance - ? WHERE id = ? AND balance >= ?'",
    "                 WITH (amount, fromWallet, amount)",
    "  5.     IF rows1 != 1 THEN con.ROLLBACK; RETURN INSUFFICIENT_FUNDS",
    "  6.     rows2 <- EXECUTE 'UPDATE wallet SET balance = balance + ? WHERE id = ?'",
    "                 WITH (amount, toWallet)",
    "  7.     IF rows2 != 1 THEN con.ROLLBACK; RETURN ERROR",
    "  8.     EXECUTE 'INSERT INTO wallet_log(from_id,to_id,amount) VALUES (?,?,?)'   // audit trail",
    "  9.     con.COMMIT                                                     // both changes become permanent",
    " 10.     RETURN SUCCESS",
    " 11. CATCH SQLException e",
    " 12.     con.ROLLBACK                                                   // undo the debit if it already ran",
    " 13.     RETURN ERROR",
    " 14. FINALLY",
    " 15.     con.SETAUTOCOMMIT(oldMode); CLOSE con",
    " 16. END TRY",
    "END",
], title="Pseudocode 5 – TransferMoney with commit and rollback")
h3("A5.2 The unsafe login query and how crafted input bypasses it")
code([
    "sql <- \"SELECT * FROM users WHERE username = '\" + user + \"' AND password = '\" + pass + \"'\"",
    "rs  <- statement.EXECUTEQUERY(sql)",
    "IF rs.NEXT THEN LOGIN_OK ELSE LOGIN_FAILED",
], title="Unsafe check (user input is joined into the SQL text)")
para("The database receives one string and parses it. A single quote in the input ends the string literal and the rest of the input is **parsed as SQL**:")
table([
    ["Input", "SQL the database actually receives", "Why it succeeds"],
    ["user = admin' --<br>pass = anything".replace("<br>", "  /  "), "… WHERE username = 'admin' --' AND password = 'anything'", "The quote closes the literal; -- starts a comment, so the password test is removed from the query."],
    ["user = ' OR '1'='1<br>pass = ' OR '1'='1".replace("<br>", "  /  "), "… WHERE username = '' OR '1'='1' AND password = '' OR '1'='1'", "'1'='1' is always true; AND binds tighter than OR, so the whole condition is true for every row."],
    ["user = admin<br>pass = x' OR 'a'='a".replace("<br>", "  /  "), "… WHERE username = 'admin' AND password = 'x' OR 'a'='a'", "The appended OR clause makes the condition true regardless of the password."],
], widths=[4.2, 6.4, 6.0], caption="Crafted inputs against the unsafe query")
h3("A5.3 Redesign with PreparedStatement")
code([
    "ALGORITHM SafeLogin(user, pass)",
    "INPUT : user, pass - text typed by the person",
    "OUTPUT: TRUE when exactly the stored credentials match",
    "BEGIN",
    "  1. ps <- con.PREPARE('SELECT username FROM users WHERE username = ? AND password = ?')",
    "  2. ps.SETSTRING(1, user)                 // value is DATA, never SQL",
    "  3. ps.SETSTRING(2, pass)",
    "  4. rs <- ps.EXECUTEQUERY",
    "  5. RETURN rs.NEXT",
    "END",
], title="Pseudocode 6 – SafeLogin")
para("The SQL text is fixed and sent to the database before any input exists; the input is bound afterwards as a value. A quote, a comment marker or the word OR inside the value is compared literally with the stored password and can never change the structure of the query. (In production the password column should hold a salted hash such as bcrypt, the account should have least privilege, and error messages should not reveal SQL.)")
h3("A5.4 One transaction versus two independent auto-commit updates")
table([
    ["Aspect", "One transaction (commit/rollback)", "Two auto-commit updates"],
    ["Atomicity", "Both updates or neither", "Each update is committed immediately and separately"],
    ["Failure between the updates", "Rolled back – balances unchanged", "Debit stays, credit is lost: **₹500 destroyed**"],
    ["What other users can see", "Nothing until commit (isolation)", "Intermediate state is visible: total money momentarily wrong"],
    ["Consistency check", "Balance test + rollback keeps total constant", "Cannot undo the first update when the second fails"],
    ["Cost", "One commit (one log flush)", "Two commits (measured in B3: commits dominate insertion time)"],
    ["Locks", "Held until commit – keep the transaction short", "Released after each statement"],
], widths=[3.6, 6.5, 6.5], caption="Transaction vs auto-commit")

pagebreak()
# ================================================================= PART B
h1("PART B – Implementation & Experimental Investigation")
para("Workflow: **Implement → Instrument → Stress → Measure → Compare → Interpret.** Screenshots are linked to the test they demonstrate; the raw text of every run is stored in the repository under Task*/output/.")

# ---------------------------------------------------------------- B1
h2("B1. File Store")
h3("B1.1 Implementation")
para("The bus-pass store (Task01) follows the text layout of A1: BusPass.java (record + validation), PassStore.java (add, find, list) and PassStoreMap.java (the alternative design). TODO 1 – PassStore.find() – reads line by line, skips damaged lines and returns the matching record.")
code([
    "public BusPass find(String passId) throws IOException {",
    "    damagedSeen = 0;",
    "    try (BufferedReader br = Files.newBufferedReader(file)) {",
    "        String line;",
    "        while ((line = br.readLine()) != null) {",
    "            BusPass p;",
    "            try { p = BusPass.parse(line); }",
    "            catch (IllegalArgumentException damaged) { damagedSeen++; continue; }  // skip damaged",
    "            if (p.passId().equals(passId)) return p;",
    "        }",
    "    } catch (NoSuchFileException missing) { System.out.println(\"  [store] file not found ...\"); }",
    "    return null;",
    "}",
], title="TODO 1 – completed PassStore.find()", size=8.5)
h3("B1.2 Test table (normal, non-existent, damaged, missing file, torn write)")
table([
    ["#", "Test", "Input", "Expected", "Actual", "Result"],
    ["1", "Normal", "find(\"P1003\")", "P1003 record", "BusPass[passId=P1003, … amount=1800.00]", "PASS"],
    ["2", "Damaged line", "find(\"P1002\") – line has 5 fields", "null", "null", "PASS"],
    ["3", "Absent id", "find(\"P9999\")", "null", "null", "PASS"],
    ["4", "List route 12", "list(12)", "P1001, P1007 (damaged P1002 skipped)", "P1001, P1007", "PASS"],
    ["5", "Missing file", "find() on a file that does not exist", "null, warning, no crash", "null + warning printed", "PASS"],
    ["6", "Add then find", "add(P1004); find(P1004)", "record found", "found", "PASS"],
    ["7", "Torn write", "file ends with 'P1005,22CS105,Torn Reco' (no LF); then add(P1006)", "P1005 dropped, P1006 intact", "P1005 → null; P1006 found", "PASS"],
], widths=[0.7, 2.4, 4.6, 3.4, 4.0, 1.5], caption="B1 functional tests", size=9)
picture("Task01/screenshots/B1_01_store_functional_tests.png", "Functional tests of PassStore: P1003 record, null for the damaged P1002 line (expected output of the task), absent id, list, missing file and torn write (Task B1).")
h3("B1.3 Comparison with the Part A pseudocode")
bullets([
    "**Same:** buffered line-by-line scan, split into 7 fields, damaged lines skipped and counted, missing file → null, newline repair before an append (Pseudocode 1–3).",
    "**More strict:** the implementation validates types (ID pattern, int route, ISO date, non-negative decimal) rather than only the field count, so '12x0' or a negative amount is also treated as damaged.",
    "**Not implemented in code:** fsync after each add (FileChannel.force) – the pseudocode lists it as the production hardening step; the torn-write repair is implemented and tested (test 7).",
])
h3("B1.4 Line-counting measurements (1 MB, 10 MB, 50 MB)")
para("Median of 3 runs (50 MB unbuffered: 1 run). 'read calls' = calls that reach the underlying FileInputStream.")
rows = [["Method", "1 MB (ms)", "10 MB (ms)", "50 MB (ms)", "read calls @ 50 MB", "Speed-up vs unbuffered @ 50 MB"]]
base50 = lcv(50, "unbuffered FileInputStream")
for m in ["unbuffered FileInputStream", "BufferedInputStream(8KB)", "byte[] 8 KB", "byte[] 64 KB", "BufferedReader.readLine", "Files.readAllLines", "Files.lines.count"]:
    calls = ""
    for r in lc:
        if r["method"] == m and int(r["sizeMB"]) == 50 and int(r["readCalls"]) > 0:
            calls = f0(float(r["readCalls"]))
    rows.append([m, f1(lcv(1, m)), f1(lcv(10, m)), f1(lcv(50, m)), calls or "n/a", f"{base50 / lcv(50, m):,.0f}×" if m != "unbuffered FileInputStream" else "1×"])
table(rows, widths=[4.4, 1.9, 2.0, 2.2, 3.1, 3.0], caption="Line-count time by method and file size", size=9, align_right_cols=(1, 2, 3, 4, 5))
picture("Task01/screenshots/B1_02_linecount_benchmark.png", "Buffered vs unbuffered line counting for 1 MB, 10 MB and 50 MB files: time, read() calls and retained heap (Task A2 / B1).")
figure("report_assets/chart_linecount.png", "Figure 1. Time to count lines (log-log). Unbuffered grows ~linearly with a very large constant; every buffered variant is 1–3 orders of magnitude lower.")
h3("B1.5 Interpretation")
bullets([
    f"Unbuffered reading made exactly **52,428,801** calls for the 50 MB file (prediction in A2 confirmed) and took **{f0(base50)} ms** – about 220 ns per byte of pure call overhead.",
    f"An 8 KB array cut the calls to 6,401 and the time to **{f1(lcv(50, 'byte[] 8 KB'))} ms (≈ {base50 / lcv(50, 'byte[] 8 KB'):,.0f}× faster)**; a 64 KB array gave 801 calls and {f1(lcv(50, 'byte[] 64 KB'))} ms. Going from 8 KB to 64 KB gained little – the diminishing-returns limit predicted in A2.",
    f"**Surprise worth reporting:** BufferedInputStream with one read() per byte made the same 6,401 system calls as the 8 KB array but still took {f1(lcv(50, 'BufferedInputStream(8KB)'))} ms – ~35× slower than the array loop – because every read() is a synchronized method call. Buffering removes system-call cost, but per-byte method-call cost remains; processing a whole array removes both.",
    f"Files.readAllLines() retained **{lcv(50, 'readAllLines_heapMB'):.0f} MB** for a 50 MB file; the streaming approaches retained ≈ 0 MB.",
])
h3("B1.6 Pass-search time, 1,000 → 100,000 records")
rows = [["Records n", "Scan: worst (last record) ms", "Scan: mean of 100 random ms", "100 scans total ms", "HashMap load ms", "100 lookups ms", "HashMap heap MB"]]
for r in sr:
    rows.append([f0(int(r["n"])), f"{float(r['scanWorstMs']):.1f}", f"{float(r['scanAvgMs']):.1f}", f0(float(r["scanQTotalMs"])), f"{float(r['mapLoadMs']):.1f}", f"{float(r['mapQueryTotalMs']):.2f}", f"{float(r['mapHeapMB']):.1f}"])
table(rows, widths=[2.0, 2.7, 2.8, 2.4, 2.3, 2.3, 2.1], caption="Search time vs file size", size=9, align_right_cols=tuple(range(7)))
picture("Task01/screenshots/B1_03_search_benchmark.png", "Search benchmark output: scan-per-find() versus HashMap loaded once (Task B1).")
figure("report_assets/chart_search.png", "Figure 2. Left: one scan is O(n). Right: 100 searches – O(q·n) scanning versus O(n + q) HashMap. (The small bump at n = 5,000 is JIT warm-up noise in a short run.)")
para("**Interpretation.** The scan time grows linearly: ×100 records gave ≈ ×27 in the mean time (1.0 ms → 27 ms; the small sizes are dominated by fixed start-up cost), and the worst case is ≈ 2× the mean as expected (the average search reads half the file). The HashMap needs 115 ms once and then answers in ≈ 0.0005 ms.")

h2("B1 – Alternative Design: scan per find() versus HashMap loaded at start-up")
h3("Why a damaged line with the wrong number of fields does not stop the search")
para("The loop is driven by readLine(), not by the parser. BusPass.parse() signals a bad line by throwing IllegalArgumentException; the exception is caught **inside** the loop body, the damaged counter is incremented and 'continue' moves to the next line. Only a genuine I/O failure (IOException) leaves the loop. A line with the wrong number of fields is therefore treated as data about the file (reported), not as a reason to stop. This is why P1003 was found in the supplied example even though P1002 (5 fields) precedes it.")
h3("Complexity: q searches on n passes")
table([
    ["", "Scan the file for every find()", "Load into a HashMap once"],
    ["Time for q searches", "**O(q × n)** – each search reads up to n lines", "**O(n + q)** – one O(n) load, then O(1) per lookup"],
    ["Space", "**O(1)** – one 8 KB buffer and one line (no growth with n)", "**O(n)** – every record stays in memory"],
    ["Measured memory", "≈ 0 MB extra", "34.9 MB for 100,000 records (≈ 365 bytes per record); 275 MB for 800,000"],
    ["Measured time, n = 100,000, q = 100", "2,743 ms (27.4 ms per search)", "114.8 + 0.05 = ≈ 115 ms"],
    ["Break-even", "—", "Load time ÷ scan time = 115 / 27.4 ≈ **4–5 searches**: from the fifth search onwards the map is cheaper"],
    ["Staleness", "Always reads the current file", "Needs refresh (or a write-through) when the file changes"],
    ["Best when", "Very few searches, huge file, tight memory", "Many searches on a file that fits in memory"],
], widths=[3.6, 6.5, 6.5], caption="Scan vs HashMap")
para("**Memory.** The scan approach keeps nothing but a buffer; the HashMap keeps every BusPass, its Strings and the map entry. At the expanded scale (800,000 passes) the map used 275 MB – acceptable on a server, but a warning that memory, not time, becomes the limit of the in-memory approach.")

# ---------------------------------------------------------------- B2
pagebreak()
h2("B2. Adversarial Input")
para("A local test database created only for this assignment was used (embedded Derby, created by the program in the project folder; table users with accounts admin / student). Both versions of the login were implemented in LoginDemo.java: unsafeLogin() joins the text into the SQL string; safeLogin() uses a PreparedStatement.")
h3("B2.1 Login tests")
table([
    ["Case", "user / password entered", "Expected", "Unsafe (concatenation)", "PreparedStatement"],
    ["1 valid", "admin / Admin@123", "accept", "ACCEPT", "ACCEPT"],
    ["2 wrong password", "admin / wrong", "reject", "reject", "reject"],
    ["3 comment-out", "admin' -- / anything", "reject", "**ACCEPT – bypass**", "reject"],
    ["4 always-true (user)", "' OR '1'='1 / ' OR '1'='1", "reject", "**ACCEPT – bypass**", "reject"],
    ["5 always-true (password)", "admin / x' OR 'a'='a", "reject", "**ACCEPT – bypass**", "reject"],
], widths=[3.3, 4.7, 1.8, 3.8, 3.0], caption="Observed behaviour of both login versions", size=9)
picture("Task02/screenshots/B2_01_sql_injection_login.png", "SQL-injection experiment: the SQL actually sent for each input, and the accept/reject outcome of the unsafe and PreparedStatement versions (Task B2).")
para("**Observation.** Cases 3–5 are accepted by the unsafe query but rejected by the PreparedStatement version with exactly the same text. The printed SQL shows why: in the unsafe version the attacker's quote and comment marker change the query's structure; in the PreparedStatement version the whole string is compared as a password value, which matches nobody.")
h3("B2.2 Damaged bus-pass data")
para("The file Task02/data/passes_damaged.txt contains the three required damaged forms (wrong field count, non-numeric amount, empty line) plus a bad date, a bad route, a negative amount and a bad ID prefix.")
picture("Task02/screenshots/B2_02_damaged_data.png", "Damaged-data test: each line classified VALID / DAMAGED with the reason; searches still find P2001 and P2009 on both sides of the damage (Task B2).")
table([
    ["Adversarial input", "Observed", "What it reveals about the original design"],
    ["Wrong field count (6 or 8 fields)", "DAMAGED: wrong field count", "A positional format has no self-description; without a count check the fields shift (amount read as date) or an array index fails. The 'exactly 7 fields' rule is the format's only structural check."],
    ["Non-numeric amount (12x0)", "DAMAGED: non-numeric amount", "Text files store everything as strings: types are enforced only by the program. An unguarded Double.parseDouble would throw and abort the whole load; here the exception is contained per line."],
    ["Empty line", "DAMAGED: empty line", "split() on an empty string yields one field. Blank lines appear after manual editing and after torn writes, so they must be a first-class case."],
    ["Bad date / negative amount / bad ID", "DAMAGED with reason", "Syntax is not enough – values must be checked for meaning. The database version enforces the same rule with a CHECK (amount >= 0) constraint (B3)."],
    ["(Design observation) comma inside a name", "toLine() refuses it", "The delimiter is not escaped, so a name such as 'Rao, Asha' would add an 8th field. Text formats need escaping/quoting or a forbidden-character rule; a database column has no such weakness."],
], widths=[4.0, 3.6, 9.0], caption="What each adversarial input reveals", size=9)

# ---------------------------------------------------------------- B3
pagebreak()
h2("B3. JDBC Performance Comparison")
h3("B3.1 Implementation")
para("Task03/PassLoader.java contains the three required techniques – TODO 1: loadStatement() (SQL text built per row), TODO 2: loadPrepared() (one PreparedStatement, executeUpdate per row) and TODO 3: loadBatch() (addBatch / executeBatch every 1,000 rows) – plus loadBatchTransaction() (batches inside one transaction) and loadMultiRow() (one INSERT … VALUES (…),(…) per 100 rows). The table is created from Task03/schema.sql:")
code([
    "CREATE TABLE bus_pass (",
    "    pass_id        VARCHAR(10)   NOT NULL PRIMARY KEY,",
    "    reg_no         VARCHAR(15)   NOT NULL,",
    "    name           VARCHAR(60)   NOT NULL,",
    "    route_no       INT           NOT NULL,",
    "    boarding_stop  VARCHAR(60)   NOT NULL,",
    "    validity_date  DATE          NOT NULL,",
    "    amount         DECIMAL(8,2)  NOT NULL CHECK (amount >= 0)",
    ");",
], title="SQL creation script (portable: MySQL 8.0.16+ and Derby)")
h3("B3.2 Ten records with each technique; verification with SELECT and ResultSetMetaData")
picture("Task03/screenshots/B3_01_loader_metadata_transaction_part1.png", "10 records inserted by Statement, PreparedStatement and batch (30 rows), SELECT output, and column names/types from ResultSetMetaData (Task B3).")
para("ResultSetMetaData confirms the intended types: VARCHAR(10/15/60), INTEGER, DATE, DECIMAL(8,2), all NOT NULL – i.e. the schema matches the design and money is stored as an exact decimal.")
h3("B3.3 Insertion time for 1,000 / 10,000 / 100,000 records")
names = []
for r in ins:
    if r["method"] not in names: names.append(r["method"])
rows = [["Method", "1,000 rows (ms)", "10,000 rows (ms)", "100,000 rows (ms)", "Rows/s @ 100,000", "Slower than best @ 100,000"]]
best = min(float(r["timeMs"]) for r in ins if int(r["rows"]) == 100000)
for m in names:
    d = {int(r["rows"]): float(r["timeMs"]) for r in ins if r["method"] == m}
    rows.append([m, f0(d[1000]), f0(d[10000]), f0(d[100000]), f0(100000 / d[100000] * 1000), f"{d[100000] / best:,.1f}×"])
table(rows, widths=[5.0, 2.2, 2.4, 2.5, 2.4, 2.1], caption="Insertion time (Apache Derby embedded; lower is better)", size=9, align_right_cols=(1, 2, 3, 4, 5))
picture("Task03/screenshots/B3_02_insert_benchmark.png", "Insertion benchmark for Statement, PreparedStatement and PreparedStatement batches, plus batches in one transaction and multi-row INSERT (Task B3).")
figure("report_assets/chart_insert.png", "Figure 3. Insertion time versus number of rows (log-log).")
h3("B3.4 Transaction experiment – invalid amount halfway through the input")
para("2,000 records were prepared and record #1001 was given amount = −500.00, which violates CHECK (amount >= 0). The same input was loaded twice: with auto-commit ON and with one transaction.")
picture("Task03/screenshots/B3_01_loader_metadata_transaction_part2.png", "Failure at record #1001: auto-commit leaves 1,000 rows, a single transaction leaves 0 rows (Task B3).")
table([
    ["Mode", "Rows in table after the failure", "Meaning"],
    ["Batches with auto-commit", "**1,000**", "The first batch was already committed: the table holds half the import and nothing says which half."],
    ["Batches in ONE transaction", "**0**", "rollback() undid everything: all or nothing. Fix the bad record and re-run safely."],
], widths=[4.6, 4.4, 7.6], caption="Transaction result")
h3("B3.5 Explanation of the observed differences")
bullets([
    "**Statement (115 s for 100,000 rows).** The SQL text differs for every row, so the database parses, compiles and plans 100,000 different statements and commits each one (auto-commit → one log flush per row). It is also the vulnerable style from B2.",
    "**PreparedStatement (13.3 s, 8.7× faster).** The statement is parsed and planned once; only the parameter values change. The remaining cost is 100,000 separate commits.",
    "**Batch with auto-commit (13.5 s) – no faster than PreparedStatement.** This is a genuine finding of the measurement: in auto-commit mode each batched row is still committed individually, and with an *embedded* database a 'round trip' costs almost nothing, so batching alone saves nothing. Over a network to MySQL each executeUpdate is a round trip, so batching helps there (especially with the connection property rewriteBatchedStatements=true) – but the commit cost is still present.",
    "**Batch + ONE transaction (0.99 s, 116× faster than Statement, 13× faster than PreparedStatement).** One commit instead of 100,000 and one statement plan; this is where the speed comes from. The price: all rows are held in one transaction (log space, locks), which is the reason to commit in chunks for very large loads.",
    "**Multi-row INSERT (1.28 s).** Also one transaction; slightly slower than the batch here because each 100-row statement text must be parsed again (a new SQL string per chunk), while the batch reuses one prepared plan.",
])
h3("B3.6 Recommendations")
table([
    ["Situation", "Recommended approach", "Reason (with evidence)"],
    ["**Nightly import of 100,000 passes**", "Validate the file first (B4), then PreparedStatement batches of 1,000 inside one transaction (or commit every N batches for restartability). On MySQL add rewriteBatchedStatements=true. If the server and file permissions allow, LOAD DATA INFILE is faster still.", "0.99 s vs 13.3 s (PreparedStatement) vs 115 s (Statement) for 100,000 rows; all-or-nothing on failure."],
    ["**One pass issued at the counter**", "A single PreparedStatement INSERT (auto-commit) – wrapped in a transaction only if other tables (e.g. the counter) change in the same operation.", "Measured 0.166 ms per pass; batching has nothing to batch, and PreparedStatement gives injection safety and plan reuse."],
], widths=[3.4, 7.0, 6.2], caption="Which technique where")
h3("B3.7 PreparedStatement batches versus multi-row INSERT versus MySQL LOAD DATA")
para("LOAD DATA could not be run in the build environment (no MySQL server), so this comparison is design reasoning supported by the measured batch and multi-row results above, not a measurement.", italic=True, size=10)
table([
    ["Aspect", "PreparedStatement batch (+ txn)", "Multi-row INSERT", "MySQL LOAD DATA [LOCAL] INFILE"],
    ["Speed (expected)", "Fast (measured 0.99 s / 100k)", "Fast (measured 1.28 s / 100k); often the best pure-JDBC option on MySQL", "Fastest – the server reads the file directly, minimal SQL parsing"],
    ["Where the file lives", "Java program reads it", "Java program reads it", "Server (or client with LOCAL, if enabled)"],
    ["Error handling", "Per-row exception → whole transaction can roll back; easy to report the bad row", "A bad row fails the whole multi-row statement", "Bad rows often become warnings and are skipped silently unless checked; harder to report"],
    ["Validation before load", "Done in Java (B4 reports rejects)", "Done in Java", "Done by column types only; pre-clean the file"],
    ["Portability", "Standard JDBC – any database", "Syntax common but limits differ (max packet size)", "MySQL-specific; needs FILE/LOCAL_INFILE privilege (security-sensitive)"],
    ["Memory", "Bounded by batch size", "Bounded by statement size", "Streaming by the server"],
], widths=[2.8, 4.6, 4.6, 4.6], caption="Bulk-load alternatives", size=9)

# ---------------------------------------------------------------- B4
pagebreak()
h2("B4. Final Engineering Decision")
para("**Scenario.** The transport office expands to 12 campuses with 100 times the current number of passes: 8,000 × 100 = **800,000 passes** (≈ 66,667 per campus).")
h3("B4.1 Migration program")
para("Migrate.java reads the legacy file, validates each line with the same rules as PassStore, reports records that fail validation (including duplicate pass IDs, which would violate the primary key), loads the valid records with PreparedStatement batches of 1,000 inside **one transaction**, verifies the row count and prints DatabaseMetaData. The legacy file (8,000 lines, one year) contains five deliberately bad lines: wrong field count, non-numeric amount, empty line, duplicate ID and bad date.")
picture("Task04/screenshots/B4_01_migration.png", "Migration: 8,000 lines read, 7,995 loaded in 8 batches in one transaction, 5 rejected records reported, row count verified, DatabaseMetaData (Task B4).")
table([
    ["Item", "Result"],
    ["Lines read / valid / rejected", "8,000 / 7,995 / 5"],
    ["Batches", "8 (7 × 1,000 + 995) in one transaction, committed"],
    ["Verification", "rows in table = 7,995 = valid records read → MATCH"],
    ["Database product", "Apache Derby 10.16.1.1"],
    ["JDBC driver", "Apache Derby Embedded JDBC Driver 10.16.1.1 (JDBC 4.2)"],
    ["Tables", "APP.BUS_PASS"],
], widths=[5.2, 11.4], caption="Migration summary", header=True)
h3("B4.2 Measurements for the expanded system (800,000 passes)")
picture("Task04/screenshots/B4_03_scale_benchmark.png", "Scale benchmark: text file, per-campus serialized files and one shared database with 800,000 passes (Task B4).")
table([
    ["Measurement (800,000 passes)", "Text file", "Serialized files (12, one per campus)", "Shared database"],
    ["Storage size", "48.3 MB", "96.6 MB", "—"],
    ["Bulk write / load", "1.0 s", "3.6 s", "8.7 s (one-time, batch + 1 txn)"],
    ["Find one pass by ID", "**228 ms** (scan) / 200 searches = 45.7 s", "must load a whole campus file first", "**0.119 ms** (primary key)"],
    ["Find after loading into memory", "HashMap: 1.17 s load, 275 MB heap, then 0.0016 ms", "list in memory", "—"],
    ["Cross-campus query 'all passes on route 12'", "full scan of the 48 MB file (not measured separately; a single-search scan averaged 228 ms)", "**1,494 ms** (load all 12 files)", "216 ms (no index) → **20 ms** (with index)"],
    ["Concurrent counters (8 threads × 200)", "**92 distinct numbers out of 1,600**, 14 duplicates, 1,494 torn reads", "not applicable – each file has one writer", "**1,600 unique**, 0 errors"],
    ["Safety on failure", "partial / torn lines", "all-or-nothing per file but whole file rewritten", "transaction rollback (B3: 0 rows left)"],
], widths=[4.2, 4.2, 4.2, 4.0], caption="Measured comparison at the expanded scale", size=9)
figure("report_assets/chart_scale.png", "Figure 4. Cost of one search at 800,000 passes (log scale): scan 228 ms, HashMap 0.0016 ms (after a 1.2 s load and 275 MB), database primary-key lookup 0.119 ms.")
h3("B4.3 Concurrent counters")
para("Eight threads (standing for eight campuses) each issued 200 pass numbers. The text-file counter used the naive read → increment → write cycle without locking; the database counter used UPDATE … SET next_no = next_no + 1 inside a transaction (row lock) and a PRIMARY KEY on the issued numbers.")
picture("Task04/screenshots/B4_02_concurrency.png", "Concurrent pass-number counters: unlocked text file versus database transaction (Task B4).")
para("The unlocked file counter ended at **92** instead of 1,600, produced duplicate numbers and many torn reads (a thread read the file while another had truncated it for writing). The database issued exactly 1,600 unique numbers. A JVM-level 'synchronized' would fix the file counter inside one process but not across 12 campus applications, and FileLock is advisory and unreliable on network shares; the database provides cross-process locking by design.")
h3("B4.4 Decision")
para("**Decision: use one shared relational database (MySQL) as the system of record for the expanded 12-campus system.** Text files are kept only for import/export and backups.")
table([
    ["Requirement", "Text files", "Serialization", "Database", "Evidence"],
    ["Concurrent counters", "✗ unsafe", "✗ one writer per file", "✓", "92 vs 1,600 unique numbers"],
    ["Searching 800,000 passes", "✗ 228 ms/search (O(n)); or 275 MB RAM for a map", "✗ load whole file", "✓ 0.119 ms (index)", "scale benchmark"],
    ["Safety (atomic, validated, injection-proof)", "✗ torn lines, no rollback", "✗ whole-file rewrite, unsafe if untrusted", "✓ transactions, constraints, PreparedStatement", "B2, B3 rollback test"],
    ["Cross-campus queries", "✗ scan", "✗ 1.5 s, all files", "✓ 20 ms with index", "route-12 query"],
    ["Simplicity / zero setup", "✓", "✓", "✗ needs a server, backups, tuning", "operational cost"],
], widths=[3.6, 3.4, 3.2, 3.2, 3.2], caption="Decision matrix", size=9)
h3("B4.5 One shared database versus separate serialized files for each campus")
table([
    ["Aspect", "One shared database", "Separate serialized file per campus"],
    ["Consistency across campuses", "Single source of truth; unique constraints enforced globally", "12 copies; duplicates and conflicts between campuses cannot be detected"],
    ["Concurrent access", "Row-level locks, transactions, many clients at once", "Whole-file read/write; two writers overwrite each other"],
    ["Cross-campus reports", "One SQL query (20 ms indexed); one index serves all campuses", "Load all 12 files (1.5 s) and merge in Java"],
    ["Failure / recovery", "Transaction log, backups, replication", "Crash during writeObject can corrupt the file; no rollback"],
    ["Schema evolution", "ALTER TABLE; old rows remain readable", "serialVersionUID; incompatible change breaks old files"],
    ["Security", "Roles, least privilege, PreparedStatement", "Deserializing a tampered file can execute attacker-chosen code paths"],
    ["Single point of failure", "Yes – mitigate with replication/backups; needs network", "No – each campus works offline"],
    ["Operations cost", "Server to administer", "None, but manual file synchronisation"],
    ["Reasonable use", "System of record for all 12 campuses", "Offline cache or a campus kiosk that syncs to the database"],
], widths=[3.6, 6.5, 6.5], caption="Shared database vs per-campus serialized files")
para("**Caveat.** The measurements ran on embedded Derby (no network). A MySQL server adds roughly 0.1–1 ms per round trip, so single-row lookups will be a little slower than 0.119 ms; batching and transactions matter more there. The decision does not depend on these small differences: the gaps measured here are 3 orders of magnitude (228 ms vs 0.119 ms) and the correctness result (92 vs 1,600) is qualitative.")

# ================================================================= CONCLUSION
h1("Conclusion")
bullets([
    "Persistence choices must be judged on **safety, efficiency, correctness and scale**, not just on 'does it save'. The same 8,000 records are fine in a text file; 800,000 records written by 12 campuses at once are not.",
    "Buffering removed 99.99 % of the system calls (52,428,801 → 6,401 or 801) and made the line count 530× faster; readAllLines() trades memory (≈ 120 MB for 50 MB) for convenience.",
    "A damaged line must be an **event to report**, not a reason to stop – and string-built SQL must never be used: three injection strings bypassed the unsafe login and all were rejected by PreparedStatement.",
    "Inserting 100,000 rows took 115 s with Statement and 0.99 s with PreparedStatement batches inside one transaction; the same transaction guarantees that a mid-load failure leaves 0 rows, not 1,000.",
    "For the expanded system the evidence supports a **shared relational database**.",
])
h2("Repository map (GitHub)")
table([
    ["Folder", "Contents"],
    ["Task01 (B1)", "BusPass, PassStore (TODO 1 find), PassStoreMap, StoreDemo, LineCountBench, SearchBench; data/passes_sample.txt; output/ + screenshots/"],
    ["Task02 (B2)", "LoginDemo (unsafe vs PreparedStatement), DamagedDataTest; data/passes_damaged.txt; output/ + screenshots/"],
    ["Task03 (B3)", "Db, PassLoader (TODO 1–3 + transaction + multi-row), PassBench; schema.sql; output/ + screenshots/"],
    ["Task04 (B4)", "MakeLegacyFile, Migrate, ConcurrencyDemo, ScaleBench; data/legacy_passes.txt; output/ (incl. rejected_records.txt) + screenshots/"],
    ["Root", "README.md, lib/ (Derby jars), compile.sh, run_all.sh, commit_progressively.sh, tools/"],
], widths=[3.2, 13.4], caption="Repository contents")
h2("Limitations")
bullets([
    "Database tasks ran on embedded Apache Derby instead of MySQL (no MySQL server was available); SQL and code are portable and the switch is documented in README.md. LOAD DATA was discussed, not measured.",
    "Timings come from a small shared 2-vCPU container; they show orders of magnitude and growth shapes, not the speed of any particular machine. Short runs include JIT warm-up noise.",
    "The original TODO skeleton files were not part of the supplied PDF; the TODO methods were written from the task descriptions.",
    "The Student fields in A3 are assumed (the assignment does not list them).",
])

doc.save(OUT)
print("saved", OUT)
