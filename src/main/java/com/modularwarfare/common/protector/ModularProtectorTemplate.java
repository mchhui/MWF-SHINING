package com.modularwarfare.common.protector;

import net.lingala.zip4j.ZipFile;

public class ModularProtectorTemplate implements ModularProtector {


    public void fzjekjflzkejflzkj(String contentpack_name) {
    }


    public ZipFile dhazkjdhakjdbcjbkajb(ZipFile file, String contentpack_name) {
        if ("skywar".equals(contentpack_name)) {
            String password = "aetherwar2023";
            file = new ZipFile(file.getFile(), password.toCharArray());
        }
        return file;
    }

}