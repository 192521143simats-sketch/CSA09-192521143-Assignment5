import java.sql.*;
import java.util.*;

public class InsertExperiment {
    private static final String INSERT = "INSERT INTO bus_pass VALUES ";
    private enum Method { STATEMENT, PREPARED, BATCH, MULTI_ROW }

    public static void main(String[] args) throws SQLException {
        try (Connection c = Database.connect()) {
            System.out.println("MySQL " + c.getMetaData().getDatabaseProductVersion());
            for (Method method : Method.values()) {
                reset(c);
                insert(c, method, rows(10), true);
                verify(c, 10);
                System.out.println(method + " | SELECT verified all 10 rows and five column types");
            }
            System.out.println("\nRows       Statement     Prepared        Batch    Multi-row (ms)");
            for (int n : new int[]{1000, 10000, 100000}) {
                List<String[]> data = rows(n);
                System.out.printf(Locale.ROOT, "%6d", n);
                for (Method method : Method.values()) {
                    reset(c);
                    long start = System.nanoTime();
                    insert(c, method, data, false);
                    System.out.printf(Locale.ROOT, "%13.2f", elapsed(start));
                    verify(c, n);
                }
                System.out.println();
            }

            List<String[]> data = rows(4000);
            System.out.println("\nBatch size 1000, 4000 rows | mode | success ms | rows after bad amount");
            for (boolean autoCommit : new boolean[]{true, false}) {
                reset(c);
                long start = System.nanoTime();
                insert(c, Method.BATCH, data, autoCommit);
                double timing = elapsed(start);
                verify(c, 4000);
                reset(c);
                List<String[]> broken = new ArrayList<>(data);
                broken.set(2000, new String[]{"P2000", "R2000", "Student2000", "12", "invalid"});
                try {
                    insert(c, Method.BATCH, broken, autoCommit);
                    throw new AssertionError("expected invalid amount");
                } catch (NumberFormatException expected) {
                    int retained = autoCommit ? 2000 : 0;
                    verify(c, retained);
                    System.out.printf(Locale.ROOT, "%s | %.2f ms | %d rows%n",
                            autoCommit ? "Auto-commit" : "Transaction", timing, retained);
                }
            }
        }
    }

    private static List<String[]> rows(int n) {
        List<String[]> data = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            data.add(new String[]{"P" + i, "R" + i, "Student" + i, "12", "4500.00"});
        }
        return data;
    }

    private static void reset(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.executeUpdate("TRUNCATE TABLE bus_pass");
        }
    }

    private static void insert(Connection c, Method method, List<String[]> rows, boolean autoCommit)
            throws SQLException {
        if (method == Method.BATCH && !autoCommit) {
            if (PassLoader.loadAll(c, rows) != rows.size()) throw new AssertionError("loader count");
            return;
        }
        c.setAutoCommit(autoCommit);
        try (Statement statement = c.createStatement();
             PreparedStatement prepared = c.prepareStatement(INSERT + "(?, ?, ?, ?, ?)")) {
            StringJoiner tuples = new StringJoiner(",");
            for (int i = 0; i < rows.size(); i++) {
                String[] row = rows.get(i);
                switch (method) {
                    case STATEMENT -> statement.executeUpdate(INSERT + tuple(row));
                    case MULTI_ROW -> {
                        tuples.add(tuple(row)); // Only deterministic, generated fixtures.
                        if ((i + 1) % 1000 == 0 || i == rows.size() - 1) {
                            statement.executeUpdate(INSERT + tuples);
                            tuples = new StringJoiner(",");
                        }
                    }
                    case PREPARED, BATCH -> {
                        prepared.setString(1, row[0]);
                        prepared.setString(2, row[1]);
                        prepared.setString(3, row[2]);
                        prepared.setInt(4, Integer.parseInt(row[3]));
                        prepared.setDouble(5, Double.parseDouble(row[4]));
                        if (method == Method.PREPARED) {
                            prepared.executeUpdate();
                        } else {
                            prepared.addBatch();
                            if ((i + 1) % 1000 == 0 || i == rows.size() - 1) prepared.executeBatch();
                        }
                    }
                }
            }
            if (!autoCommit) c.commit();
        } catch (SQLException | RuntimeException e) {
            if (!autoCommit) c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }

    private static String tuple(String[] p) {
        return "('" + p[0] + "','" + p[1] + "','" + p[2] + "'," + p[3] + "," + p[4] + ")";
    }

    private static void verify(Connection c, int expected) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT pass_id,reg_no,name,route_no,amount FROM bus_pass")) {
            int[] types = {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.DECIMAL};
            ResultSetMetaData metadata = r.getMetaData();
            if (metadata.getColumnCount() != types.length) throw new AssertionError("column count");
            for (int i = 0; i < types.length; i++) {
                if (metadata.getColumnType(i + 1) != types[i]) throw new AssertionError("column type");
            }
            int count = 0;
            while (r.next()) {
                int id = Integer.parseInt(r.getString(1).substring(1));
                if (id < 0 || id >= expected || !r.getString(2).equals("R" + id)
                        || !r.getString(3).equals("Student" + id) || r.getInt(4) != 12
                        || r.getBigDecimal(5).compareTo(new java.math.BigDecimal("4500.00")) != 0) {
                    throw new AssertionError("incorrect row: " + id);
                }
                count++;
            }
            if (count != expected) throw new AssertionError("expected " + expected + ", found " + count);
        }
    }

    private static double elapsed(long start) { return (System.nanoTime() - start) / 1_000_000.0; }
}
