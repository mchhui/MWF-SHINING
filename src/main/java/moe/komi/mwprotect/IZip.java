package moe.komi.mwprotect;

import java.io.IOException;
import java.util.Set;

public interface IZip {
    Set<IZipEntry> getFileList() throws IOException;
}
