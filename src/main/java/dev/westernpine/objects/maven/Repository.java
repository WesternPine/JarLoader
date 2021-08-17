package dev.westernpine.objects.maven;

import java.net.MalformedURLException;
import java.net.URL;

public class Repository {
	
	/**
	 * The default repository for all dependencies.
	 */
	public static final Repository MAVEN_CENTRAL_REPOSITORY = new Repository("Maven Central", "https://repo1.maven.org/maven2/");
	
	/**
	 * The name of the repository.
	 */
	public final String name;
	
	/**
	 * The URL of the repository.
	 */
	public final String url;
	
	/**
	 * A representation of a repository.
	 * @param name The name of the repository.
	 * @param url The URL of the repository.
	 */
	public Repository(String name, String url) {
		this.name = name;
		this.url = url;
	}
	
	/**
	 * 
	 * @return A URL representation of this repository.
	 */
	public URL getAsURL() {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

}
