package shoppy.order;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import shoppy.Config;
import shoppy.model.Category;
import shoppy.model.Item;
import shoppy.model.Order;


public class PrepareOrder {

	static Config config = new Config();
	
	static Path orderFile = config.root.resolve("db-orders-fixed.json");
	static Path catalogFile = config.catalogFile;
	
	static Path sourceFolder = config.originalDir;
	static Path destinationFolder = config.root.resolve("commande");
	
	static boolean copyFiles = true;

	public static void main(String[] args) throws IOException {
		
		if (!Files.isRegularFile(orderFile)) throw new IOException("missing file " + orderFile);
		if (!Files.isRegularFile(catalogFile)) throw new IOException("missing file " + catalogFile);
		if (!Files.isDirectory(sourceFolder)) throw new IOException("missing directory " + sourceFolder);
		if (!Files.isDirectory(destinationFolder)) throw new IOException("missing directory " + destinationFolder);
		
		// read files:
		Order[] orders = readJsonFile(orderFile, Order[].class, false);
		LinkedHashMap<String, Category> catalog = readJsonFile(catalogFile, new TypeToken<LinkedHashMap<String, Category>>(){}.getType(), true);
		
		// collect item prices:
		Map<String, Double> prices = new LinkedHashMap<>();
		for (Entry<String, Category> cat : catalog.entrySet()) {
			for (Item item : cat.getValue().items) {
				prices.put(item.r, item.p);
			}
		}
		
		// count orders by item ref:
		Map<String, Integer> numberByItemRef = new LinkedHashMap<>();
		int totalItems = 0;
		for (Order order : orders) {
			for (String itemRef : order.items) {
				int n = numberByItemRef.get(itemRef) == null ? 0 : numberByItemRef.get(itemRef);
				numberByItemRef.put(itemRef, n + 1);
				totalItems++;
			}
		}
		
		// output result:
		System.out.println("Commandes: " + orders.length);
		double pTotal = 0;
		for (Order order : orders) {
			for (String itemRef : order.items) {
				Double pItem = prices.get(itemRef);
				if (pItem == null) System.out.println("??? " + itemRef);
				pTotal += pItem;
			}
		}
		System.out.println("Photos: " + totalItems);
		System.out.println("Photos types: " + numberByItemRef.size());
		System.out.println("Recette: " + pTotal);
		System.out.println();
		
		double seuil = 3.0;
		
		System.out.println("Photos groupe:");
		int nbGroup = 0;
		for (Entry<String, Integer> e : numberByItemRef.entrySet()) {
			if (prices.get(e.getKey()) > seuil) {
				System.out.println("  " + e.getKey() + "   " + e.getValue());
				nbGroup += e.getValue();
				copy(e.getKey(), "group/" + makeName(e.getKey()) + "x" + e.getValue());
			}
		}
		System.out.println("  total: " + nbGroup);
		System.out.println();
		
		System.out.println("Photos individuelles:");
		int nbIndiv = 0;
		for (Entry<String, Integer> e : numberByItemRef.entrySet()) {
			if (prices.get(e.getKey()) < seuil && e.getValue() > 1) {
				System.out.println("  " + e.getKey() + "   " + e.getValue());
				nbIndiv += e.getValue();
				for (int i = 1; i <= e.getValue(); i++) {
					copy(e.getKey(), "indiv/"+makeName(e.getKey()) + "-" + i);
				}
			}
		}
		for (Entry<String, Integer> e : numberByItemRef.entrySet()) {
			if (prices.get(e.getKey()) < seuil && e.getValue() == 1) {
				System.out.println("  " + e.getKey() + "   " + e.getValue());
				nbIndiv += e.getValue();
				copy(e.getKey(), "indiv/"+makeName(e.getKey()));
			}
		}
		System.out.println("  total: " + nbIndiv);
		System.out.println();
		
	}
	
	private static String makeName(String key) {
		return key.replace('/', '-').replace("JUD_", "");
	}
	
	private static void copy(String n1, String n2) throws IOException {
		Path p1 = sourceFolder.resolve(n1+".JPG");
		Path p2 = destinationFolder.resolve(n2+".JPG");
		System.out.println("copy " + p1 + " -> " + p2);
		if (copyFiles) Files.copy(p1, p2);
	}

	private static <T> T readJsonFile(Path fileName, Type  type, boolean skip) throws IOException {
		byte[] bytes = Files.readAllBytes(fileName);
		String s = new String(bytes, StandardCharsets.UTF_8);
		if (skip) s = s.substring(20, s.length() - 2);
		return new Gson().fromJson(new StringReader(s), type);
	}
	
}
