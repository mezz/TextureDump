package mezz.texturedump.dumpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.stream.JsonWriter;
import mezz.texturedump.util.Log;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.ProgressManager;

public class ModStatsDumper {
	private final Map<String, ModContainer> modContainersForLowercaseIds = new HashMap<>();

	public ModStatsDumper() {
		for (Map.Entry<String, ModContainer> modEntry : Loader.instance().getIndexedModList().entrySet()) {
			String lowercaseId = modEntry.getKey().toLowerCase(Locale.ENGLISH);
			modContainersForLowercaseIds.put(lowercaseId, modEntry.getValue());
		}
	}

	public void saveModStats(String name, TextureMap map, File outputFolder) {
		Map<String, Long> modPixelCounts = map.mapUploadedSprites.values().stream()
				.collect(Collectors.groupingBy(
						sprite -> new ResourceLocation(sprite.getIconName()).getResourceDomain(),
						Collectors.summingLong(sprite -> sprite.getIconWidth() * sprite.getIconHeight()))
				);

		final long totalPixels = modPixelCounts.values().stream().mapToLong(longValue -> longValue).sum();

		final String filename = name + "_mod_statistics";
		File output = new File(outputFolder, filename + ".js");

		List<Map.Entry<String, Long>> sortedEntries = modPixelCounts.entrySet().stream()
				.sorted(Collections.reverseOrder(Comparator.comparing(Map.Entry::getValue)))
				.collect(Collectors.toList());

		ProgressManager.ProgressBar progressBar = ProgressManager.push("Dumping Mod TextureMap Statistics", sortedEntries.size());

		try {
			FileWriter fileWriter = new FileWriter(output);
			fileWriter.write("var modStatistics = \n//Start of Data\n");
			JsonWriter jsonWriter = new JsonWriter(fileWriter);
			jsonWriter.setIndent("    ");
			jsonWriter.beginArray();
			{
				for (Map.Entry<String, Long> modPixels : sortedEntries) {
					String resourceDomain = modPixels.getKey();
					progressBar.step(resourceDomain);

					long pixelCount = modPixels.getValue();
					ModMetadata metadata = getModMetadata(resourceDomain);

					jsonWriter.beginObject()
							.name("resourceDomain").value(resourceDomain)
							.name("pixelCount").value(pixelCount)
							.name("percentOfTextureMap").value(pixelCount * 100f / totalPixels)
							.name("modName").value(metadata.name)
							.name("url").value(metadata.url);

					jsonWriter.name("authors").beginArray();
					for (String author : metadata.authorList) {
						jsonWriter.value(author);
					}
					jsonWriter.endArray();

					jsonWriter.endObject();
				}

			}
			jsonWriter.endArray();
			jsonWriter.close();
			fileWriter.close();

			Log.info("Saved mod statistics to {}.", output.getAbsoluteFile());
		} catch (IOException e) {
			Log.error("Failed to save mod statistics info.", e);
		}

		ProgressManager.pop(progressBar);
	}

	private ModMetadata getModMetadata(String resourceDomain) {
		ModContainer modContainer = modContainersForLowercaseIds.get(resourceDomain.toLowerCase(Locale.ENGLISH));
		if (modContainer == null) {
			ModMetadata modMetadata = new ModMetadata();
			modMetadata.name = resourceDomain.equals("minecraft") ? "Minecraft" : "unknown";
			return modMetadata;
		} else {
			return modContainer.getMetadata();
		}
	}
}
