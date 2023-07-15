package com.modularwarfare.common.protector;

import com.modularwarfare.ModularWarfare;
import net.lingala.zip4j.ZipFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class ModularProtectorOfficial implements ModularProtector {
    private HashMap<String, String> decryptedResponses = new HashMap<>();
    private HashMap<String, String> passwordMappings = new HashMap<>();
    private HashMap<String, String> urlMappings = new HashMap<>();
    private HashMap<String, String> encryptedResponses = new HashMap<>();

    public static String encrypt(String input) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if ((c >= 'a') && (c <= 'm')) {
                c = (char) (c + '\r');
            } else if ((c >= 'A') && (c <= 'M')) {
                c = (char) (c + '\r');
            } else if ((c >= 'n') && (c <= 'z')) {
                c = (char) (c - '\r');
            } else if ((c >= 'N') && (c <= 'Z')) {
                c = (char) (c - '\r');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public void fetchData(String url) {
        URL requestUrl = null;
        try {
            requestUrl = new URL("http://modularmods.net/api/pack.php");
            Map<String, Object> parameters = new LinkedHashMap<>();
            parameters.put(reverseString("dXLTQ"), reverseString(encrypt("oeIKophMwjDqKYGD")));
            parameters.put("xwazs", url);

            StringBuilder postData = new StringBuilder();
            for (Entry<String, Object> param : parameters.entrySet()) {
                if (postData.length() != 0) {
                    postData.append('&');
                }
                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

            HttpURLConnection conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            this.decryptedResponses.put(url, base64DecodePassword(new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8")).lines().collect(Collectors.joining())));
            this.encryptedResponses.put(url, encrypt(this.decryptedResponses.get(url)));
        } catch (IOException e) {
            ModularWarfare.LOGGER.info("A critical error occurred openning " + url + ", please verify your internet connection.");
            throw new RuntimeException(e);
        }
    }

    private String base64DecodePassword(String password) {
        String reversePassword = reverseString(password);
        Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(reversePassword);
        return new String(bytes);
    }

    private String reverseString(String str) {
        StringBuilder builder = new StringBuilder();
        builder.append(str);
        builder.reverse();
        return String.valueOf(builder);
    }

    private String appendPassword(String password) {
        String pass = password + "eadsq";
        Decoder decoder = Base64.getDecoder();
        byte[] bytes = decoder.decode(pass);
        return new String(bytes);
    }

    public ZipFile decryptFile(ZipFile file, String fileName) {
        if (file != null) {
            fetchData(fileName);
            if (this.urlMappings.containsKey(fileName)) {
                file.setPassword(base64DecodePassword(this.urlMappings.get(fileName)).toCharArray());
                this.urlMappings.remove(fileName);
                return file;
            }
        }
        return null;
    }

    public ZipFile decryptFileWithAlternateMethod(ZipFile file, String fileName) {
        if (file != null) {
            fetchData(fileName);
            if (this.urlMappings.containsKey(fileName)) {
                file.setPassword(base64DecodePassword(this.urlMappings.get(fileName)).toCharArray());
                this.urlMappings.remove(fileName);
                return file;
            }
        }
        return null;
    }

    public ZipFile decryptAlternateFile(ZipFile file, String fileName) {
        if (file != null) {
            if ("alpha.zip".equals(file.getFile().getName())
                    || "s.zip".equals(file.getFile().getName())
                    || "t.zip".equals(file.getFile().getName())) {
                file.setPassword("aetherwar2023".toCharArray());
                return file;
            }

            if (this.encryptedResponses.containsKey(fileName)) {
                file.setPassword(encrypt(this.encryptedResponses.get(fileName)).toCharArray());
                return file;
            } else {
                fetchData(fileName);
                if (this.encryptedResponses.containsKey(fileName)) {
                    file.setPassword(encrypt(this.encryptedResponses.get(fileName)).toCharArray());
                    return file;
                }
            }
        }
        return null;
    }

    public ZipFile decryptAnotherAlternateFile(ZipFile file, String fileName) {
        if (file != null) {
            fetchData(fileName);
            if (this.passwordMappings.containsKey(fileName)) {
                file.setPassword(base64DecodePassword(this.passwordMappings.get(fileName)).toCharArray());
                this.passwordMappings.remove(fileName);
                return file;
            }
        }
        return null;
    }
}
