public class UncaughtTrace {
    static int readAmount(String text) { return Integer.parseInt(text); }
    static int withdraw(String text)   { return readAmount(text) * 2; }
    public static void main(String[] args) {
        System.out.println("Starting withdrawal...");
        System.out.println("Result = " + withdraw("12x"));
        System.out.println("never printed");
    }
}
