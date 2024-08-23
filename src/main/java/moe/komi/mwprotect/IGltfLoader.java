package moe.komi.mwprotect;

import mchhui.hegltf.GltfDataModel;

import java.io.InputStream;

public interface IGltfLoader {
    public void loadGltf(GltfDataModel gltfDataModel, InputStream inputStream, String path);
}
