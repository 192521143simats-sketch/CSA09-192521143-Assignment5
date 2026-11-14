import java.sql.*;
import java.nio.file.Path;

public class LoginCheck {
    static boolean accepts(Connection connection, String id, String password) throws SQLException {
        String sql = "SELECT 1 FROM assignment5_users WHERE id = ? AND pwd = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, password);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try (Connection connection = Database.connect()) {
            boolean correct = accepts(connection, "demo", "sample-password");
            boolean wrong = accepts(connection, "demo", "incorrect");
            boolean quoted = accepts(connection, "O'Brien", "incorrect");
            if (!correct || wrong || quoted) throw new AssertionError("login cases");
            System.out.println("PreparedStatement: valid credentials accepted = " + correct);
            System.out.println("PreparedStatement: wrong password accepted = " + wrong);
            System.out.println("PreparedStatement: quoted unknown ID accepted = " + quoted);
        }
        int[] rejected = {0};
        BusFile store = new BusFile(Path.of(args[0]));
        var rows = store.list(12, rejection -> {
            rejected[0]++;
            System.out.println(rejection);
        });
        if (rejected[0] != 3 || rows.size() != 1) throw new AssertionError("file cases");
        System.out.println("Valid route 12 passes retained = " + rows.size());
    }
}
