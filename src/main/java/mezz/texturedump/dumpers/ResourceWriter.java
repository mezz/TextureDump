package mezz.texturedump.dumpers;

import mezz.texturedump.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.StartupMessageManager;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.List;

public class ResourceWriter {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void writeFiles(String name, File outputFolder, File mipmapsDir, List<File> textureImageFiles, List<File> textureInfoJsFiles, File modStatsFile, File resourceDir, int mipmapLevels) throws IOException {
        StartupMessageManager.addModMessage("Writing TextureMap resources files");

        for (int level = 0; level < mipmapLevels; level++) {
            File textureInfoJsFile = textureInfoJsFiles.get(level);
            File textureImageFile = textureImageFiles.get(level);

            StartupMessageManager.addModMessage("Mipmap Level " + level);

            String webPage = getResourceAsString("page.html")
                    .replaceAll("\\[statisticsFile]", modStatsFile.getAbsolutePath())
                    .replaceAll("\\[textureImage]", textureImageFile.getAbsolutePath())
                    .replaceAll("\\[textureInfo]", textureInfoJsFile.getAbsolutePath())
                    .replaceAll("\\[resourceDir]", resourceDir.getAbsolutePath());

            final File htmlFile;
            if (level == 0) {
                htmlFile = new File(outputFolder, name + ".html");
            } else {
                htmlFile = new File(mipmapsDir, name + "_mipmap_" + level + ".html");
            }
            FileWriter htmlFileWriter = new FileWriter(htmlFile);
            htmlFileWriter.write(webPage);
            htmlFileWriter.close();

            LOGGER.info("Exported html to: {}", htmlFile.getAbsolutePath());
        }
    }

    public static void writeResources(File resourceDir) throws IOException {
        writeFileFromResource(resourceDir, "fastdom.min.js");
        writeFileFromResource(resourceDir, "texturedump.js");
        writeFileFromResource(resourceDir, "texturedump.css");
        writeFileFromResource(resourceDir, "texturedump.backgrounds.css");
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        final Resource resource = resourceManager.getResource(new ResourceLocation(Constants.MOD_ID, "bg.png"));
        final InputStream inputStream = resource.getInputStream();
        IOUtils.copy(inputStream, new FileOutputStream(new File(resourceDir, "bg.png")));
    }

    private static void writeFileFromResource(File outputFolder, String s) throws IOException {
        FileWriter fileWriter;
        fileWriter = new FileWriter(new File(outputFolder, s));
        fileWriter.write(getResourceAsString(s));
        fileWriter.close();
    }

    private static String getResourceAsString(String resourceName) throws IOException {
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        final Resource resource = resourceManager.getResource(new ResourceLocation(Constants.MOD_ID, resourceName));
        final InputStream inputStream = resource.getInputStream();
        StringWriter writer = new StringWriter();
        IOUtils.copy(inputStream, writer, Charset.defaultCharset());
        String string = writer.toString();
        inputStream.close();
        return string;
    }
}
