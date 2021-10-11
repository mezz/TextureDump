package mezz.texturedump.dumpers;

import com.google.gson.stream.JsonWriter;
import mezz.texturedump.TextureDump;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.StartupMessageManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TextureInfoDumper {
	public static List<File> saveTextureInfoDataFiles(String name, AtlasTexture map, int mipmapLevels, File outputFolder) throws IOException {
		Set<ResourceLocation> animatedTextures = map.listAnimatedSprites.stream()
				.map(TextureAtlasSprite::getName)
				.collect(Collectors.toSet());

		StartupMessageManager.addModMessage("Dumping TextureMap info to file");

		List<File> dataFiles = new ArrayList<>();
		for (int level = 0; level < mipmapLevels; level++) {
			final String filename = name + "_mipmap_" + level;
			File dataFile = new File(outputFolder, filename + ".js");

			StringWriter out = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(out);
			jsonWriter.setIndent("    ");

			Collection<TextureAtlasSprite> values = map.mapUploadedSprites.values();
			StartupMessageManager.addModMessage("Mipmap Level " + level);

			jsonWriter.beginArray();
			{
				for (TextureAtlasSprite sprite : values) {
					ResourceLocation iconName = sprite.getName();
					boolean animated = animatedTextures.contains(iconName);
					jsonWriter.beginObject()
							.name("name").value(iconName.toString())
							.name("animated").value(animated)
							.name("x").value(sprite.x / (1L << level))
							.name("y").value(sprite.y / (1L << level))
							.name("width").value(sprite.getWidth() / (1L << level))
							.name("height").value(sprite.getHeight() / (1L << level))
							.endObject();
				}
			}
			jsonWriter.endArray();
			jsonWriter.close();
			out.close();

			FileWriter fileWriter;
			fileWriter = new FileWriter(dataFile);
			fileWriter.write("var textureData = \n//Start of Data\n" + out);
			fileWriter.close();

			dataFiles.add(dataFile);
		}
		return dataFiles;
	}
}
