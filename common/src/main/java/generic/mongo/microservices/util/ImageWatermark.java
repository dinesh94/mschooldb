package generic.mongo.microservices.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

/**
 * This program demonstrates how to add a watermark over an image using Java.
 * 
 * @author www.codejava.net
 * 
 */
public class ImageWatermark {

	public static File addTextWatermark(String text, InputStream inputStream) throws IOException {
		try {
			BufferedImage sourceImage = ImageIO.read(inputStream);
			Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();

			// initializes necessary graphic properties
			AlphaComposite alphaChannel = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.4f);
			g2d.setComposite(alphaChannel);
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font("Arial", Font.BOLD, 45));
			FontMetrics fontMetrics = g2d.getFontMetrics();
			Rectangle2D rect = fontMetrics.getStringBounds(text, g2d);

			// calculates the coordinate where the String is painted
			int centerX = (sourceImage.getWidth() - (int) rect.getWidth()) / 2;
			int centerY = sourceImage.getHeight() - (int) rect.getHeight();

			// rotates the coordinate by 90 degree counterclockwise
			AffineTransform at = new AffineTransform();
			at.rotate(-Math.PI / 2);
			// g2d.setTransform(at);

			// paints the textual watermark
			g2d.drawString(text, centerX, centerY);

			// create a temp file
			File temp = File.createTempFile("temp-file", ".tmp");

			ImageIO.write(sourceImage, "jpg", temp);
			g2d.dispose();

			return temp;

		} catch (IOException ex) {
			System.err.println(ex);
			throw ex;
		}
	}

	/**
	 * Embeds a textual watermark over a source image to produce a watermarked
	 * one.
	 * 
	 * @param text
	 *            The text to be embedded as watermark.
	 * @param sourceImageFile
	 *            The source image file.
	 * @param destImageFile
	 *            The output image file.
	 */
	public static void addTextWatermark(String text, File sourceImageFile,
			File destImageFile) {
		try {
			BufferedImage sourceImage = ImageIO.read(sourceImageFile);
			Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();

			// initializes necessary graphic properties
			AlphaComposite alphaChannel = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.4f);
			g2d.setComposite(alphaChannel);
			g2d.setColor(Color.WHITE);
			g2d.setFont(new Font("Arial", Font.BOLD, 45));
			FontMetrics fontMetrics = g2d.getFontMetrics();
			Rectangle2D rect = fontMetrics.getStringBounds(text, g2d);

			// calculates the coordinate where the String is painted
			int centerX = (sourceImage.getWidth() - (int) rect.getWidth()) / 2;
			int centerY = sourceImage.getHeight() - (int) rect.getHeight();

			// rotates the coordinate by 90 degree counterclockwise
			AffineTransform at = new AffineTransform();
			at.rotate(-Math.PI / 2);
			// g2d.setTransform(at);

			// paints the textual watermark
			g2d.drawString(text, centerX, centerY);

			ImageIO.write(sourceImage, "png", destImageFile);
			g2d.dispose();

		} catch (IOException ex) {
			System.err.println(ex);
		}
	}

	/**
	 * Embeds an image watermark over a source image to produce a watermarked
	 * one.
	 * 
	 * @param watermarkImageFile
	 *            The image file used as the watermark.
	 * @param sourceImageFile
	 *            The source image file.
	 * @param destImageFile
	 *            The output image file.
	 */
	public static void addImageWatermark(File watermarkImageFile,
			File sourceImageFile, File destImageFile) {
		try {
			BufferedImage sourceImage = ImageIO.read(sourceImageFile);
			BufferedImage watermarkImage = ImageIO.read(watermarkImageFile);

			// initializes necessary graphic properties
			Graphics2D g2d = (Graphics2D) sourceImage.getGraphics();
			AlphaComposite alphaChannel = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, 0.3f);
			g2d.setComposite(alphaChannel);

			// calculates the coordinate where the image is painted
			int topLeftX = (sourceImage.getWidth() - watermarkImage.getWidth()) / 2;
			int topLeftY = (sourceImage.getHeight() - watermarkImage.getHeight());

			// paints the image watermark
			g2d.drawImage(watermarkImage, topLeftX, topLeftY, null);

			ImageIO.write(sourceImage, "png", destImageFile);
			g2d.dispose();

		} catch (IOException ex) {
			System.err.println(ex);
		}
	}

	public static void main(String[] args) {
		File sourceImageFile = new File("d:\\watermark\\OrignalImage.png");
		File destImageFile = new File("d:\\watermark\\OrignalImage_watermarked.jpg");

		addTextWatermark("\u00a9kandapohe.com", sourceImageFile, destImageFile);
		
		try {
			File resized = ImageUtil.createThumbnailByImageScalr(destImageFile, "jpg", 600, 600);
			File customDir = new File("d:\\watermark\\resize_watermarked.jpg");
			FileUtils.copyFile(resized, customDir);
		}
		catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}