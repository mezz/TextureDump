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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(
		modid = TextureDump.MODID,
		name = "Texture Dump",
		version = TextureDump.VERSION,
		acceptedMinecraftVersions = "[1.10,1.12]",
		clientSideOnly = true
)
public class TextureDump {
	public static final String MODID = "texturedump";
	public static final String VERSION = "@VERSION@";
	private boolean dumped = false;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onMainMenuOpen(GuiOpenEvent event) {
		if (!dumped && event.getGui() instanceof GuiMainMenu) {
			dumped = true;
			TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
			for (Map.Entry<ResourceLocation, ITextureObject> entry : textureManager.mapTextureObjects.entrySet()) {
				ITextureObject textureObject = entry.getValue();
				if (textureObject instanceof TextureMap) {
					String name = entry.getKey().toString().replace(':', '_').replace('/', '_');
					dumpTextureMap((TextureMap) textureObject, name);
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	private void dumpTextureMap(TextureMap map, String name) {
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
