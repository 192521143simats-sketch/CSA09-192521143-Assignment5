import java.io.*;
import java.math.BigDecimal;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.CRC32;

public final class BusFile {
    public record Pass(String id, String register, String name, int route, String stop,
                       LocalDate validTo, BigDecimal amount) {}

    private final Path path;
    public BusFile(Path path) { this.path = path; }

    public static String encode(Pass p) {
        for (String field : List.of(p.id(), p.register(), p.name(), p.stop())) {
            if (field.isBlank() || field.contains("\n") || field.contains("\r")) {
                throw new IllegalArgumentException("blank field or embedded line break");
            }
        }
        String body = String.join("|", escape(p.id()), escape(p.register()), escape(p.name()),
                Integer.toString(p.route()), escape(p.stop()), p.validTo().toString(),
                p.amount().toPlainString());
        String line = body + "|" + checksum(body);
        decode(line);
        return line;
    }

    private static String escape(String field) {
        return field.replace("%", "%25").replace("|", "%7C");
    }

    private static String unescape(String field) {
        if (field.replace("%25", "").replace("%7C", "").contains("%")) {
            throw new IllegalArgumentException("invalid escape");
        }
        return field.replace("%7C", "|").replace("%25", "%");
    }

    private static String checksum(String body) {
        CRC32 crc = new CRC32();
        crc.update(body.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().toHexDigits((int) crc.getValue());
    }

    public static Pass decode(String line) {
        String[] f = line.split("\\|", -1);
        if (f.length != 8) throw new IllegalArgumentException("expected 8 fields");
        String id = unescape(f[0]);
        String register = unescape(f[1]);
        String name = unescape(f[2]);
        int route = Integer.parseInt(f[3]);
        String stop = unescape(f[4]);
        LocalDate validTo = LocalDate.parse(f[5]);
        BigDecimal amount;
        try {
            amount = new BigDecimal(f[6]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("amount is not numeric", e);
        }
        if (id.isBlank() || register.isBlank() || name.isBlank() || stop.isBlank()
                || id.length() > 24 || register.length() > 24 || name.length() > 120
                || stop.length() > 120 || validTo.getYear() < 1000 || validTo.getYear() > 9999
                || route <= 0 || amount.signum() < 0 || amount.scale() > 2
                || amount.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException("field outside permitted range");
        }
        if (!checksum(line.substring(0, line.lastIndexOf('|'))).equals(f[7])) {
            throw new IllegalArgumentException("checksum mismatch");
        }
        return new Pass(id, register, name, route, stop, validTo, amount);
    }

    public void add(Pass pass) throws IOException {
        synchronized (BusFile.class) { append(pass); }
    }

    private void append(Pass pass) throws IOException {
        String line = encode(pass);
        try (FileChannel coordination = FileChannel.open(
                     path.resolveSibling(path.getFileName() + ".lock"),
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = coordination.lock();
             RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw")) {
            if (find(pass.id(), ignored -> {}) != null) {
                throw new IllegalArgumentException("duplicate pass ID");
            }
            long end = file.length();
            while (end > 0) {
                file.seek(end - 1);
                if (file.read() == '\n') break;
                end--;
            }
            file.setLength(end);
            file.seek(end);
            file.write((line + "\n").getBytes(StandardCharsets.UTF_8));
            file.getChannel().force(true);
        }
    }

    @FunctionalInterface
    public interface Visitor { boolean visit(Pass row) throws IOException; }

    public void scan(Visitor visitor, Consumer<String> reject) throws IOException {
        // Track LF explicitly so an otherwise valid but unterminated tail is rejected.
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            int ch;
            int number = 0;
            StringBuilder line = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                if (ch != '\n') {
                    line.append((char) ch);
                    continue;
                }
                number++;
                Pass row;
                try {
                    int length = line.length();
                    if (length > 0 && line.charAt(length - 1) == '\r') length--;
                    row = decode(line.substring(0, length));
                } catch (IllegalArgumentException | java.time.DateTimeException e) {
                    reject.accept("line " + number + " | " + e.getMessage());
                    line.setLength(0);
                    continue;
                }
                line.setLength(0);
                if (!visitor.visit(row)) return;
            }
            if (!line.isEmpty()) reject.accept("line " + (number + 1) + " | incomplete write");
        } catch (NoSuchFileException e) {
            // A missing store has zero records.
        }
    }

    public Pass find(String id, Consumer<String> reject) throws IOException {
        Pass[] match = new Pass[1];
        scan(row -> {
            if (!row.id().equals(id)) return true;
            match[0] = row;
            return false;
        }, reject);
        return match[0];
    }

    public List<Pass> list(int route, Consumer<String> reject) throws IOException {
        List<Pass> rows = new ArrayList<>();
        scan(row -> {
            if (row.route() == route) rows.add(row);
            return true;
        }, reject);
        return rows;
    }
}


