import java.io.*;
import java.util.zip.*;

public class ZipBench {
    public static void main(String[] args) throws Exception {
        File tempDir = new File("temp_benchmark");
        tempDir.mkdirs();
        File zipFile = new File(tempDir, "test.zip");

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            for (int i = 1; i <= 50000; i++) {
                zos.putNextEntry(new ZipEntry("file_" + i + ".txt"));
                zos.write(("Hello world! This is a test file number " + i).getBytes());
                zos.closeEntry();
            }
        }

        long start = System.currentTimeMillis();
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                while (zis.read(buffer) > 0) {}
            }
        }
        long unbuffered = System.currentTimeMillis() - start;

        start = System.currentTimeMillis();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile)))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                while (zis.read(buffer) > 0) {}
            }
        }
        long buffered = System.currentTimeMillis() - start;

        System.out.println("Unbuffered: " + unbuffered + " ms");
        System.out.println("Buffered: " + buffered + " ms");
    }
}
