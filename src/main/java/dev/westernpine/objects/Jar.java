package dev.westernpine.objects;

import dev.westernpine.objects.classloaders.JarClassLoader;

public class Jar {
	
	private JarClassLoader loader;
	
	private Class<?> clazz;
	
	public Jar(JarClassLoader loader, Class<?> clazz) {
		this.loader = loader;
		this.clazz = clazz;
	}
	
	/**
	 * Get the class loader for this instance.
	 * @return The class loader for this instance.
	 */
	public JarClassLoader getLoader() {
		return this.loader;
	}
	
	/**
	 * Get the initialized class.
	 * @return The initialized class.
	 */
	public Class<?> getLoadedClass() {
		return this.clazz;
	}

}
