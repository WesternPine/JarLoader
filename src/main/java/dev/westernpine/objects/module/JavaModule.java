package dev.westernpine.objects.module;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.StreamSupport;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.westernpine.JarLoader;
import dev.westernpine.exceptions.InvalidJarFileException;
import dev.westernpine.exceptions.ModuleLoadException;
import dev.westernpine.objects.classloaders.JarClassLoader;

public class JavaModule {
	
	public static final String MODULE_JSON_FILENAME = "module.json";
	
	private File file;
	
	private URL fileUrl;
	
	private String name;
	
	private String main;
	
	private String version;
	
	private String[] softDepends;
	
	private String[] depends;
	
	private JarClassLoader loader;
	
	private Object instance;
	
	public Consumer<JavaModule> onLoad = module -> {};
	
	public Consumer<JavaModule> onUnload = module -> {};
	
	/**
	 * A representation of a jar file as an environment.
	 * @param file The file to load.
	 * @throws ModuleLoadException If any exception occurs while enabling a module, such as it being an invalid jar, missing module.json fields, and general inability to load a jar.
	 */
	public JavaModule(File file) throws ModuleLoadException {
		try {
			if(!(file.isFile() && !file.getName().equals(".jar") && file.getName().endsWith(".jar"))) {
				throw new InvalidJarFileException(file);
			}
			fileUrl = file.toURI().toURL();
			this.file = file;
		} catch (MalformedURLException | InvalidJarFileException e) {
			throw new ModuleLoadException("Unable to parse File URL: " + file.getName(), e);
		}
		
		
		try (JarInputStream in = new JarInputStream(new BufferedInputStream(Files.newInputStream(Path.of(fileUrl.toURI()))))) {
			JarEntry entry;
			while ((entry = in.getNextJarEntry()) != null) {
				if (entry.getName().equals(MODULE_JSON_FILENAME)) {
					try (Reader pluginInfoReader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
						JsonObject moduleJson = JsonParser.parseReader(pluginInfoReader).getAsJsonObject();
						this.name = Optional.ofNullable(moduleJson.get("name")).map(object -> object.isJsonNull() ? null : object.getAsString()).orElse(null);
						this.main = Optional.ofNullable(moduleJson.get("main")).map(object -> object.isJsonNull() ? null : object.getAsString()).orElse(null);
						this.version = Optional.ofNullable(moduleJson.get("version")).map(object -> object.isJsonNull() ? null : object.getAsString()).orElse(null);
						this.softDepends =  Optional.ofNullable(moduleJson.get("softdepends")).map(object -> object.isJsonNull() ? null : object.getAsJsonArray()).map(jsonArray -> StreamSupport.stream(jsonArray.spliterator(), false).map(jsonElement -> jsonElement.getAsJsonObject().getAsString()).toArray(String[]::new)).orElse(new String[] {});
						this.depends =  Optional.ofNullable(moduleJson.get("depends")).map(object -> object.isJsonNull() ? null : object.getAsJsonArray()).map(jsonArray -> StreamSupport.stream(jsonArray.spliterator(), false).map(jsonElement -> jsonElement.getAsJsonObject().getAsString()).toArray(String[]::new)).orElse(new String[] {});
						break;
					} catch (Exception e) {
						throw new ModuleLoadException("Unable to parse " + MODULE_JSON_FILENAME + " for jar: " + file.getName(), e);
					}
				}
			}
		} catch (IOException | URISyntaxException e) {
			throw new ModuleLoadException("Unable to load jar file contents: " + file.getName(), e);
		}
		if(Objects.isNull(name)) {
			throw new ModuleLoadException("Unable to find required \"name\" json member in " + MODULE_JSON_FILENAME + " for jar: " + file.getName(), new NullPointerException());
		}
		if(Objects.isNull(main)) {
			throw new ModuleLoadException("Unable to find required \"main\" json member in " + MODULE_JSON_FILENAME + " for jar: " + file.getName(), new NullPointerException());
		}
		if(Objects.isNull(version)) {
			throw new ModuleLoadException("Unable to find required \"version\" json member in " + MODULE_JSON_FILENAME + " for jar: " + file.getName(), new NullPointerException());
		}
	}
	
	/**
	 * 
	 * @return The jar file.
	 */
	public File getFile() {
		return this.file;
	}
	
	/**
	 * 
	 * @return The URL of the jar file.
	 */
	public URL getFileURL() {
		return this.fileUrl;
	}
	
	/**
	 * 
	 * @return The name of this module as specified in the module.json.
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * 
	 * @return The main class of this module as specified in the module.json.
	 */
	public String getMain() {
		return this.main;
	}
	
	/**
	 * 
	 * @return The version of this module as specified in the module.json.
	 */
	public String getVersion() {
		return this.version;
	}
	
	/**
	 * 
	 * @return Any dependencies optionally required for this module to start as specified in the module.json.
	 */
	public String[] getSoftDepends() {
		return this.softDepends;
	}
	
	/**
	 * 
	 * @return Any dependencies required for this module to start as specified in the module.json.
	 */
	public String[] getDepends() {
		return this.depends;
	}
	
	/**
	 * 
	 * @return The class loader used to load this jar.
	 */
	public JarClassLoader getLoader() {
		return this.loader;
	}
	
	/**
	 * 
	 * @return The instance of the main class.
	 */
	public Object getInstance() {
		return this.instance;
	}
	
	/**
	 * Loads this module via getting the constructor of the main class, and initializing it.
	 * @throws ModuleLoadException If an exception occured initializing the module.
	 */
	public void load() throws ModuleLoadException {
		this.loader = JarLoader.newLoader(true);
		loader.addURL(fileUrl);
		try {
			Class<?> clazz = this.loader.loadClass(main);
			try {
				instance = clazz.getDeclaredConstructor().newInstance();
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				e1.printStackTrace();
				throw new ModuleLoadException(e1.getMessage(), e1);
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new ModuleLoadException(e2.getMessage(), e2);
			}
		} catch (ClassNotFoundException e) {
			throw new ModuleLoadException(e.getMessage(), e);
		}
		this.onLoad.accept(this);
	}
	
	/**
	 * Attempts to unload the instance of this module by closing the class loader, and nullifying the instance.
	 * @throws IOException
	 */
	public void unload() throws IOException {
		this.onUnload.accept(this);
		this.instance = null;
		this.loader.close();
	}
	
	/**
	 * 
	 * @return True if the instance is loaded.
	 */
	public boolean isLoaded() {
		return this.instance != null;
	}
	
	/**
	 * 
	 * @return A list of all dependencies, soft or not.
	 */
	public List<String> getAllDependencies() {
		List<String> dependencies = new ArrayList<>();
		dependencies.addAll(Arrays.asList(depends));
		dependencies.addAll(Arrays.asList(softDepends));
		return dependencies;
	}
	
	/**
	 * Check if this module contains a dependency.
	 * @param name The module name to check.
	 * @return True if this module depends on the name specified.
	 */
	public boolean isDependency(String name) {
		return Arrays.asList(softDepends).contains(name) || Arrays.asList(depends).contains(name);
	}

}