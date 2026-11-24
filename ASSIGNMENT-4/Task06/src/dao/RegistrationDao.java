package dao;
import java.util.List;
import model.Event;
import model.Registration;

/** Data-access contract: the only layer that knows about storage. eventId == null means "all events". */
public interface RegistrationDao {
    List<Event> findEvents() throws Exception;
    boolean exists(String studentId, int eventId) throws Exception;
    void insert(String studentId, String name, String email, int eventId) throws DuplicateRegistrationException, Exception;
    int count(Integer eventId) throws Exception;
    List<Registration> findPage(Integer eventId, int limit, int offset) throws Exception;   // SQL LIMIT/OFFSET
    List<Registration> findAll(Integer eventId) throws Exception;
}
