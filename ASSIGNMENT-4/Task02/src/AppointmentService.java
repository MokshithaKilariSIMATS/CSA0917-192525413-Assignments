import java.util.*;
import java.util.regex.Pattern;

/** Validation + booking rules. Doctor lists are loaded once into a Map (cache). */
public class AppointmentService {
    private static final Pattern PHONE = Pattern.compile("[6-9]\\d{9}");
    private final DoctorDao dao;
    private final Map<String, List<String>> doctorCache = new HashMap<>();
    private final Set<String> booked = new HashSet<>();   // "doctor|slot"

    public AppointmentService(DoctorDao dao) {
        this.dao = dao;
        for (String d : dao.departments()) doctorCache.put(d, dao.doctorsIn(d)); // load ONCE
    }
    public List<String> departments() { return dao.departments(); }
    public List<String> doctors(String dept) { return doctorCache.getOrDefault(dept, Collections.emptyList()); }

    public String book(String patient, String phone, String dept, String doctor, String slot) {
        if (patient == null || patient.trim().length() < 3) throw new IllegalArgumentException("Patient name must have at least 3 characters.");
        if (phone == null || !PHONE.matcher(phone.trim()).matches()) throw new IllegalArgumentException("Phone must be 10 digits starting with 6-9.");
        if (dept == null || doctor == null) throw new IllegalArgumentException("Select a department and a doctor.");
        if (!booked.add(doctor + "|" + slot)) throw new IllegalStateException(doctor + " is already booked at " + slot + ".");
        return "Appointment confirmed: " + patient.trim() + " with " + doctor + " at " + slot;
    }
}
