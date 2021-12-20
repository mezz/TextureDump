package mezz.texturedump.dumpers;

import com.google.gson.stream.JsonWriter;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModStatsDumper {
	private static final Logger LOGGER = LogManager.getLogger();

	public Path saveModStats(String name, TextureAtlas map, Path modStatsDir) throws IOException {
		Map<String, Long> modPixelCounts = map.texturesByName.values().stream()
				.collect(Collectors.groupingBy(
						sprite -> sprite.getName().getNamespace(),
						Collectors.summingLong(sprite -> (long) sprite.getWidth() * sprite.getHeight()))
				);

		final long totalPixels = modPixelCounts.values().stream().mapToLong(longValue -> longValue).sum();

		final String filename = name + "_mod_statistics";
		Path output = modStatsDir.resolve(filename + ".js");

		List<Map.Entry<String, Long>> sortedEntries = modPixelCounts.entrySet().stream()
				.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
				.collect(Collectors.toList());

		StartupMessageManager.addModMessage("Dumping Mod TextureMap Statistics");
		FileWriter fileWriter = new FileWriter(output.toFile());
		fileWriter.write("var modStatistics = \n//Start of Data\n");
		JsonWriter jsonWriter = new JsonWriter(fileWriter);
		jsonWriter.setIndent("    ");
		jsonWriter.beginArray();
		{
			for (Map.Entry<String, Long> modPixels : sortedEntries) {
				String resourceDomain = modPixels.getKey();
				long pixelCount = modPixels.getValue();
				writeModStatisticsObject(jsonWriter, resourceDomain, pixelCount, totalPixels);
			}
		}
		jsonWriter.endArray();
		jsonWriter.close();
		fileWriter.close();

		LOGGER.info("Saved mod statistics to {}.", output.toString());
		return output;
	}

	private static void writeModStatisticsObject(JsonWriter jsonWriter, String resourceDomain, long pixelCount, long totalPixels) throws IOException {
		IModInfo modInfo = getModMetadata(resourceDomain);
		String modName = modInfo != null ? modInfo.getDisplayName() : "";

		jsonWriter.beginObject()
				.name("resourceDomain").value(resourceDomain)
				.name("pixelCount").value(pixelCount)
				.name("percentOfTextureMap").value(pixelCount * 100f / totalPixels)
				.name("modName").value(modName)
				.name("url").value(getModConfigValue(modInfo, "displayURL"))
				.name("issueTrackerUrl").value(getModConfigValue(modInfo, "issueTrackerURL"));

		jsonWriter.name("authors").beginArray();
		{
			String authors = getModConfigValue(modInfo, "authors");
			if (!authors.isEmpty()) {
				String[] authorList = authors.split(",");
				for (String author : authorList) {
					jsonWriter.value(author.trim());
				}
			}
		}
		jsonWriter.endArray();

		jsonWriter.endObject();
	}

	private static String getModConfigValue(@Nullable IModInfo modInfo, String key) {
		if (modInfo == null) {
			return "";
		}
		Map<String, Object> modConfig = modInfo.getModProperties();
		Object value = modConfig.getOrDefault(key, "");
		if (value instanceof String) {
			return (String) value;
		}
		return "";
	}

	@Nullable
	private static IModInfo getModMetadata(String resourceDomain) {
		ModList modList = ModList.get();
		IModFileInfo modFileInfo = modList.getModFileById(resourceDomain);
		if (modFileInfo == null) {
			return null;
		}
		return modFileInfo.getMods()
				.stream()
				.findFirst()
				.orElse(null);
	}
}
