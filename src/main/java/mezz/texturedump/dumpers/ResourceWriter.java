package mezz.texturedump.dumpers;

import mezz.texturedump.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.fml.StartupMessageManager;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ResourceWriter {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void writeFiles(String name, Path outputFolder, Path mipmapsDir, List<Path> textureImageFiles, List<Path> textureInfoJsFiles, Path modStatsFile, Path resourceDir, int mipmapLevels) throws IOException {
        StartupMessageManager.addModMessage("Writing TextureMap resources files");

        for (int level = 0; level < mipmapLevels; level++) {
            Path textureInfoJsFile = textureInfoJsFiles.get(level);
            Path textureImageFile = textureImageFiles.get(level);

            StartupMessageManager.addModMessage("Mipmap Level " + level);

            String webPage = getResourceAsString("page.html")
                    .replaceAll("\\[statisticsFile]", fileToRelativeHtmlPath(modStatsFile, outputFolder))
                    .replaceAll("\\[textureImage]", fileToRelativeHtmlPath(textureImageFile, outputFolder))
                    .replaceAll("\\[textureInfo]", fileToRelativeHtmlPath(textureInfoJsFile, outputFolder))
                    .replaceAll("\\[resourceDir]", fileToRelativeHtmlPath(resourceDir, outputFolder));

            final Path htmlFile;
            if (level == 0) {
                htmlFile = outputFolder.resolve(name + ".html");
            } else {
                htmlFile = mipmapsDir.resolve(name + "_mipmap_" + level + ".html");
            }
            FileWriter htmlFileWriter = new FileWriter(htmlFile.toFile());
            htmlFileWriter.write(webPage);
            htmlFileWriter.close();

            LOGGER.info("Exported html to: {}", htmlFile.toString());
        }
    }

    private static String fileToRelativeHtmlPath(Path file, Path outputFolder) {
        String path = outputFolder.relativize(file).toString();
        return FilenameUtils.separatorsToUnix(path);
    }

    public static void writeResources(Path resourceDir) throws IOException {
        writeFileFromResource(resourceDir, "fastdom.min.js");
        writeFileFromResource(resourceDir, "texturedump.js");
        writeFileFromResource(resourceDir, "texturedump.css");
        writeFileFromResource(resourceDir, "texturedump.backgrounds.css");
        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        final Resource resource = resourceManager.getResource(new ResourceLocation(Constants.MOD_ID, "bg.png"));
        final InputStream inputStream = resource.getInputStream();
        final OutputStream outputStream = Files.newOutputStream(resourceDir.resolve("bg.png"));
        IOUtils.copy(inputStream, outputStream);
    }

    private static void writeFileFromResource(Path outputFolder, String s) throws IOException {
        FileWriter fileWriter = new FileWriter(outputFolder.resolve(s).toFile());
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
