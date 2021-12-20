package mezz.texturedump;

import mezz.texturedump.dumpers.ResourceWriter;
import mezz.texturedump.dumpers.ModStatsDumper;
import mezz.texturedump.dumpers.TextureImageDumper;
import mezz.texturedump.dumpers.TextureInfoDumper;
import mezz.texturedump.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.resource.ISelectiveResourceReloadListener;
import net.minecraftforge.resource.VanillaResourceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Mod(value = Constants.MOD_ID)
public class TextureDump {
	private boolean mainMenuOpened = false;

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
		Minecraft minecraft = Minecraft.getInstance();
		IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) minecraft.getResourceManager();
		reloadableResourceManager.addReloadListener((ISelectiveResourceReloadListener) (resourceManager, resourcePredicate) -> {
			if (resourcePredicate.test(VanillaResourceType.TEXTURES)) {
				if (mainMenuOpened) { // only reload when the player requests it in-game
					try {
						dumpTextureMaps();
					} catch (IOException e) {
						Log.error("Failed to dump texture maps with error.", e);
					}
				}
			}
		});
	}

	private void onMainMenuOpen(GuiOpenEvent event) {
		if (!mainMenuOpened && event.getGui() instanceof MainMenuScreen) {
			mainMenuOpened = true;
			try {
				dumpTextureMaps();
			} catch (IOException e) {
				Log.error("Failed to dump texture maps with error.", e);
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

			for (Map.Entry<ResourceLocation, Texture> entry : textureManager.mapTextureObjects.entrySet()) {
				Texture textureObject = entry.getValue();
				if (textureObject instanceof AtlasTexture) {
					String name = entry.getKey().toString().replace(':', '_').replace('/', '_');
					dumpTextureMap((AtlasTexture) textureObject, name, outputFolder, mipmapsDir, resourceDir, modStatsDir, texturesDir, textureInfoDir);
				}
			}
		} catch (IOException e) {
			Log.error("Failed to dump texture maps.", e);
		}
	}

	private static void dumpTextureMap(AtlasTexture map, String name, Path outputFolder, Path mipmapsDir, Path resourceDir, Path modStatsDir, Path texturesDir, Path textureInfoDir) {
		try {
			ModStatsDumper modStatsDumper = new ModStatsDumper();
			Path modStatsFile = modStatsDumper.saveModStats(name, map, modStatsDir);

			List<Path> textureImageJsFiles = TextureImageDumper.saveGlTextures(name, map.getGlTextureId(), texturesDir);
			int mipmapLevels = textureImageJsFiles.size();
			List<Path> textureInfoFiles = TextureInfoDumper.saveTextureInfoDataFiles(name, map, mipmapLevels, textureInfoDir);

			ResourceWriter.writeFiles(name, outputFolder, mipmapsDir, textureImageJsFiles, textureInfoFiles, modStatsFile, resourceDir, mipmapLevels);
		} catch (IOException e) {
			Log.error(String.format("Failed to dump texture map: %s.", name), e);
		}
	}

	public static Path createSubDirectory(Path outputFolder, String subfolderName) throws IOException {
		Path subfolder = outputFolder.resolve(subfolderName);
		return Files.createDirectories(subfolder);
	}
}
