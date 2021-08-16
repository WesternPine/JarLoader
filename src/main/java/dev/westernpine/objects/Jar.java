package dev.westernpine.objects;

import java.net.URLClassLoader;

public class Jar {
	
	private URLClassLoader loader;
	
	private Class<?> clazz;
	
	public Jar(URLClassLoader loader, Class<?> clazz) {
		this.loader = loader;
		this.clazz = clazz;
	}
	
	public URLClassLoader getLoader() {
		return this.loader;
	}
	
	public Class<?> getLoadedClass() {
		return this.clazz;
	}

}
