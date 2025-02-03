package com.rekuta.mashi.utilities;

import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class FileUtil {

    public static void createNewFile(String path) {
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            makeDir(dirPath);
        }

        File file = new File(path);

        try {
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void deleteFile(String path) {
        File file = new File(path);

        if (!file.exists()) return;

        if (file.isFile()) {
            file.delete();
            return;
        }

        File[] fileArr = file.listFiles();

        if (fileArr != null) {
            for (File subFile : fileArr) {
                if (subFile.isDirectory()) {
                    deleteFile(subFile.getAbsolutePath());
                }

                if (subFile.isFile()) {
                    subFile.delete();
                }
            }
        }

        file.delete();
    }

    public static boolean isExistFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void makeDir(String path) {
        if (!isExistFile(path)) {
            File file = new File(path);
            file.mkdirs();
        }
    }

    public static ArrayList<String> listFiles(String path) {
        ArrayList<String> fileNames = new ArrayList<>();
        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return fileNames;

        File[] listFiles = dir.listFiles();
        if (listFiles == null) return fileNames;

        for (File file: listFiles) {
            fileNames.add(file.getName());
        }
        return fileNames;
    }

    public static ArrayList<String> listDirs(String path) {
        ArrayList<String> fileNames = new ArrayList<>();
        File dir = new File(path);
        if (!dir.exists() || dir.isFile()) return fileNames;

        File[] listFiles = dir.listFiles();
        if (listFiles == null) return fileNames;

        for (File file: listFiles) {
            if (file.isDirectory()) {
                fileNames.add(file.getName());
            }
        }
        return fileNames;
    }

    public static String getExternalStorageDir() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getAbsolutePath(String parent, String child) {
        File file = new File(parent, child);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        return file.getAbsolutePath();
    }

    public static byte[] readBytesFromFile(String path) {
        ByteArrayOutputStream byteArrayOutputStream = null;

        try (FileInputStream inputStream = new FileInputStream(path)) {
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        assert byteArrayOutputStream != null;
        return byteArrayOutputStream.toByteArray();
    }

    public static void writeBytesToFile(byte[] bytes, String path) {
        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
