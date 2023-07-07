package moe.komi.mwprotect;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class LegacyZip implements IZip {
    ZipFile zipFile;

    public LegacyZip(String fileName) throws IOException {
        this(new File(fileName));
    }

    public LegacyZip(File file) throws IOException {
        try {
            zipFile = new ZipFile(file);
        } catch (ZipException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Set<IZipEntry> getFileList() throws IOException {
        try {
            List<FileHeader> fileHeaders = zipFile.getFileHeaders();
            Set<IZipEntry> result = new HashSet<>();
            fileHeaders.forEach(entry -> result.add(new LegacyZipEntry(zipFile, entry)));
            return result;
        } catch (ZipException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int hashCode() {
        return zipFile.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LegacyZip)) return false;
        LegacyZip that = (LegacyZip) obj;
        return Objects.equals(zipFile, that.zipFile);
    }


}
