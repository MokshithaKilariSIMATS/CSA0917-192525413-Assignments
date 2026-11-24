package service;
import dao.*;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.regex.Pattern;
import model.Event;
import model.Registration;

/** Business rules: validation, duplicate checking, paging maths and CSV export. No Swing and no SQL here. */
public class RegistrationService {
    public static final int PAGE_SIZE = 50;
    private static final Pattern ID = Pattern.compile("\\d{9}");
    private static final Pattern NAME = Pattern.compile("[A-Za-z][A-Za-z .'-]{2,49}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private final RegistrationDao dao;
    public RegistrationService(RegistrationDao dao) { this.dao = dao; }

    public List<Event> events() throws Exception { return dao.findEvents(); }

    public void register(String studentId, String name, String email, Event event) throws Exception {
        if (event == null) throw new IllegalArgumentException("Select an event.");
        if (studentId == null || !ID.matcher(studentId.trim()).matches()) throw new IllegalArgumentException("Student ID must be exactly 9 digits.");
        if (name == null || !NAME.matcher(name.trim()).matches()) throw new IllegalArgumentException("Name must be 3-50 letters (spaces, . ' - allowed).");
        if (email == null || !EMAIL.matcher(email.trim()).matches()) throw new IllegalArgumentException("Enter a valid e-mail address.");
        String id = studentId.trim();
        if (dao.exists(id, event.getId())) throw new DuplicateRegistrationException("Student " + id + " is already registered for " + event.getName() + ".");
        dao.insert(id, name.trim(), email.trim(), event.getId());   // DB UNIQUE key is the final guard against races
    }
    public int totalRows(Integer eventId) throws Exception { return dao.count(eventId); }
    public int totalPages(Integer eventId) throws Exception { return Math.max(1, (dao.count(eventId) + PAGE_SIZE - 1) / PAGE_SIZE); }
    public List<Registration> page(Integer eventId, int pageIndex) throws Exception { return dao.findPage(eventId, PAGE_SIZE, pageIndex * PAGE_SIZE); }
    public List<Registration> all(Integer eventId) throws Exception { return dao.findAll(eventId); }

    public int exportCsv(Integer eventId, Writer w) throws Exception, IOException {
        List<Registration> rows = dao.findAll(eventId);
        w.write("id,student_id,student_name,email,event,registered_at\n");
        for (Registration r : rows)
            w.write(r.getId() + "," + r.getStudentId() + ",\"" + r.getStudentName() + "\"," + r.getEmail() + ",\"" + r.getEventName() + "\"," + r.getRegisteredAt() + "\n");
        return rows.size();
    }
}
