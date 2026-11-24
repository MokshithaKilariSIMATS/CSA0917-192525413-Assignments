import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * One bus-pass record (Part A, Task A1 layout).
 * Text layout (7 comma-separated fields, one record per line):
 *   passId,regNo,name,route,stop,validity(yyyy-MM-dd),amount
 */
public record BusPass(String passId, String regNo, String name, int route,
                      String stop, LocalDate validity, BigDecimal amount)
        implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final int FIELDS = 7;

    /** Parse one line. Throws IllegalArgumentException whose message is the reason it is damaged. */
    public static BusPass parse(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("empty line");
        }
        String[] f = line.split(",", -1);
        if (f.length != FIELDS) {
            throw new IllegalArgumentException("wrong field count (" + f.length + ", expected " + FIELDS + ")");
        }
        for (int i = 0; i < f.length; i++) f[i] = f[i].trim();
        if (!f[0].matches("P\\d+")) {
            throw new IllegalArgumentException("bad pass id '" + f[0] + "'");
        }
        int route;
        try {
            route = Integer.parseInt(f[3]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("non-numeric route '" + f[3] + "'");
        }
        LocalDate validity;
        try {
            validity = LocalDate.parse(f[5]);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("bad validity date '" + f[5] + "'");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(f[6]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("non-numeric amount '" + f[6] + "'");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("negative amount '" + f[6] + "'");
        }
        return new BusPass(f[0], f[1], f[2], route, f[4], validity, amount);
    }

    /** Returns null when the line is valid, otherwise the reason it is damaged. */
    public static String check(String line) {
        try {
            parse(line);
            return null;
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String toLine() {
        if (name.indexOf(',') >= 0 || stop.indexOf(',') >= 0) {
            throw new IllegalArgumentException("delimiter ',' not allowed inside name/stop");
        }
        return String.join(",", passId, regNo, name, Integer.toString(route), stop,
                validity.toString(), amount.toPlainString());
    }
}
