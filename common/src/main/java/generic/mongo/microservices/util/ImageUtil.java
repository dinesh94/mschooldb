package generic.mongo.microservices.util;

import java.awt.color.CMMException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.imgscalr.Scalr.Mode;

public class ImageUtil {
	
	private final static String GIF_EXTENSION_TEXT = "gif";
	private final static String PNG_EXTENSION_TEXT = "png";

	
	public static File createThumbnailByImageScalr(File imageFile, String fileFormat, int height, int width) throws IllegalArgumentException, IOException {
		String imageName = imageFile.getName();
		if(fileFormat.equalsIgnoreCase(GIF_EXTENSION_TEXT)) {
			imageName = imageName.substring(0, imageName.indexOf('.')+1) +PNG_EXTENSION_TEXT;
			fileFormat = PNG_EXTENSION_TEXT;
		}

		// create a temp file
		File thumbNailFile = File.createTempFile("temp-file", ".tmp");
		BufferedImage img = readImage(imageFile);
		BufferedImage thumbImg = Scalr.resize(img, Method.QUALITY, Mode.AUTOMATIC, width, height, Scalr.OP_ANTIALIAS);
		ImageIO.write(thumbImg, fileFormat, thumbNailFile);
		return thumbNailFile;
	}
	
	public static BufferedImage readImage(File file) throws IOException
	{
		BufferedImage img = null;
		try {
			img = ImageIO.read(file);
		}catch(CMMException ex) {
			ex.printStackTrace();
		}catch(IIOException ex) {
			ex.printStackTrace();
		}
		catch(IllegalArgumentException ex) {
			ex.printStackTrace();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
		return img;
	}
}
