package mezz.texturedump.dumpers;

import com.google.gson.stream.JsonWriter;
import mezz.texturedump.TextureDump;
import mezz.texturedump.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.StartupMessageManager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class TextureInfoDumper {
	public static void saveTextureInfo(String name, AtlasTexture map, int mipmapLevels, File outputFolder) {
		Set<ResourceLocation> animatedTextures = map.listAnimatedSprites.stream()
				.map(TextureAtlasSprite::getName)
				.collect(Collectors.toSet());

		StartupMessageManager.addModMessage("Dumping TextureMap info to file");

		for (int i = 0; i <= mipmapLevels; i++) {
			final int level = i;
			final String filename = name + "_mipmap_" + level;
			final String statisticsFile = name + "_mod_statistics";
			File htmlFile = new File(outputFolder, filename + ".html");
			File dataFile = new File(outputFolder, filename + ".js");

			StringWriter out = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(out);
			jsonWriter.setIndent("    ");

			Collection<TextureAtlasSprite> values = map.mapUploadedSprites.values();
			StartupMessageManager.addModMessage("Mipmap Level " + level);

			try {
				jsonWriter.beginArray();
				{
					for (TextureAtlasSprite sprite : values) {
						ResourceLocation iconName = sprite.getName();
						boolean animated = animatedTextures.contains(iconName);
						jsonWriter.beginObject()
							.name("name").value(iconName.toString())
							.name("animated").value(animated)
							.name("x").value(sprite.x / (1 << level))
							.name("y").value(sprite.y / (1 << level))
							.name("width").value(sprite.getWidth() / (1 << level))
							.name("height").value(sprite.getHeight() / (1 << level))
							.endObject();
					}
				}
				jsonWriter.endArray();
				jsonWriter.close();
				out.close();

				FileWriter fileWriter;
				fileWriter = new FileWriter(dataFile);
				fileWriter.write("var textureData = \n//Start of Data\n" + out.toString());
				fileWriter.close();

				String webPage = getResourceAsString("page.html");
				webPage = webPage.replaceAll("\\[statisticsFile]", statisticsFile);
				webPage = webPage.replaceAll("\\[textureName]", filename);

				fileWriter = new FileWriter(htmlFile);
				fileWriter.write(webPage);
				fileWriter.close();

				Log.info("Exported html to: {}", htmlFile.getAbsolutePath());
			} catch (IOException e) {
				Log.error("Failed to save texture info.", e);
			}
		}

		try {
			writeFileFromResource(outputFolder, "fastdom.min.js");
			writeFileFromResource(outputFolder, "texturedump.js");
			writeFileFromResource(outputFolder, "texturedump.css");
			writeFileFromResource(outputFolder, "texturedump.backgrounds.css");
			IResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
			final IResource resource = resourceManager.getResource(new ResourceLocation(TextureDump.MOD_ID, "bg.png"));
			final InputStream inputStream = resource.getInputStream();
			IOUtils.copy(inputStream, new FileOutputStream(new File(outputFolder, "bg.png")));
		} catch (IOException e) {
			Log.error("Failed to save additional page files.", e);
		}
	}

	private static void writeFileFromResource(File outputFolder, String s) throws IOException {
		FileWriter fileWriter;
		fileWriter = new FileWriter(new File(outputFolder, s));
		fileWriter.write(getResourceAsString(s));
		fileWriter.close();
	}

	private static String getResourceAsString(String resourceName) throws IOException {
		IResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
		final IResource resource = resourceManager.getResource(new ResourceLocation(TextureDump.MOD_ID, resourceName));
		final InputStream inputStream = resource.getInputStream();
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, Charset.defaultCharset());
		String webPage = writer.toString();
		inputStream.close();
		return webPage;
	}
}
