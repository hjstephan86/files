package de.epp.cfiles;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@CapacitorPlugin(name = "ZipExtract")
public class ZipPlugin extends Plugin {

    @PluginMethod
    public void extract(PluginCall call) {
        String zipPath = call.getString("zipPath");
        String destDir = call.getString("destDir");

        if (zipPath == null || destDir == null) {
            call.reject("zipPath and destDir are required");
            return;
        }

        try {
            File destFolder = new File(destDir);
            if (!destFolder.exists()) {
                destFolder.mkdirs();
            }
            String canonicalDest = destFolder.getCanonicalPath() + File.separator;

            int count = 0;
            FileInputStream fis = new FileInputStream(zipPath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File outFile = new File(destDir, entryName);

                // Zip-Slip verhindern
                if (!outFile.getCanonicalPath().startsWith(canonicalDest)) {
                    zis.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(outFile);
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = zis.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    count++;
                }
                zis.closeEntry();
            }

            zis.close();
            fis.close();

            JSObject result = new JSObject();
            result.put("count", count);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("extract_failed");
        }
    }

    @PluginMethod
    public void pack(PluginCall call) {
        String srcDir = call.getString("srcDir");
        String zipPath = call.getString("zipPath");

        if (srcDir == null || zipPath == null) {
            call.reject("srcDir and zipPath are required");
            return;
        }

        try {
            File src = new File(srcDir);
            if (!src.exists() || !src.isDirectory()) {
                call.reject("srcDir does not exist or is not a directory");
                return;
            }

            FileOutputStream fos = new FileOutputStream(zipPath);
            ZipOutputStream zos = new ZipOutputStream(fos);
            String basePath = src.getCanonicalPath() + File.separator;
            addDirToZip(src, basePath, zos);
            zos.close();
            fos.close();

            call.resolve(new JSObject());
        } catch (Exception e) {
            call.reject("pack_failed");
        }
    }

    private void addDirToZip(File dir, String basePath, ZipOutputStream zos) throws java.io.IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[8192];
        for (File file : files) {
            String entryName = file.getCanonicalPath().substring(basePath.length());
            if (file.isDirectory()) {
                zos.putNextEntry(new ZipEntry(entryName + "/"));
                zos.closeEntry();
                addDirToZip(file, basePath, zos);
            } else {
                zos.putNextEntry(new ZipEntry(entryName));
                FileInputStream fis = new FileInputStream(file);
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
                fis.close();
                zos.closeEntry();
            }
        }
    }
}
