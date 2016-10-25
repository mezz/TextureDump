package mezz.texturedump;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.IntBuffer;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.stream.JsonWriter;
import mezz.texturedump.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.IOUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

@Mod(modid = TextureDump.MODID, name = "Texture Dump", version = TextureDump.VERSION, clientSideOnly = true)
public class TextureDump {
	public static final String MODID = "texturedump";
	public static final String VERSION = "1.2";

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void postTextureStitch(TextureStitchEvent.Post event) {
		TextureMap map = event.getMap();
		if (map.mapUploadedSprites.size() <= 1) {
			return; // skip the first texture stitch event that only contains the missing texture
		}
		String name = map.getBasePath().replace('/', '_');
		int mip = map.getMipmapLevels();
		File outputFolder = new File("texture_dump");
		if (!outputFolder.exists()) {
			if (!outputFolder.mkdir()) {
				Log.error("Failed to create directory " + outputFolder);
				return;
			}
		}
		saveGlTexture(name, map.getGlTextureId(), mip, outputFolder);
		try {
			saveTextureInfo(name, map, mip, outputFolder);
		} catch (IOException e) {
			Log.error("Failed to save texture info.", e);
		}
		try {
			saveModStats(name, map, outputFolder);
		} catch (IOException e) {
			Log.error("Failed to save mod statistics info.", e);
		}
	}

	public static void saveModStats(String name, TextureMap map, File outputFolder) throws IOException {
		Map<String, Long> modPixelCounts = map.mapUploadedSprites.values().stream()
				.collect(Collectors.groupingBy(
						sprite -> new ResourceLocation(sprite.getIconName()).getResourceDomain(),
						Collectors.summingLong(sprite -> sprite.getIconWidth() * sprite.getIconHeight()))
				);

		final long totalPixels = modPixelCounts.values().stream().mapToLong(longValue -> longValue).sum();

		Map<String, ModContainer> modContainersForLowercaseIds = new HashMap<>();
		for (Map.Entry<String, ModContainer> modEntry : Loader.instance().getIndexedModList().entrySet()) {
			String lowercaseId = modEntry.getKey().toLowerCase(Locale.ENGLISH);
			modContainersForLowercaseIds.put(lowercaseId, modEntry.getValue());
		}

		final String filename = name + "_mod_statistics";
		File output = new File(outputFolder, filename + ".json");
		FileWriter fileWriter = new FileWriter(output);
		JsonWriter jsonWriter = new JsonWriter(fileWriter);
		jsonWriter.setIndent("    ");
		jsonWriter.beginArray();
		{
			List<Map.Entry<String, Long>> sortedEntries = modPixelCounts.entrySet().stream()
					.sorted(Collections.reverseOrder(Comparator.comparing(Map.Entry::getValue)))
					.collect(Collectors.toList());
			for (Map.Entry<String, Long> modPixels : sortedEntries) {
				String modId = modPixels.getKey();

				long pixelCount = modPixels.getValue();
				jsonWriter.beginObject()
						.name("modId").value(modId)
						.name("pixelCount").value(pixelCount)
						.name("percentOfTextureMap").value(pixelCount * 100f / totalPixels);

				ModContainer modContainer = modContainersForLowercaseIds.get(modId.toLowerCase(Locale.ENGLISH));
				if (modContainer != null) {
					ModMetadata metadata = modContainer.getMetadata();
					jsonWriter.name("modName").value(metadata.name)
							.name("url").value(metadata.url);

					jsonWriter.name("authors").beginArray();
					for (String author : metadata.authorList) {
						jsonWriter.value(author);
					}
					jsonWriter.endArray();
				}

				jsonWriter.endObject();
			}
		}
		jsonWriter.endArray();
		jsonWriter.close();
		fileWriter.close();
	}

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
			final IResource resource = resourceManager.getResource(new ResourceLocation(MODID, "page.html"));
			final InputStream inputStream = resource.getInputStream();
			StringWriter writer = new StringWriter();
			IOUtils.copy(inputStream, writer, Charset.defaultCharset());
			String webPage = writer.toString();
			webPage = webPage.replaceFirst("\\[textureData\\]", out.toString());
			webPage = webPage.replaceFirst("\\[textureName\\]", filename.toString());

			FileWriter fileWriter = new FileWriter(output);
			fileWriter.write(webPage);
			fileWriter.close();


			inputStream.close();

			Log.info("Exported html to: {}", output.getAbsolutePath());
		}
	}

	public static void saveGlTexture(String name, int textureId, int mipmapLevels, File outputFolder) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

		for (int level = 0; level <= mipmapLevels; level++) {
			int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_WIDTH);
			int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_HEIGHT);
			int size = width * height;

			BufferedImage bufferedimage = new BufferedImage(width, height, 2);
			File output = new File(outputFolder, name + "_mipmap_" + level + ".png");
			IntBuffer buffer = BufferUtils.createIntBuffer(size);
			int[] data = new int[size];

			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
			buffer.get(data);
			bufferedimage.setRGB(0, 0, width, height, data, 0, width);

			try {
				ImageIO.write(bufferedimage, "png", output);
				Log.info("Exported png to: {}", output.getAbsolutePath());
			} catch (IOException ioexception) {
				Log.info("Unable to write: ", ioexception);
			}
		}
	}
}
