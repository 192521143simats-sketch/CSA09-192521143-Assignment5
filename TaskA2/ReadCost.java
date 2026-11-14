public class ReadCost {
    public static void main(String[] args) {
        long bytes = 50L * 1024 * 1024;
        long block = 64L * 1024;
        System.out.println("Input bytes (50 MiB): " + bytes);
        System.out.println("Single-byte calls including EOF: " + (bytes + 1));
        System.out.println("Full-block calls including EOF: " + ((bytes + block - 1) / block + 1));
        System.out.println("Buffer bytes: " + block);
    }
}
