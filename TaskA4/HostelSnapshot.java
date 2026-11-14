import java.io.*;
import java.sql.Connection;
import java.util.List;

public class HostelSnapshot {
    record Hostel(List<Room> rooms) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
    record Room(int number, List<Resident> residents) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
    static class Resident implements Serializable {
        private static final long serialVersionUID = 1L;
        String name = "Divya";
        String registerNo = "R101";
        String phoneNumber;
        transient String password = "fixture-only";
        transient Connection connection;
    }
    public static void main(String[] args) throws Exception {
        var resident = new Resident();
        var hostel = new Hostel(List.of(new Room(101, List.of(resident, resident))));
        var bytes = new ByteArrayOutputStream();
        try (var writer = new ObjectOutputStream(bytes)) {
            writer.writeObject(hostel);
        }
        try (var reader = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            var room = ((Hostel) reader.readObject()).rooms().getFirst();
            var loaded = room.residents().getFirst();
            boolean shared = loaded == room.residents().get(1);
            if (loaded.password != null || loaded.connection != null || !shared) throw new AssertionError();
            System.out.println("Room: " + room.number() + " | Resident: " + loaded.name + " " + loaded.registerNo);
            System.out.println("Restored password: " + loaded.password + " | Connection: " + loaded.connection);
            System.out.println("Restored optional phone: " + loaded.phoneNumber);
            System.out.println("Shared resident reference preserved: " + shared);
        }
    }
}
