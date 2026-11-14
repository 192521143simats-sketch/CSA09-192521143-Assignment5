import java.sql.*;
import java.util.*;

public class DaoDemo {
    record Student(String registerNo, String name, String program) {}

    interface Dao<T, K> {
        void insert(T row) throws SQLException;
        Optional<T> findById(K id) throws SQLException;
        List<T> findAll() throws SQLException;
        boolean update(T row) throws SQLException;
        boolean delete(K id) throws SQLException;
    }

    record StudentDao(Connection connection) implements Dao<Student, String> {
        private int change(String sql, String... fields) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (int i = 0; i < fields.length; i++) ps.setString(i + 1, fields[i]);
                return ps.executeUpdate();
            }
        }
        private static Student map(ResultSet rows) throws SQLException {
            return new Student(rows.getString("reg_no"), rows.getString("name"),
                    rows.getString("program"));
        }
        public void insert(Student s) throws SQLException {
            change("INSERT INTO student(reg_no,name,program) VALUES (?,?,?)",
                    s.registerNo(), s.name(), s.program());
        }
        public Optional<Student> findById(String id) throws SQLException {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT reg_no,name,program FROM student WHERE reg_no=?")) {
                ps.setString(1, id);
                try (ResultSet rows = ps.executeQuery()) {
                    return rows.next() ? Optional.of(map(rows)) : Optional.empty();
                }
            }
        }
        public List<Student> findAll() throws SQLException {
            try (Statement s = connection.createStatement();
                 ResultSet rows = s.executeQuery("SELECT reg_no,name,program FROM student ORDER BY reg_no")) {
                List<Student> students = new ArrayList<>();
                while (rows.next()) students.add(map(rows));
                return students;
            }
        }
        public boolean update(Student s) throws SQLException {
            return change("UPDATE student SET name=?,program=? WHERE reg_no=?",
                    s.name(), s.program(), s.registerNo()) == 1;
        }
        public boolean delete(String id) throws SQLException {
            return change("DELETE FROM student WHERE reg_no=?", id) == 1;
        }
    }

    public static void main(String[] args) throws SQLException {
        try (Connection c = Database.connect(); Statement s = c.createStatement()) {
            s.execute("CREATE TEMPORARY TABLE student (reg_no VARCHAR(24) PRIMARY KEY,"
                    + "name VARCHAR(120) NOT NULL, program VARCHAR(80) NOT NULL)");
            Dao<Student, String> dao = new StudentDao(c);
            dao.insert(new Student("R101", "Divya", "Engineering"));
            System.out.println("Find R101: " + dao.findById("R101").orElseThrow());
            System.out.println("Update R101: " + dao.update(new Student("R101", "Divya", "CSE")));
            System.out.println("List: " + dao.findAll());
            System.out.println("Delete R101: " + dao.delete("R101"));
            if (dao.findById("R101").isPresent()) throw new AssertionError();
        }
    }
}
