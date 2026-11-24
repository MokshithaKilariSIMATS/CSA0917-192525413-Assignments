import java.util.LinkedHashMap;
import java.util.Map;

/** Pure conversion logic shared by all four GUI versions (fixed illustrative rates, base = USD). */
public class ConversionLogic {
    public static final Map<String, Double> PER_USD = new LinkedHashMap<>();
    static {
        PER_USD.put("USD", 1.0); PER_USD.put("INR", 83.20); PER_USD.put("EUR", 0.92);
        PER_USD.put("GBP", 0.79); PER_USD.put("JPY", 149.50);
    }
    /** @throws NumberFormatException if the text is not a positive number */
    public static String convert(String amountText, String from, String to) {
        double amt = Double.parseDouble(amountText.trim());
        if (amt <= 0) throw new NumberFormatException("Amount must be positive");
        double result = amt / PER_USD.get(from) * PER_USD.get(to);
        return String.format("%.2f %s = %.2f %s", amt, from, result, to);
    }
}
