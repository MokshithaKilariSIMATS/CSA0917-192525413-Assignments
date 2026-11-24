import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

// ---- exception hierarchy -------------------------------------------------
class ExamException extends Exception {                       // checked root
    ExamException(String m) { super(m); }
    ExamException(String m, Throwable c) { super(m, c); }
}
class ExamWindowClosedException extends ExamException {       // checked: expected business failure
    ExamWindowClosedException(String m) { super(m); }
}
class NotRegisteredException extends ExamException {          // checked
    NotRegisteredException(String m) { super(m); }
}
class FileTooLargeException extends ExamException {           // checked
    FileTooLargeException(String m) { super(m); }
}
class SubmissionStorageException extends ExamException {      // checked, wraps IOException
    SubmissionStorageException(String m, Throwable c) { super(m, c); }
}
class InvalidFileNameException extends RuntimeException {     // UNCHECKED: programming/caller error
    InvalidFileNameException(String m) { super(m); }
}

// ---- storage layer -----------------------------------------------------------
class Storage {
    void save(String student, String fileName, long sizeKb) throws SubmissionStorageException {
        try {
            if (sizeKb > 500) throw new IOException("Disk quota exceeded");
            System.out.println("    [Storage ] saved " + fileName);
        } catch (IOException e) {
            throw new SubmissionStorageException("Could not store " + fileName, e);   // chaining
        }
    }
}

// ---- service layer -----------------------------------------------------------
class SubmissionService {
    private final Storage storage = new Storage();
    private final Set<String> registered = Set.of("S1", "S2", "S3");
    private final LocalDateTime deadline = LocalDateTime.of(2026, 11, 23, 12, 0);
    static final long LIMIT_KB = 1000;

    void submit(String student, String fileName, long sizeKb, LocalDateTime now)
            throws ExamWindowClosedException, NotRegisteredException, FileTooLargeException, SubmissionStorageException {
        if (fileName == null || !fileName.endsWith(".pdf"))
            throw new InvalidFileNameException("Only .pdf accepted: " + fileName);
        if (now.isAfter(deadline)) throw new ExamWindowClosedException("Exam window closed at " + deadline);
        if (!registered.contains(student)) throw new NotRegisteredException(student + " is not registered for CSA09");
        if (sizeKb > LIMIT_KB) throw new FileTooLargeException(sizeKb + " KB exceeds limit " + LIMIT_KB + " KB");
        System.out.println("    [Service ] validated " + student + " / " + fileName);
        storage.save(student, fileName, sizeKb);
    }
}

// ---- controller layer (final handler) ------------------------------------------
public class Portal {
    static final SubmissionService service = new SubmissionService();
    static final LocalDateTime BEFORE = LocalDateTime.of(2026, 11, 23, 10, 0);
    static final LocalDateTime AFTER  = LocalDateTime.of(2026, 11, 23, 13, 0);

    static String controller(String student, String file, long kb, LocalDateTime now) {
        try {
            service.submit(student, file, kb, now);
            return "Submission accepted.";
        } catch (ExamWindowClosedException | NotRegisteredException | FileTooLargeException e) {
            return "Rejected: " + e.getMessage();
        } catch (SubmissionStorageException e) {
            System.out.println("    [Controller] logging chained cause -> " + e.getCause());
            return "Sorry, your file could not be saved. Please try again or contact the exam cell.";
        } catch (InvalidFileNameException e) {
            return "Invalid file: " + e.getMessage();
        }
    }

    static void scenario(String title, String s, String f, long kb, LocalDateTime t) {
        System.out.println(title);
        System.out.println("    Student sees: " + controller(s, f, kb, t));
    }

    public static void main(String[] args) {
        System.out.println("===== Scenarios (one per exception) =====");
        scenario("1. Valid submission",              "S1", "answer.pdf", 120,  BEFORE);
        scenario("2. ExamWindowClosedException",     "S1", "answer.pdf", 120,  AFTER);
        scenario("3. NotRegisteredException",        "S9", "answer.pdf", 120,  BEFORE);
        scenario("4. FileTooLargeException",         "S1", "answer.pdf", 1500, BEFORE);
        scenario("5. SubmissionStorageException",    "S1", "answer.pdf", 800,  BEFORE);
        scenario("6. InvalidFileNameException (unchecked)", "S1", "answer.exe", 100, BEFORE);

        System.out.println("\n===== Chained stack trace (cause preserved) =====");
        try { new Storage().save("S1", "answer.pdf", 800); }
        catch (SubmissionStorageException e) { e.printStackTrace(System.out); }

        System.out.println("\n===== Re-thrown WITHOUT the cause =====");
        try {
            try { new Storage().save("S1", "answer.pdf", 800); }
            catch (SubmissionStorageException e) { throw new SubmissionStorageException("Could not store answer.pdf", null); }
        } catch (SubmissionStorageException e) {
            e.printStackTrace(System.out);
            System.out.println("getCause() = " + e.getCause());
        }
        System.out.println("\n===== getCause() walk of the chained exception =====");
        try { new Storage().save("S1", "answer.pdf", 800); }
        catch (SubmissionStorageException e) {
            Throwable t = e; int level = 0;
            while (t != null) { System.out.println("  level " + level++ + ": " + t.getClass().getName() + " : " + t.getMessage()); t = t.getCause(); }
        }
    }
}
