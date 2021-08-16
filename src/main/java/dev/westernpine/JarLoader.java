package dev.westernpine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import dev.westernpine.exceptions.InvalidJarFileException;
import dev.westernpine.exceptions.ModuleLoadException;
import dev.westernpine.objects.Jar;
import dev.westernpine.objects.loaders.URLClassLoaderAccess;
import dev.westernpine.objects.maven.Dependency;
import dev.westernpine.objects.module.JavaModule;

public class JarLoader {
	
	private List<URLClassLoader> loaders;
	
	public JarLoader() {
		this.loaders = new ArrayList<>();
	}
	
	public void shutdown() {
		loaders.forEach(loader->{try {loader.close();} catch (IOException e){}});
		loaders.clear();
	}
	
	public Jar loadJar(File jarFile, String classToLoad) throws InvalidJarFileException, MalformedURLException, ClassNotFoundException {
		jarFile = new File(jarFile.getAbsolutePath());
		if(!jarFile.isFile() || jarFile.getName().equals(".jar") || !jarFile.getName().endsWith(".jar"))
			throw new InvalidJarFileException(jarFile);
		URL fileUrl = jarFile.toURI().toURL();
		URLClassLoader classLoader = new URLClassLoader(new URL[] {fileUrl});
		Class<?> clazz = null;
		try {
			clazz = classLoader.loadClass(classToLoad);
		} catch (Exception e) {
			try {
				classLoader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			throw new RuntimeException(e);
		}
		loaders.add(classLoader);
		return new Jar(classLoader, clazz);
	}
	
	/**
	 * @deprecated As of Java 9, the Java Module system now uses strong encapsulation, meaning a deep-reflective operation of 'setAccessible(boolean accessible)` on reflected objects is now prohibited. If you wish to enable dependency loading, start the JVM with the flag: '--add-opens=java.base/java.net=ALL-UNNAMED' to permit access.
	 */
	@Deprecated
	public void loadDependency(ClassLoader classLoader, Dependency dependency, File saveLocation) {
		saveLocation = new File(saveLocation.getAbsolutePath());
		//Download dependency if not exists.
		if(!saveLocation.exists()) {
			try {
				URL url = dependency.getUrl();
				try (InputStream is = url.openStream()) {
                    Files.copy(is, saveLocation.toPath());
                }
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (!saveLocation.exists()) {
            throw new RuntimeException("Unable to download dependency: " + dependency.toString());
        }
		/*
		 * Get access to controlling classloader, and add the URLs in.
		 * If the controlling classloader is NOT of type URL, then cancel operation basically.
		 */
		try {
			URLClassLoaderAccess.create(classLoader instanceof URLClassLoader ? (URLClassLoader) classLoader : null).addURL(saveLocation.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load dependency: " + saveLocation.toString(), e);
        }
	}
	
	public List<JavaModule> loadModules(List<File> jarFiles) {
		List<JavaModule> modules = jarFiles.stream()
				.map(file -> new File(file.getAbsolutePath()))
				.map(file -> {
					try {
						return new JavaModule(file);
					} catch (ModuleLoadException e) {
						e.printStackTrace();
					}
					return null;
				})
				.filter(module -> module != null)
				.collect(Collectors.toList());
		
		//sort and initialize modules.
		(modules = sortModules(modules))
		.forEach(module -> {
			try {
				module.load();
			} catch (ModuleLoadException le) {
				System.out.println("A module failed to load: " + le.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Failed to initialize module because of an uncaught exception! Skipping...");
			}
		});
		
		return modules; //Return loaded and unloaded modules!
	}
	

}
