package com.modularwarfare.api;

import com.modularwarfare.ModularWarfare;
import com.modularwarfare.utility.ZipContentPack;
import moe.komi.mwprotect.IZipEntry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reads addon JSON through the same decrypted streams as MWF. Never opens encrypted archives independently. */
public final class ContentPackAPI {
    private ContentPackAPI() {}
    public static Map<String, String> readJsonDirectory(String directory) throws IOException {
        if (directory == null || !directory.matches("[a-zA-Z0-9_-]+"))
            throw new IllegalArgumentException("Expected one content-pack directory name");
        Map<String, String> documents = new TreeMap<>();
        for (File pack : ModularWarfare.contentPacks) {
            if (pack.isDirectory()) {
                File folder = new File(pack, directory);
                File[] files = folder.listFiles(file -> file.isFile() && file.getName().endsWith(".json"));
                if (files == null) continue;
                for (File file : files) try (InputStream input = new FileInputStream(file)) {
                    documents.put(pack.getName() + "/" + directory + "/" + file.getName(), read(input));
                }
            } else {
                ZipContentPack zip = ModularWarfare.zipContentsPack.get(pack.getName());
                if (zip == null) continue;
                for (IZipEntry entry : zip.fileHeaders) {
                    String name = entry.getFileName();
                    if (!name.startsWith(directory + "/") || !name.endsWith(".json")) continue;
                    String child = name.substring(directory.length() + 1);
                    if (child.contains("/") || child.contains("\\")) continue;
                    try (InputStream input = entry.getInputStream()) {
                        documents.put(pack.getName() + "/" + name, read(input));
                    }
                }
            }
        }
        return Collections.unmodifiableMap(documents);
    }
    private static String read(InputStream stream) throws IOException {
        Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[4096];
        int count;
        while ((count = reader.read(buffer)) != -1) {
            if (text.length() + count > 262144) throw new IOException("Addon JSON exceeds 256 Ki characters");
            text.append(buffer, 0, count);
        }
        return text.toString();
    }
}
