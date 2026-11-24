import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;

/** Creates Task04/data/legacy_passes.txt : 8,000 records (one year) with a few deliberately bad lines. */
public class MakeLegacyFile {
    public static void main(String[] args) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get("Task04/data/legacy_passes.txt"))) {
            for (int i = 1; i <= 8000; i++) {
                BusPass p = new BusPass("P" + (300000 + i), "22CS" + String.format("%05d", i % 90000),
                        "Student " + i, 1 + (i % 25), "Stop " + (i % 40),
                        LocalDate.parse("2027-03-31"), new BigDecimal("1500.00"));
                String line = p.toLine();
                switch (i) {
                    case 1000 -> line = "P301000,22CS01000,Missing Fields,5";               // wrong field count
                    case 2500 -> line = "P302500,22CS02500,Bad Amount,5,Stop 3,2027-03-31,15OO.00"; // non-numeric amount
                    case 4000 -> line = "";                                                  // empty line
                    case 5500 -> line = "P300001,22CS99999,Duplicate Id,7,Stop 9,2027-03-31,1500.00"; // duplicate key
                    case 7000 -> line = "P307000,22CS07000,Bad Date,5,Stop 3,31/03/2027,1500.00";
                    default -> { }
                }
                w.write(line);
                w.write('\n');
            }
        }
        System.out.println("legacy_passes.txt written (8,000 lines)");
    }
}
