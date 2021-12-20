package mezz.texturedump.dumpers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.StartupMessageManager;

import mezz.texturedump.util.Log;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class TextureImageDumper {
	public static List<Path> saveGlTextures(String name, int textureId, Path texturesDir) throws IOException {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

		int parentTextureWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
		int parentTextureHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

		int minimumSize = Math.min(parentTextureWidth, parentTextureHeight);
		int mipmapLevels = MathHelper.log2(minimumSize);

		StartupMessageManager.addModMessage("Dumping TextureMap to file");
		List<Path> textureFiles = new ArrayList<>();
		for (int level = 0; level < mipmapLevels; level++) {
			int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_WIDTH);
			int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, level, GL11.GL_TEXTURE_HEIGHT);
			int size = width * height;

			if (width == 0 || height == 0) {
				return textureFiles;
			}

			BufferedImage bufferedimage = new BufferedImage(width, height, 2);
			Path output = texturesDir.resolve(name + "_mipmap_" + level + ".png");
			IntBuffer buffer = BufferUtils.createIntBuffer(size);
			int[] data = new int[size];

			GL11.glGetTexImage(GL11.GL_TEXTURE_2D, level, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
			buffer.get(data);
			bufferedimage.setRGB(0, 0, width, height, data, 0, width);

			ImageIO.write(bufferedimage, "png", output.toFile());
			Log.info("Exported png to: {}", output.toString());
			textureFiles.add(output);
		}
		return textureFiles;
	}
}
