import java.nio.file.Files;
import java.time.LocalDate;
import java.math.BigDecimal;
public class LayoutDemo {
    public static void main(String[] args) throws Exception {
        var path = Files.createTempFile("pass-layout", ".txt");
        try {
            var row = new BusFile.Pass("P1", "R1", "Arun | Kumar", 12, "North Gate",
                    LocalDate.of(2027, 6, 30), new BigDecimal("4500.00"));
            var store = new BusFile(path);
            store.add(row);
            System.out.print("UTF-8 record: " + Files.readString(path));
            System.out.println("Found name: " + store.find("P1", System.out::println).name());
            System.out.println("Route 12 matches: " + store.list(12, System.out::println).size());
        } finally {
            Files.deleteIfExists(path.resolveSibling(path.getFileName() + ".lock"));
            Files.delete(path);
        }
    }
}
