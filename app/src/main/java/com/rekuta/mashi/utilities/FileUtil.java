package com.rekuta.mashi.utilities;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    public static List<Float> readFloatsFromFile(String path) {
        ArrayList<Float> floats = new ArrayList<>();
        try (FileInputStream inputStream = new FileInputStream(path)) {
            byte[] buffer = new byte[4];
            while (inputStream.read(buffer) != -1) {
                floats.add(ByteBuffer.wrap(buffer).getFloat());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return floats;
    }

    public static void writeFloatsToFile(List<Float> floats, String path) {
        if (floats.isEmpty()) return;

        ByteBuffer buffer = ByteBuffer.allocate(floats.size() * 4);

        for (float f : floats) {
            buffer.putFloat(f);
        }

        try (FileOutputStream fos = new FileOutputStream(path)) {
            fos.write(buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
