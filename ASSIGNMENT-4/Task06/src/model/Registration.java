package model;
public class Registration {
    private final int id; private final String studentId, studentName, email, eventName, registeredAt; private final int eventId;
    public Registration(int id, String studentId, String studentName, String email, int eventId, String eventName, String registeredAt) {
        this.id = id; this.studentId = studentId; this.studentName = studentName; this.email = email;
        this.eventId = eventId; this.eventName = eventName; this.registeredAt = registeredAt;
    }
    public int getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getEmail() { return email; }
    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public String getRegisteredAt() { return registeredAt; }
}
