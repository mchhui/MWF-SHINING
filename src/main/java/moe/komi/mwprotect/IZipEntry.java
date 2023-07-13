package moe.komi.mwprotect;

import java.io.IOException;
import java.io.InputStream;

public interface IZipEntry {
    String getFileName();

    InputStream getInputStream() throws IOException;

    long getHandle();
}
