package mezz.texturedump.dumpers;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.progress.StartupProgressManager;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraft.client.renderer.texture.TextureMap;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.gson.stream.JsonWriter;
import mezz.texturedump.util.Log;

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

		StartupProgressManager.start("Dumping Mod TextureMap Statistics", sortedEntries.size(), progressBar -> {
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
						UnmodifiableConfig modConfig = metadata.getModConfig();

						jsonWriter.beginObject()
							.name("resourceDomain").value(resourceDomain)
							.name("pixelCount").value(pixelCount)
							.name("percentOfTextureMap").value(pixelCount * 100f / totalPixels)
							.name("modName").value(metadata.getDisplayName())
							.name("url").value(getModConfigValue(modConfig, "displayURL"))
							.name("issueTrackerUrl").value(getModConfigValue(modConfig, "issueTrackerURL"));

						jsonWriter.name("authors").beginArray();
						{
							String authors = getModConfigValue(modConfig, "authors");
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

				}
				jsonWriter.endArray();
				jsonWriter.close();
				fileWriter.close();

				Log.info("Saved mod statistics to {}.", output.getAbsoluteFile());
			} catch (IOException e) {
				Log.error("Failed to save mod statistics info.", e);
			}
		});
	}

	private static String getModConfigValue(UnmodifiableConfig modConfig, String key) {
		return modConfig.getOptional(key)
			.filter(value -> value instanceof String)
			.map(value -> (String) value)
			.orElse("");
	}

	private IModInfo getModMetadata(String resourceDomain) {
		ModList modList = ModList.get();
		List<ModInfo> mods = modList.getMods();
		ModInfo mod = mods.stream()
			.filter(m -> m.getModId().equals(resourceDomain))
			.findFirst()
			.orElse(null);
		if (mod == null) {
			throw new IllegalArgumentException();
		} else {
			return mod;
		}
	}
}
