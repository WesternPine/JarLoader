package dev.westernpine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import dev.westernpine.exceptions.InvalidJarFileException;
import dev.westernpine.exceptions.ModuleLoadException;
import dev.westernpine.objects.DependencyMapper;
import dev.westernpine.objects.Jar;
import dev.westernpine.objects.classloaders.JarClassLoader;
import dev.westernpine.objects.classloaders.WrappedURLClassLoader;
import dev.westernpine.objects.maven.Dependency;
import dev.westernpine.objects.module.JavaModule;

/**
 * JarLoader is an overlysimplified ClassLoader and dependency utility.
 * 
 * Special thanks to these users/orgs and their code for guidence on this utility:
 *  - https://github.com/slimjar
 *  - https://github.com/lucko
 *  - https://github.com/VelocityPowered
 *  
 * @author WesternPine
 *
 */
public abstract class JarLoader {
	
	/**
	 * Wrap a URLClassLoader with this wrapper class to add URL's into the class loader.
	 * @param loader The class loader to wrap.
	 * @return A wrapped form of the class loader to inject URL's into.
	 * @deprecated Only works with ClassLoaders of type URLClassLoader! As of Java 9, the Java Module system now uses strong encapsulation, meaning a deep-reflective operation of 'setAccessible(boolean accessible)` on reflected objects is now prohibited. If you wish to enable dependency loading, start the JVM with the flag: '--add-opens=java.base/java.net=ALL-UNNAMED' to permit access.
	 */
	@Deprecated
	public static WrappedURLClassLoader wrapLoader(ClassLoader loader) {
		return WrappedURLClassLoader.create(loader);
	}
	
	/**
	 * Load jars into the current class loader. (Only works with URL class loaders!)
	 * @param loader The class loader to use.
	 * @param dependency The dependency to import.
	 * @param saveLocation The file location to look for the jar at, or to save to.
	 * @deprecated Only works with ClassLoaders of type URLClassLoader! As of Java 9, the Java Module system now uses strong encapsulation, meaning a deep-reflective operation of 'setAccessible(boolean accessible)` on reflected objects is now prohibited. If you wish to enable dependency loading, start the JVM with the flag: '--add-opens=java.base/java.net=ALL-UNNAMED' to permit access.
	 */
	@Deprecated
	public static void loadDependency(ClassLoader loader, Dependency dependency, File saveLocation) {
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
			wrapLoader(loader).addURL(saveLocation.toURI().toURL());
        } catch (Exception e) {
            throw new RuntimeException("Unable to load dependency: " + saveLocation.toString(), e);
        }
	}
	
	/**
	 * Creates a new URLClassLoader with the abiliy to add URLs.
	 * @param save Whether to save this loader to a list of other loaders to look for classes in. (If loading multiple jars that need to access classes of eachother)
	 * @return A new URLClassLoader with the abiliy to add URLs.
	 */
	public static JarClassLoader newLoader(boolean save) {
		JarClassLoader loader = new JarClassLoader();
		return save ? loader.save() : loader;
	}
	
	/**
	 * Creates a new URLClassLoader with the abiliy to add URLs.
	 * @param jarFile The default file to add to the URL loader.
	 * @param save Whether to save this loader to a list of other loaders to look for classes in. (If loading multiple jars that need to access classes of eachother)
	 * @return A new URLClassLoader with the abiliy to add URLs.
	 */
	public static JarClassLoader newLoader(File jarFile, boolean save) throws InvalidJarFileException, MalformedURLException, IOException {
		JarClassLoader loader = new JarClassLoader();
		try {
			loader.addFile(jarFile);
		} catch (InvalidJarFileException | MalformedURLException e) {
			loader.close();
			throw e;
		}
		return save ? loader.save() : loader;
	}
	
	/**
	 * Create a new instance of a Jar object that contains the new class loader used, and the sp[ecified class loaded.
	 * @param jarFile The jar file to load.
	 * @param classToLoad The class to initialize.
	 * @param save Whether to save this loader to a list of other loaders to look for classes in. (If loading multiple jars that need to access classes of eachother)
	 * @return A new instance of a Jar object that contains the new class loader used, and the sp[ecified class loaded.
	 * @throws InvalidJarFileException Thrown if the specified jar file is not a valid jar file.
	 * @throws IOException Thrown if the URL of the jarfile is invalid, or if a problem occured while closing the class loader.
	 */
	public static Jar newLoaderWithClass(File jarFile, String classToLoad, boolean save) throws InvalidJarFileException, IOException {
		jarFile = new File(jarFile.getAbsolutePath());
		if(!jarFile.isFile() || jarFile.getName().equals(".jar") || !jarFile.getName().endsWith(".jar"))
			throw new InvalidJarFileException(jarFile);
		URL fileUrl = jarFile.toURI().toURL();
		JarClassLoader loader = new JarClassLoader(new URL[] {fileUrl});
		Class<?> clazz = null;
		try {
			clazz = loader.loadClass(classToLoad);
		} catch (Exception e) {
			loader.close();
			throw new RuntimeException(e);
		}
		if(save)
			loader.save();
		return new Jar(loader, clazz);
	}
	
	/**
	 * This is a complete dependency system. Give this method a list of files to load. It will make a new class loader for each jar, load the classes based off the module.json resource file (MUST contain "name", "main", and "version" string values, with optional "depends" and "softdepends" json string array values that either requires other modules to be present to initialize, or isn't required to start.), and map the dependencies before initializing them.
	 * @param jarFiles The files to initialize as modules. This method filters out files that aren't jar files.
	 * @return An object containing the mapped (Duplicates removed, Chained/Self-Referenced dependencies removed, Modules with required dependencies missing removed, and Mapped in order of execution.) modules ready for initialization.
	 */
	public static DependencyMapper loadModules(List<File> jarFiles) {
		return new DependencyMapper(new LinkedList<>(jarFiles.stream()
				.map(file -> new File(file.getAbsolutePath()))
				.filter(file -> file.isFile() && !file.getName().equals(".jar") && file.getName().endsWith(".jar"))
				.map(file -> {
					try {
						return new JavaModule(file);
					} catch (ModuleLoadException e1) {
						e1.printStackTrace();
					}
					return null;
				})
				.filter(module -> module != null)
				.collect(Collectors.toList())));
	}
}