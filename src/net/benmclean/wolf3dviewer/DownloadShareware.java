package net.benmclean.wolf3dviewer;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadShareware {
    public static void WOLF3DShareware() throws Exception {
        if (!Files.exists(Paths.get("WOLF3D/WOLF3D.EXE"))) {
            download("https://archive.org/download/Wolfenstein3d/Wolfenstein3dV14sw.ZIP");
            if (!Files.exists(Paths.get("WOLF3D")))
                Files.createDirectory(Paths.get("WOLF3D"));
            unzip("Wolfenstein3dV14sw.ZIP", new File("WOLF3D"));
            Files.delete(Paths.get("Wolfenstein3dV14sw.ZIP"));
        }
    }

    public static void download (String url) throws IOException {
        try (BufferedInputStream inputStream = new BufferedInputStream(new URL(url).openStream());
             FileOutputStream fileOS = new FileOutputStream("Wolfenstein3dV14sw.ZIP")) {
            byte data[] = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
                fileOS.write(data, 0, byteContent);
            }
        } catch (IOException e) {
            // handles IO exceptions
            throw e;
        }
    }

    public static void unzip(String fileZip, File destDir) throws Exception {
//            String fileZip = "src/main/resources/unzipTest/compressed.zip";
//            File destDir = new File("src/main/resources/unzipTest");
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            File newFile = newFile(destDir, zipEntry);
            FileOutputStream fos = new FileOutputStream(newFile);
            int len;
            while ((len = zis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
        zis.close();
    }

    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
