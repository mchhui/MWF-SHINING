package com.modularwarfare.utility;

import com.google.common.collect.Sets;
import moe.komi.mwprotect.IZip;
import moe.komi.mwprotect.IZipEntry;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraft.client.resources.ResourcePackFileNotFoundException;
import net.minecraftforge.fml.common.FMLContainerHolder;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.discovery.ModCandidate;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * A remote (on the web), protectable resource pack
 */
@SideOnly(Side.CLIENT)
public class MWResourcePack extends AbstractResourcePack implements FMLContainerHolder {


    private ModContainer container;

    public MWResourcePack(ModContainer container) {
        super(container.getSource());
        this.container = container;
    }

    @Override
    public String getPackName() {
        return ((Container) getFMLContainer()).packName;
    }

    @Override
    public BufferedImage getPackImage() {
        return null;
    }

    @Override
    public ModContainer getFMLContainer() {
        return container;
    }

    @Override
    protected InputStream getInputStreamByName(String resourceName) throws IOException {
        InputStream s = null;

        try {
            String name1 = resourceName.replaceAll("\\\\", "/");
            IZipEntry fileHandle = ((Container) getFMLContainer()).zipEntries.stream().filter(fileHeader -> fileHeader.getFileName().equalsIgnoreCase(name1)).findFirst().orElse(null);
            if (fileHandle == null) {
                String name2 = name1.replaceAll("/", "\\\\");
                fileHandle = ((Container) getFMLContainer()).zipEntries.stream().filter(fileHeader -> fileHeader.getFileName().equalsIgnoreCase(name2)).findFirst().orElse(null);
            }
            if (fileHandle != null) {
                s = fileHandle.getInputStream();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (s == null) {
            if ("pack.mcmeta".equals(resourceName)) {
                return new ByteArrayInputStream(("{\n" +
                        " \"pack\": {\n" +
                        "   \"description\": \"dummy MW pack for " + container.getName() + "\",\n" +
                        "   \"pack_format\": 3\n" +
                        "}\n" +
                        "}").getBytes(StandardCharsets.UTF_8));
            }
            throw new ResourcePackFileNotFoundException(this.resourcePackFile, resourceName);
        } else {
            return s;
        }
    }

    public boolean hasResourceName(String name) {
        String name1 = name.replaceAll("\\\\", "/");
        boolean flag = ((Container) getFMLContainer()).zipEntries.stream().filter(fileHeader -> fileHeader.getFileName().equalsIgnoreCase(name1)).findFirst().orElse(null) != null;
        if (!flag) {
            String name2 = name1.replaceAll("/", "\\\\");
            flag = ((Container) getFMLContainer()).zipEntries.stream().filter(fileHeader -> fileHeader.getFileName().equalsIgnoreCase(name2)).findFirst().orElse(null) != null;
        }
        return flag;
    }

    public Set<String> getResourceDomains() {
        Set<String> set = Sets.<String>newHashSet();
        set.add("modularwarfare");
        return set;
    }

    /**
     * A {@link FMLModContainer} using an {@link MWResourcePack}
     */

    public static class Container extends FMLModContainer {

        private String packName;
        private IZip zipFile;
        private Set<IZipEntry> zipEntries;

        public Container(String className, ModCandidate container, Map<String, Object> modDescriptor, IZip zipFile, String packName) throws IOException {
            super(className, container, modDescriptor);
            this.zipFile = zipFile;
            this.packName = packName.substring(15);
            zipEntries = zipFile.getFileList();
        }

        @Override
        public Class<?> getCustomResourcePackClass() {
            return MWResourcePack.class;
        }
    }
}
