package mezz.texturedump;

import java.io.File;
import java.util.Map;
import java.util.function.Consumer;

import mezz.texturedump.dumpers.ModStatsDumper;
import mezz.texturedump.dumpers.TextureImageDumper;
import mezz.texturedump.dumpers.TextureInfoDumper;
import mezz.texturedump.util.Log;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
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

@Mod(value = TextureDump.MOD_ID)
public class TextureDump {
	public static final String MOD_ID = "texturedump";
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
		reloadableResourceManager.addReloadListener(resourceManager -> {
			if (mainMenuOpened) { // only reload when the player requests it in-game
				dumpTextureMaps();
			}
		});
	}

	private void onMainMenuOpen(GuiOpenEvent event) {
		if (!mainMenuOpened && event.getGui() instanceof GuiMainMenu) {
			mainMenuOpened = true;
			dumpTextureMaps();
		}
	}

	private static void dumpTextureMaps() {
		TextureManager textureManager = Minecraft.getInstance().getTextureManager();
		for (Map.Entry<ResourceLocation, ITextureObject> entry : textureManager.mapTextureObjects.entrySet()) {
			ITextureObject textureObject = entry.getValue();
			if (textureObject instanceof TextureMap) {
				String name = entry.getKey().toString().replace(':', '_').replace('/', '_');
				dumpTextureMap((TextureMap) textureObject, name);
			}
		}
	}

	private static void dumpTextureMap(TextureMap map, String name) {
		int mip = map.getMipmapLevels();
		File outputFolder = new File("texture_dump");
		if (!outputFolder.exists()) {
			if (!outputFolder.mkdir()) {
				Log.error("Failed to create directory " + outputFolder);
				return;
			}
		}

		TextureImageDumper.saveGlTexture(name, map.getGlTextureId(), mip, outputFolder);
		TextureInfoDumper.saveTextureInfo(name, map, mip, outputFolder);

		ModStatsDumper modStatsDumper = new ModStatsDumper();
		modStatsDumper.saveModStats(name, map, outputFolder);
	}

}
