package shoppy.publish;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Rotation;

import shoppy.Config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Publish {

	static Config config = new Config();
	
	public static void main(String[] args) throws IOException {
		// creates output directories
		Files.createDirectories(config.thumbDir);
		Files.createDirectories(config.largeDir);
		for (String categoryName : config.categoryIds) {
			Files.createDirectories(config.thumbDir.resolve(categoryName));
			Files.createDirectories(config.largeDir.resolve(categoryName));
		}
		
		// processes categories
		JsonObject categoriesJson = new JsonObject();
		for (int i = 0; i < config.categoryIds.length; i++) {
			processCategory(config.categoryIds[i], config.categoryTitles[i], categoriesJson);
		}
		
		// write catalog file
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		Gson gson = gsonBuilder.create();
		String r = "exports.categories = " + gson.toJson(categoriesJson) + ";\n";
		Files.write(config.catalogFile, r.getBytes(StandardCharsets.UTF_8));
	}

	static void processCategory(String categoryId, String categoryTitle, JsonObject categoriesJson) throws IOException {
		System.out.println("cat " + categoryId);
		JsonObject categoryJson = new JsonObject();
		categoryJson.addProperty("r", categoryId);
		categoryJson.addProperty("name", categoryTitle);
		categoryJson.add("items", new JsonArray());
		categoriesJson.add(categoryId, categoryJson);
		Path categoryDir = config.originalDir.resolve(categoryId);
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(categoryDir)) {
            for (Path path : directoryStream) {
                processItem(categoryJson, path);
            }
        }
	}

	private static void processItem(JsonObject categoryJson, Path articlePath) throws IOException {
		String articleName = articlePath.getFileName().toString();
		System.out.println(articleName);
		String baseName = articleName.substring(0, articleName.length() - 4);
		boolean isGroup = baseName.charAt(baseName.length() - 1) == 'G';
		int price = isGroup ? 4 : 2;
		JsonObject item = new JsonObject();
		item.addProperty("r", categoryJson.get("r").getAsString() + "/" + baseName);
		item.addProperty("p", price);
		categoryJson.get("items").getAsJsonArray().add(item);
		processPhotos(articlePath, !isGroup);
	}

	private static void processPhotos(Path articlePath, boolean rotate) throws IOException {
		// https://github.com/rkalla/imgscalr
		// http://stackoverflow.com/questions/1625137/image-resize-quality-java
		
		Path relPath = config.originalDir.relativize(articlePath); // category + photo name
		BufferedImage originalImage = ImageIO.read(articlePath.toFile());
		
		BufferedImage thumbImage = Scalr.resize(originalImage, 270);
		if (rotate) thumbImage = Scalr.rotate(thumbImage, Rotation.CW_270);
		ImageIO.write(thumbImage, "jpg", config.thumbDir.resolve(relPath).toFile());
		
		BufferedImage largeImage = Scalr.resize(originalImage, 600);
		if (rotate) largeImage = Scalr.rotate(largeImage, Rotation.CW_270);
		Graphics2D gc = largeImage.createGraphics();
		gc.setColor(Color.red);
		gc.setFont(new Font("Arial", Font.BOLD, 36));
		gc.drawString("SPECIMEN", 20, 40);
		ImageIO.write(largeImage, "jpg", config.largeDir.resolve(relPath).toFile());
	}
	
}
