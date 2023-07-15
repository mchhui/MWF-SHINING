package com.modularwarfare.common.protector;

import net.lingala.zip4j.ZipFile;

public class ModularProtectorTemplate implements ModularProtector {


    public void fetchData(String contentpack_name) {
    }


    public ZipFile decryptAlternateFile(ZipFile file, String contentpack_name) {
        if (file.getFile().getName().contains("skywar")) {
            String password = "aetherwar2023";
            file.setPassword(password.toCharArray());
        }
        return file;
    }

}