package dev.westernpine.objects.classloaders;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import dev.westernpine.JarLoader;
import dev.westernpine.exceptions.InvalidJarFileException;

public class JarClassLoader extends URLClassLoader {

	static {ClassLoader.registerAsParallelCapable();}
	
	private JarLoader jarLoader;
	
	/**
	 * Make a new URLClassLoader that lets you add URLs of files to load classes from.
	 * @param urls The Default URLs to use.
	 */
	public JarClassLoader(JarLoader jarLoader, URL[] urls) {
		super(urls);
		this.jarLoader = jarLoader;
	}	
	
	/**
	 * Make a new URLClassLoader that lets you add URLs of files to load classes from.
	 */
	public JarClassLoader(JarLoader jarLoader) {
		super(new URL[] {});
		this.jarLoader = jarLoader;
	}
	
	/**
	 * Check if this loader is isolated.
	 * @return True if tis loader is isolated.
	 */
	public boolean isIsolated() {
		return this.jarLoader.isIsolated(this);
	}
	
	/**
	 * Isolate this loader to prevent other loaders from loading classes from it, and this loader from loading classes from others.
	 * @return This same object.
	 */
	public JarClassLoader isolate() {
		this.jarLoader.isolate(this);
		return this;
	}
	
	/**
	 * Integrate this loader to the list of loaders to load classes from.
	 * @return This same object.
	 */
	public JarClassLoader integrate() {
		this.jarLoader.integrate(this);
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
		isolate();
		super.close();
	}
	
	/**
	 * Try to load a class using this loader, or other saved loaders if this is a saved loader.
	 */
	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClass0(name, resolve, !isIsolated());
	}
	
	/*
	 * Try to load class from current jar specified, and from other jar loaders if checkOther is true.
	 */
	private Class<?> loadClass0(String name, boolean resolve, boolean checkOther) throws ClassNotFoundException {
		//Try and load class from super.
		try {return super.loadClass(name, resolve);} catch (ClassNotFoundException ignored) {}
		//Try to load class from the super of remembered class loaders.
		if (checkOther) {
			for (JarClassLoader loader : this.jarLoader.getLoaders()) {
				if (loader != this) {
					try {return loader.loadClass0(name, resolve, false);} catch (ClassNotFoundException ignored) {}
				}
			}
		}
		//If we cant find it out of all the loaded jars, then throw exception.
		throw new ClassNotFoundException(name);
	}

}
