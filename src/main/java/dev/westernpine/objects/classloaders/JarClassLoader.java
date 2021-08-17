package dev.westernpine.objects.classloaders;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import dev.westernpine.exceptions.InvalidJarFileException;

public class JarClassLoader extends URLClassLoader {

	static {ClassLoader.registerAsParallelCapable();}

	private static final Set<JarClassLoader> loaders = new CopyOnWriteArraySet<>();
	
	/**
	 * Get all the saved loaders that try to load classes off each other.
	 * @return
	 */
	public static Set<JarClassLoader> getLoaders() {
		return loaders;
	}	
	
	/**
	 * Make a new URLClassLoader that lets you add URLs of files to load classes from.
	 * @param urls The Default URLs to use.
	 */
	public JarClassLoader(URL[] urls) {
		super(urls);
	}	
	
	/**
	 * Make a new URLClassLoader that lets you add URLs of files to load classes from.
	 */
	public JarClassLoader() {
		super(new URL[] {});
	}
	
	/**
	 * Save this loader to the list of loaders to load classes from.
	 * @return This same object.
	 */
	public JarClassLoader save() {
		loaders.add(this);
		return this;
	}
	
	/**
	 * Unsave this loader to prevent other loaders from loading classes from it.
	 * @return This same object.
	 */
	public JarClassLoader unsave() {
		loaders.remove(this);
		return this;
	}
	
	/**
	 * Add a file to the list of URLs of files to read classes from.
	 * @param jarFile The file to add.
	 * @return This same object.
	 * @throws InvalidJarFileException If the jar file is invalid.
	 * @throws MalformedURLException If the URL of the jar file is invalid.
	 */
	public JarClassLoader addFile(File jarFile) throws InvalidJarFileException, MalformedURLException {
		jarFile = new File(jarFile.getAbsolutePath());
		if(!jarFile.isFile() || jarFile.getName().equals(".jar") || !jarFile.getName().endsWith(".jar"))
			throw new InvalidJarFileException(jarFile);
		super.addURL(jarFile.toURI().toURL());
		return this;
	}
	
	/**
	 * Add a file URL to the urls to load classes from.
	 */
	public void addURL(URL url) {
		super.addURL(url);
	}

	/**
	 * Remove this loader from the list of other loaders and close the URLClassLoader to prevent further loading of classes.
	 */
	@Override
	public void close() throws IOException {
		loaders.remove(this);
		super.close();
	}
	
	/**
	 * Try to load a class using this loader, or other saved loaders if this is a saved loader.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClass0(name, resolve, loaders.contains(this));
	}
	
	/*
	 * Try to load class from current jar specified, and from other jar loaders if checkOther is true.
	 */
	private Class<?> loadClass0(String name, boolean resolve, boolean checkOther) throws ClassNotFoundException {
		//Try and load class from super.
		try {return super.loadClass(name, resolve);} catch (ClassNotFoundException ignored) {}
		//Try to load class from the super of remembered class loaders.
		if (checkOther) {
			for (JarClassLoader loader : loaders) {
				if (loader != this) {
					try {return loader.loadClass0(name, resolve, false);} catch (ClassNotFoundException ignored) {}
				}
			}
		}
		//If we cant find it out of all the loaded jars, then throw exception.
		throw new ClassNotFoundException(name);
	}

}
