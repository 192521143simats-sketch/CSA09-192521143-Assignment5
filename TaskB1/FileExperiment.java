import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;

public class FileExperiment {
    private static final Consumer<String> IGNORE = message -> {};

    public static void main(String[] args) throws IOException {
        Path scratch = Files.createTempDirectory("assignment5-b1");
        try {
            Path path = scratch.resolve("passes.txt");
            BusFile store = new BusFile(path);
            require(store.find("P1", IGNORE) == null, "missing file");
            store.add(pass("P1", 12));
            Files.writeString(path, "P2|BROKEN\n", StandardOpenOption.APPEND);
            store.add(pass("P3", 7));
            require(store.find("P3", System.out::println).route() == 7, "find");
            require(store.find("P404", IGNORE) == null, "missing ID");
            require(store.list(12, IGNORE).getFirst().name().equals("Arun | Kumar"), "list");
            Files.writeString(path, BusFile.encode(pass("P4", 12)), StandardOpenOption.APPEND);
            require(store.find("P4", System.out::println) == null, "torn tail");
            store.add(pass("P5", 12));
            require(store.list(12, IGNORE).size() == 2, "tail recovery");
            System.out.println("PASS: add, find P3, missing P404, list route 12, tail recovery");

            System.out.println("\nMiB   LF count   single-byte ms   64 KiB blocks ms");
            for (int size : new int[]{1, 10, 50}) {
                int bytes = size * 1024 * 1024;
                Path log = scratch.resolve("log.txt");
                byte[] block = new byte[65536];
                Arrays.fill(block, (byte) 'A');
                for (int i = 127; i < block.length; i += 128) block[i] = '\n';
                try (OutputStream out = Files.newOutputStream(log)) {
                    for (int written = 0; written < bytes; written += block.length) out.write(block);
                }
                long start = System.nanoTime();
                long a = count(log, false);
                double single = ms(start);
                start = System.nanoTime();
                long b = count(log, true);
                double buffered = ms(start);
                require(a == b && b == bytes / 128, "line count");
                System.out.printf(Locale.ROOT, "%3d %10d %16.3f %18.3f%n", size, a, single, buffered);
            }

            System.out.println("\nRecords   last-ID median ms (5 runs)");
            for (int size : new int[]{1000, 10000, 100000}) {
                try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                    for (int i = 1; i <= size; i++) {
                        writer.write(BusFile.encode(pass("P" + i, 12)));
                        writer.write('\n');
                    }
                }
                store.find("P" + size, IGNORE);
                double[] runs = new double[5];
                for (int i = 0; i < runs.length; i++) {
                    long start = System.nanoTime();
                    require(store.find("P" + size, IGNORE) != null, "last ID");
                    runs[i] = ms(start);
                }
                Arrays.sort(runs);
                System.out.printf(Locale.ROOT, "%7d %19.3f%n", size, runs[2]);
            }
        } finally {
            try (var files = Files.list(scratch)) {
                for (Path file : files.toList()) Files.delete(file);
            }
            Files.delete(scratch);
        }
    }

    static BusFile.Pass pass(String id, int route) {
        return new BusFile.Pass(id, "192521143", "Arun | Kumar", route, "North Gate",
                LocalDate.of(2027, 6, 30), new BigDecimal("4500.00"));
    }

    static long count(Path path, boolean buffered) throws IOException {
        long count = 0;
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            if (buffered) {
                byte[] block = new byte[65536];
                int n;
                while ((n = in.read(block)) != -1) {
                    for (int i = 0; i < n; i++) if (block[i] == '\n') count++;
                }
            } else {
                int value;
                while ((value = in.read()) != -1) if (value == '\n') count++;
            }
        }
        return count;
    }

    static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    static double ms(long start) { return (System.nanoTime() - start) / 1_000_000.0; }
}
