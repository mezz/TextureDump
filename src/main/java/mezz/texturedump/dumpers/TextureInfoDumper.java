package mezz.texturedump.dumpers;

import com.google.gson.stream.JsonWriter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.StartupMessageManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TextureInfoDumper {
	public static List<File> saveTextureInfoDataFiles(String name, TextureAtlas map, int mipmapLevels, File outputFolder) throws IOException {
		StartupMessageManager.addModMessage("Dumping TextureMap info to file");

		List<File> dataFiles = new ArrayList<>();
		for (int level = 0; level < mipmapLevels; level++) {
			final String filename = name + "_mipmap_" + level;
			File dataFile = new File(outputFolder, filename + ".js");

			StringWriter out = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(out);
			jsonWriter.setIndent("    ");

			Collection<TextureAtlasSprite> values = map.texturesByName.values();
			StartupMessageManager.addModMessage("Mipmap Level " + level);

			jsonWriter.beginArray();
			{
				for (TextureAtlasSprite sprite : values) {
					ResourceLocation iconName = sprite.getName();
					boolean animated = (sprite.getAnimationTicker() != null);
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
