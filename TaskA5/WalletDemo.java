import java.sql.*;

public class WalletDemo {
    static void transfer(Connection c, boolean failAfterDebit) throws SQLException {
        c.setAutoCommit(false);
        try (PreparedStatement debit = c.prepareStatement(
                     "UPDATE wallet SET paise=paise-? WHERE id=? AND paise>=?");
             PreparedStatement credit = c.prepareStatement(
                     "UPDATE wallet SET paise=paise+? WHERE id=?")) {
            debit.setLong(1, 50000);
            debit.setString(2, "hostel");
            debit.setLong(3, 50000);
            if (debit.executeUpdate() != 1) throw new SQLException("Debit rejected");
            if (failAfterDebit) throw new SQLException("Simulated failure after debit", "45000");
            credit.setLong(1, 50000);
            credit.setString(2, "canteen");
            if (credit.executeUpdate() != 1) throw new SQLException("Credit rejected");
            c.commit();
        } catch (SQLException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(true);
        }
    }
    static void check(Statement s, String label, long hostel, long canteen) throws SQLException {
        try (ResultSet r = s.executeQuery("SELECT paise FROM wallet ORDER BY id DESC")) {
            if (!r.next() || r.getLong(1) != hostel || !r.next() || r.getLong(1) != canteen) {
                throw new AssertionError(label);
            }
        }
        System.out.printf("%s | hostel %d | canteen %d | total %d paise%n",
                label, hostel, canteen, hostel + canteen);
    }
    public static void main(String[] args) throws SQLException {
        try (Connection c = Database.connect(); Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TEMPORARY TABLE wallet (id VARCHAR(12) PRIMARY KEY,"
                    + "paise BIGINT NOT NULL CHECK (paise >= 0)) ENGINE=InnoDB");
            s.executeUpdate("INSERT INTO wallet VALUES ('hostel',100000),('canteen',20000)");
            check(s, "Before transfer", 100000, 20000);
            try {
                transfer(c, true);
                throw new AssertionError("expected failure");
            } catch (SQLException e) {
                if (!"45000".equals(e.getSQLState())) throw e;
                check(s, "After rollback", 100000, 20000);
            }
            transfer(c, false);
            check(s, "After commit", 50000, 70000);
        }
    }
}
