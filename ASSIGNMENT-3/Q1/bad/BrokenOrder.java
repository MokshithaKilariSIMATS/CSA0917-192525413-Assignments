import java.io.*;
public class BrokenOrder {
    public static void main(String[] args) {
        try {
            new FileReader("cards.txt").read();
        } catch (IOException e) {            // broader first
            System.out.println("IO problem");
        } catch (FileNotFoundException e) {  // narrower afterwards -> unreachable
            System.out.println("file missing");
        }
    }
}
