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

import dev.westernpine.exceptions.InvalidJarFileException;
import dev.westernpine.exceptions.ModuleLoadException;
import dev.westernpine.objects.loaders.ModuleClassLoader;

public class JavaModule {
	
	public static final String MODULE_JSON_FILENAME = "module.json";
	
	private File file;
	
	private URL fileUrl;
	
	private String name;
	
	private String main;
	
	private String version;
	
	private String[] softDepends;
	
	private String[] depends;
	
	private ModuleClassLoader classLoader;
	
	private Object instance;
	
	public Consumer<JavaModule> onLoad = module -> {};
	
	public Consumer<JavaModule> onUnload = module -> {};
	
	public JavaModule(File file) throws ModuleLoadException {
		try {
			if(!(file.isFile() && !file.getName().equals(".jar") && file.getName().endsWith(".jar"))) {
				throw new InvalidJarFileException(file);
			}
			fileUrl = file.toURI().toURL();
			this.file = file;
		} catch (MalformedURLException | InvalidJarFileException e) {
			e.printStackTrace();
			throw new ModuleLoadException("Unable to parse File URL: " + file.getName());
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
						throw new ModuleLoadException("Unable to parse " + MODULE_JSON_FILENAME + " for jar: " + file.getName());
					}
				}
			}
		} catch (IOException | URISyntaxException e) {
			throw new ModuleLoadException("Unable to load jar file contents: " + file.getName());
		}
		if(Objects.isNull(name)) {
			throw new ModuleLoadException("Unable to find required \"name\" json member in " + MODULE_JSON_FILENAME + " for jar: " + file.getName());
		}
		if(Objects.isNull(main)) {
			throw new ModuleLoadException("Unable to find required \"main\" json member in " + MODULE_JSON_FILENAME + " for jar: " + file.getName());
		}
		if(Objects.isNull(version)) {
			throw new ModuleLoadException("Unable to find required \"version\" json member in " + MODULE_JSON_FILENAME + " for jar: " + file.getName());
		}
	}
	
	public File getFile() {
		return this.file;
	}
	
	public URL getFileURL() {
		return this.fileUrl;
	}
	
	public String getName() {
		return this.name;
	}
	
	public String getMain() {
		return this.main;
	}
	
	public String getVersion() {
		return this.version;
	}
	
	public String[] getSoftDepends() {
		return this.softDepends;
	}
	
	public String[] getDepends() {
		return this.depends;
	}
	
	public Object getInstance() {
		return this.instance;
	}
	
	@SuppressWarnings("resource")
	public void load() throws ModuleLoadException {
		this.classLoader = new ModuleClassLoader().addUrl(fileUrl).addToClassloaders();
		try {
			Class<?> clazz = this.classLoader.loadClass(main);
			try {
				instance = clazz.getDeclaredConstructor(JavaModule.class).newInstance(this);
			} catch (NoSuchMethodException e1) {
				try {
					instance = clazz.getDeclaredConstructor().newInstance();
				} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e2) {
					e2.printStackTrace();
					throw new ModuleLoadException(e2.getMessage());
				}
			} catch (Exception e2) {
				e2.printStackTrace();
				throw new ModuleLoadException(e2.getMessage());
			}
		} catch (ClassNotFoundException e) {
			throw new ModuleLoadException(e.getMessage());
		}
		
		this.onLoad.accept(this);
	}
	
	public void unload() throws IOException {
		this.onUnload.accept(this);
		this.instance = null;
		this.classLoader.close();
	}
	
	public boolean isLoaded() {
		return this.instance != null;
	}
	
	public List<String> getAllDependencies() {
		List<String> dependencies = new ArrayList<>();
		dependencies.addAll(Arrays.asList(depends));
		dependencies.addAll(Arrays.asList(softDepends));
		return dependencies;
	}
	
	public boolean isDependency(String name) {
		return List.of(softDepends).contains(name) || List.of(depends).contains(name);
	}

}









