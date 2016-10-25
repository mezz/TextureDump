package mezz.texturedump.dumpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
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
import org.apache.commons.io.IOUtils;

public class TextureInfoDumper {
	public static void saveTextureInfo(String name, TextureMap map, int mipmapLevels, File outputFolder) throws IOException {
		Set<String> animatedTextures = map.listAnimatedSprites.stream()
				.map(TextureAtlasSprite::getIconName)
				.collect(Collectors.toSet());

		for (int level = 0; level <= mipmapLevels; level++) {
			final String filename = name + "_mipmap_" + level;
			File output = new File(outputFolder, filename + ".html");
			StringWriter out = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(out);
			jsonWriter.setIndent("    ");

			jsonWriter.beginArray();
			{
				for (TextureAtlasSprite sprite : map.mapUploadedSprites.values()) {
					String iconName = sprite.getIconName();
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

			IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
			final IResource resource = resourceManager.getResource(new ResourceLocation(TextureDump.MODID, "page.html"));
			final InputStream inputStream = resource.getInputStream();
			StringWriter writer = new StringWriter();
			IOUtils.copy(inputStream, writer, Charset.defaultCharset());
			String webPage = writer.toString();
			webPage = webPage.replaceFirst("\\[textureData\\]", out.toString());
			webPage = webPage.replaceFirst("\\[textureName\\]", filename);

			FileWriter fileWriter = new FileWriter(output);
			fileWriter.write(webPage);
			fileWriter.close();

			inputStream.close();

			Log.info("Exported html to: {}", output.getAbsolutePath());
		}
	}
}
