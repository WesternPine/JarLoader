package dev.westernpine.objects.maven;

import java.net.MalformedURLException;
import java.net.URL;

public class Repository {
	
	public static final Repository MAVEN_CENTRAL_REPOSITORY = new Repository("Maven Central", "https://repo1.maven.org/maven2/");
	
	public final String name;
	
	public final String url;
	
	public Repository(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	public URL getAsURL() {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
