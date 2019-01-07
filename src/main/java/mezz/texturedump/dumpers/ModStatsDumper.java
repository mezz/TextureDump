package mezz.texturedump.dumpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.stream.JsonWriter;

import mezz.texturedump.util.Log;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ProgressManager;
import net.minecraftforge.fml.language.IModInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;

public class ModStatsDumper {

	public void saveModStats(String name, TextureMap map, File outputFolder) {
		Map<String, Long> modPixelCounts = map.mapUploadedSprites.values().stream()
				.collect(Collectors.groupingBy(
						sprite -> sprite.getName().getNamespace(),
						Collectors.summingLong(sprite -> sprite.getWidth() * sprite.getHeight()))
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
					IModInfo metadata = getModMetadata(resourceDomain);

					jsonWriter.beginObject()
							.name("resourceDomain").value(resourceDomain)
							.name("pixelCount").value(pixelCount)
							.name("percentOfTextureMap").value(pixelCount * 100f / totalPixels)
							.name("modName").value(metadata.getDisplayName())
							.name("url").value("");

					jsonWriter.name("authors").beginArray();
/*					for (String author : metadata.get) {
						jsonWriter.value(author);
					}*/
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

	private IModInfo getModMetadata(String resourceDomain) {
		ModInfo mod = ModList.get().getMods().stream().filter(m -> m.getModId().equals(resourceDomain)).findFirst().orElse(null);
		if (mod == null) {
			throw new IllegalArgumentException();
		} else {
			return mod;
		}
	}
}
