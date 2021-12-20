package mezz.texturedump;

import mezz.texturedump.dumpers.ResourceWriter;
import mezz.texturedump.dumpers.ModStatsDumper;
import mezz.texturedump.dumpers.TextureImageDumper;
import mezz.texturedump.dumpers.TextureInfoDumper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mod(value = Constants.MOD_ID)
public class TextureDump {
	private static final Logger LOGGER = LogManager.getLogger();
	private boolean titleScreenOpened = false;

	public TextureDump() {
		DistExecutor.runWhenOn(Dist.CLIENT, ()->()-> {
			IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
			addListener(modEventBus, FMLLoadCompleteEvent.class, (event) -> this.onLoadComplete());
			addListener(MinecraftForge.EVENT_BUS, GuiOpenEvent.class, this::onMainMenuOpen);
		});
	}

	private static <T extends Event> void addListener(IEventBus eventBus, Class<T> eventType, Consumer<T> listener) {
		eventBus.addListener(EventPriority.NORMAL, false, eventType, listener);
	}

	private void onLoadComplete() {
		// Reload when resources change
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		modEventBus.addListener(EventPriority.NORMAL, false, RegisterClientReloadListenersEvent.class, registerReloadListenerEvent -> {
			registerReloadListenerEvent.registerReloadListener((ResourceManagerReloadListener) (resourceManager) -> {
				if (titleScreenOpened) { // only reload when the player requests it in-game
					try {
						dumpTextureMaps();
					} catch (IOException e) {
						LOGGER.error("Failed to dump texture maps with error.", e);
					}
				}
			});
		});
	}

	private void onMainMenuOpen(GuiOpenEvent event) {
		if (!titleScreenOpened && event.getGui() instanceof TitleScreen) {
			titleScreenOpened = true;
			try {
				dumpTextureMaps();
			} catch (IOException e) {
				LOGGER.error("Failed to dump texture maps with error.", e);
			}
		}
	}

	private static void dumpTextureMaps() throws IOException {
		Path outputFolder = Paths.get("texture_dump");
		outputFolder = Files.createDirectories(outputFolder);

		TextureManager textureManager = Minecraft.getInstance().getTextureManager();

		try {
			Path mipmapsDir = createSubDirectory(outputFolder, "mipmaps");
			Path resourceDir = createSubDirectory(outputFolder, "resources");
			Path modStatsDir = createSubDirectory(outputFolder, "modStats");
			Path texturesDir = createSubDirectory(outputFolder, "textures");
			Path textureInfoDir = createSubDirectory(outputFolder, "textureInfo");
			ResourceWriter.writeResources(resourceDir);

			for (Map.Entry<ResourceLocation, AbstractTexture> entry : textureManager.byPath.entrySet()) {
				AbstractTexture textureObject = entry.getValue();
				if (textureObject instanceof TextureAtlas) {
					String name = entry.getKey().toString().replace(':', '_').replace('/', '_');
					dumpTextureMap((TextureAtlas) textureObject, name, outputFolder, mipmapsDir, resourceDir, modStatsDir, texturesDir, textureInfoDir);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to dump texture maps.", e);
		}
	}

	private static void dumpTextureMap(TextureAtlas map, String name, Path outputFolder, Path mipmapsDir, Path resourceDir, Path modStatsDir, Path texturesDir, Path textureInfoDir) {
		try {
			ModStatsDumper modStatsDumper = new ModStatsDumper();
			Path modStatsFile = modStatsDumper.saveModStats(name, map, modStatsDir);

			List<Path> textureImageJsFiles = TextureImageDumper.saveGlTextures(name, map.getId(), texturesDir);
			int mipmapLevels = textureImageJsFiles.size();
			List<Path> textureInfoFiles = TextureInfoDumper.saveTextureInfoDataFiles(name, map, mipmapLevels, textureInfoDir);

			ResourceWriter.writeFiles(name, outputFolder, mipmapsDir, textureImageJsFiles, textureInfoFiles, modStatsFile, resourceDir, mipmapLevels);
		} catch (IOException e) {
			LOGGER.error(String.format("Failed to dump texture map: %s.", name), e);
		}
	}

	public static Path createSubDirectory(Path outputFolder, String subfolderName) throws IOException {
		Path subfolder = outputFolder.resolve(subfolderName);
		return Files.createDirectories(subfolder);
	}
}
