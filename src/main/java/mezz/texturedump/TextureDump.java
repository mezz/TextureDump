package mezz.texturedump;

import java.io.File;
import java.util.Map;

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
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.javafmlmod.FMLModLoadingContext;

@Mod(value = "texturedump")
public class TextureDump {
	public static final String MODID = "texturedump";
	public static final String VERSION = "@VERSION@";
	private boolean mainMenuOpened = false;
	
	public TextureDump() {
        FMLModLoadingContext.get().getModEventBus().addListener(this::onLoadComplete);

        // Register ourselves for server, registry and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
	}

	private void onLoadComplete(final FMLLoadCompleteEvent event) {
//		if (event.get == Side.CLIENT) {
			// Reload when resources change
			Minecraft minecraft = Minecraft.getInstance();
			IReloadableResourceManager reloadableResourceManager = (IReloadableResourceManager) minecraft.getResourceManager();
			reloadableResourceManager.addReloadListener(resourceManager -> {
				if (mainMenuOpened) { // only reload when the player requests it in-game
					dumpTextureMaps();
				}
			});
//		}
	}

	@SubscribeEvent
	public void onMainMenuOpen(GuiOpenEvent event) {
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
