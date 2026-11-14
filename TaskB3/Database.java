import java.sql.*;

public final class Database {
    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(
                System.getenv().getOrDefault("ASSIGNMENT5_URL",
                        "jdbc:mysql://127.0.0.1:33405/csa09_assignment5?rewriteBatchedStatements=true"),
                System.getenv().getOrDefault("ASSIGNMENT5_USER", "root"),
                System.getenv().getOrDefault("ASSIGNMENT5_PASSWORD", ""));
    }
}
