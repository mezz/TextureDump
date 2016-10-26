package mezz.texturedump.dumpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.stream.JsonWriter;
import mezz.texturedump.TextureDump;
import mezz.texturedump.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.ProgressManager;
import org.apache.commons.io.IOUtils;

public class TextureInfoDumper {
	public static void saveTextureInfo(String name, TextureMap map, int mipmapLevels, File outputFolder) {
		Set<String> animatedTextures = map.listAnimatedSprites.stream()
				.map(TextureAtlasSprite::getIconName)
				.collect(Collectors.toSet());

		ProgressManager.ProgressBar progressBar = ProgressManager.push("Dumping TextureMap info to file", mipmapLevels + 1);

		for (int level = 0; level <= mipmapLevels; level++) {
			final String filename = name + "_mipmap_" + level;
			File output = new File(outputFolder, filename + ".html");
			progressBar.step(filename);

			StringWriter out = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(out);
			jsonWriter.setIndent("    ");

			Collection<TextureAtlasSprite> values = map.mapUploadedSprites.values();
			ProgressManager.ProgressBar progressBar2 = ProgressManager.push("Mipmap Level " + level, values.size());
			try {
				jsonWriter.beginArray();
				{
					for (TextureAtlasSprite sprite : values) {
						String iconName = sprite.getIconName();
						progressBar2.step(iconName);
						boolean animated = animatedTextures.contains(iconName);
						jsonWriter.beginObject()
								.name("name").value(iconName)
								.name("animated").value(animated)
								.name("x").value(sprite.getOriginX() / (1 << level))
								.name("y").value(sprite.getOriginY() / (1 << level))
								.name("width").value(sprite.getIconWidth() / (1 << level))
								.name("height").value(sprite.getIconHeight() / (1 << level))
								.endObject();
					}
				}
				jsonWriter.endArray();
				jsonWriter.close();
				out.close();

				String webPage = getResourceAsString("page.html");
				String textureJson = out.toString();
				webPage = webPage.replaceFirst("\\[textureData\\]", textureJson);
				webPage = webPage.replaceFirst("\\[textureName\\]", filename);

				FileWriter fileWriter = new FileWriter(output);
				fileWriter.write(webPage);
				fileWriter.close();

				writeFileFromResource(outputFolder, "fastdom.min.js");
				writeFileFromResource(outputFolder, "texturedump.js");
				writeFileFromResource(outputFolder, "texturedump.css");
				writeFileFromResource(outputFolder, "texturedump.backgrounds.css");

				Log.info("Exported html to: {}", output.getAbsolutePath());
			} catch (IOException e) {
				Log.error("Failed to save texture info.", e);
			}
			ProgressManager.pop(progressBar2);
		}

		ProgressManager.pop(progressBar);
	}

	private static void writeFileFromResource(File outputFolder, String s) throws IOException {
		FileWriter fileWriter;
		fileWriter = new FileWriter(new File(outputFolder, s));
		fileWriter.write(getResourceAsString(s));
		fileWriter.close();
	}

	private static String getResourceAsString(String resourceName) throws IOException {
		IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
		final IResource resource = resourceManager.getResource(new ResourceLocation(TextureDump.MODID, resourceName));
		final InputStream inputStream = resource.getInputStream();
		StringWriter writer = new StringWriter();
		IOUtils.copy(inputStream, writer, Charset.defaultCharset());
		String webPage = writer.toString();
		inputStream.close();
		return webPage;
	}
}
