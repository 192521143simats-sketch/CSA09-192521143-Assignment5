import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class MigratePasses {
    private static final String INSERT = """
            INSERT INTO campus_bus_pass
            (pass_id, reg_no, name, route_no, amount, boarding_stop, valid_to)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    public static void main(String[] args) throws Exception {
        Path input = Path.of(args[0]);
        if (!java.nio.file.Files.isRegularFile(input)) throw new IllegalArgumentException("Missing pass file");
        try (Connection connection = Database.connect()) {
            DatabaseMetaData metadata = connection.getMetaData();
            System.out.println(metadata.getDatabaseProductName() + " " + metadata.getDatabaseProductVersion());
            System.out.println("Driver: " + metadata.getDriverName() + " " + metadata.getDriverVersion());
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (tables.next()) System.out.println("Table: " + tables.getString("TABLE_NAME"));
            }
            migrate(connection, new BusFile(input));
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery(
                         "SELECT pass_id, boarding_stop, valid_to FROM campus_bus_pass ORDER BY pass_id")) {
                while (rows.next()) {
                    System.out.printf("%s | %s | %s%n", rows.getString(1), rows.getString(2), rows.getDate(3));
                }
            }
        }
    }

    private static void migrate(Connection connection, BusFile source) throws Exception {
        connection.setAutoCommit(false);
        Set<String> ids = new HashSet<>();
        try (Statement query = connection.createStatement();
             ResultSet rows = query.executeQuery("SELECT pass_id FROM campus_bus_pass")) {
            while (rows.next()) ids.add(rows.getString(1).toUpperCase(Locale.ROOT));
        }
        int[] accepted = {0};
        int[] rejected = {0};
        try (PreparedStatement batch = connection.prepareStatement(INSERT)) {
            source.scan(row -> {
                if (!ids.add(row.id().toUpperCase(Locale.ROOT))) {
                    rejected[0]++;
                    System.out.println("Rejected " + row.id() + " | duplicate ID");
                    return true;
                }
                try {
                    batch.setString(1, row.id());
                    batch.setString(2, row.register());
                    batch.setString(3, row.name());
                    batch.setInt(4, row.route());
                    batch.setBigDecimal(5, row.amount());
                    batch.setString(6, row.stop());
                    batch.setDate(7, java.sql.Date.valueOf(row.validTo()));
                    batch.addBatch();
                    if (++accepted[0] % 1000 == 0) batch.executeBatch();
                    return true;
                } catch (SQLException e) {
                    throw new java.io.IOException("Database rejected a migration batch", e);
                }
            }, reason -> {
                rejected[0]++;
                System.out.println("Rejected " + reason);
            });
            if (accepted[0] % 1000 != 0) batch.executeBatch();
            connection.commit();
            System.out.printf("Committed %d passes | rejected %d records%n", accepted[0], rejected[0]);
        } catch (Exception failure) {
            connection.rollback();
            throw failure;
        }
    }
}
