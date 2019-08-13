package net.benmclean.wolf3d;

import com.albert.wolf3d.core.io.file.VswapFileReader;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        DownloadShareware.WOLF3DShareware();
        if (!Files.exists(Paths.get("output")))
            Files.createDirectory(Paths.get("output"));
        VswapFileReader.main(new String[] {"WOLF3D/VSWAP.WL1", "palettes/Wolf3d.pal", "output"});
    }
}
