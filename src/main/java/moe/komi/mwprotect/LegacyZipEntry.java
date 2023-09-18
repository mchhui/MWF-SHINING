package moe.komi.mwprotect;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class LegacyZipEntry implements IZipEntry {
    private final ZipFile zipFile;
    private final FileHeader fileHeader;

    LegacyZipEntry(ZipFile zipFile, FileHeader fileHeader) {
        this.zipFile = zipFile;
        this.fileHeader = fileHeader;
    }

    @Override
    public String getFileName() {
        return fileHeader.getFileName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return zipFile.getInputStream(fileHeader);
        } catch (ZipException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getHandle() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LegacyZipEntry)) return false;
        LegacyZipEntry that = (LegacyZipEntry) o;
        return Objects.equals(zipFile, that.zipFile) && Objects.equals(fileHeader, that.fileHeader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(zipFile, fileHeader);
    }
}
