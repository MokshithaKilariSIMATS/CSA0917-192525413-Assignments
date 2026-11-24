import java.util.*;

/** Compares "query the (simulated) DB on every department change" with "load once into a Map". */
public class CacheBenchmark {
    public static void main(String[] a) {
        int latencyMs = 5, changes = 200;
        DoctorDao dao1 = new DoctorDao(latencyMs);
        List<String> depts = dao1.departments();
        long t0 = System.nanoTime();
        for (int i = 0; i < changes; i++) dao1.doctorsIn(depts.get(i % depts.size()));
        long perCallMs = (System.nanoTime() - t0) / 1_000_000;

        DoctorDao dao2 = new DoctorDao(latencyMs);
        long t1 = System.nanoTime();
        Map<String, List<String>> cache = new HashMap<>();
        for (String d : depts) cache.put(d, dao2.doctorsIn(d));          // 4 DB calls only
        for (int i = 0; i < changes; i++) cache.get(depts.get(i % depts.size()));
        long cachedMs = (System.nanoTime() - t1) / 1_000_000;

        System.out.printf("Simulated DB latency per query : %d ms%n", latencyMs);
        System.out.printf("Department changes performed   : %d%n", changes);
        System.out.printf("DB on every change : %4d ms  (%d DB calls)%n", perCallMs, dao1.getCalls());
        System.out.printf("Load once into Map : %4d ms  (%d DB calls)%n", cachedMs, dao2.getCalls());
    }
}
