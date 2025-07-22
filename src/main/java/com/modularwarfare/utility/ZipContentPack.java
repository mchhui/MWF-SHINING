package com.modularwarfare.utility;

import com.modularwarfare.loader.ObjModel;
import moe.komi.mwprotect.IZip;
import moe.komi.mwprotect.IZipEntry;

import java.util.HashMap;
import java.util.Set;

public class ZipContentPack {

    public String contentPack;

    public Set<IZipEntry> fileHeaders;

    public IZip zipFile;

    public HashMap<String, ObjModel> models_cache = new HashMap<>();

    public ZipContentPack(String contentPack, Set<IZipEntry> fileHeaders, IZip zipFile) {
        this.contentPack = contentPack;
        this.fileHeaders = fileHeaders;
        this.zipFile = zipFile;
    }
}
