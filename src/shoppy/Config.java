package shoppy;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Config {
	
	public Path root = Paths.get("E:\\nosave\\SitePhotosDojo\\20XX");
	
	public Path originalDir =  root.resolve("original");
	public Path thumbDir =  root.resolve("site/thumb");
	public Path largeDir =  root.resolve("site/large");
	
	public Path catalogFile = root.resolve("catalog.js");
	
	public String[] categoryIds = {"ba" ,"da", "de", "m1", "m2", "ad"};
	public String[] categoryTitles = {"Baby-do", "Débutant mardi", "Débutant mercredi", "Moyen 1", "Moyen 2", "Adultes"};
	
}
