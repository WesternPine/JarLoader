package dev.westernpine.objects.loaders;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ModuleClassLoader extends URLClassLoader {

	private static final Set<ModuleClassLoader> loaders = new CopyOnWriteArraySet<>();

	static {
		ClassLoader.registerAsParallelCapable();
	}

	public ModuleClassLoader(URL[] urls) {
		super(urls);
	}

	public ModuleClassLoader() {
		super(new URL[] {});
	}

	public ModuleClassLoader addToClassloaders() {
		loaders.add(this);
		return this;
	}

	public ModuleClassLoader addUrl(URL url) {
		addURL(url);
		return this;
	}

	@Override
	public void close() throws IOException {
		loaders.remove(this);
		super.close();
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		return loadClass0(name, resolve, true);
	}
	
	/*
	 * Try to lead class from current jar specified, or from other jar modules. (AKA dependencies)
	 */
	private Class<?> loadClass0(String name, boolean resolve, boolean checkOther) throws ClassNotFoundException {
		try {
			return super.loadClass(name, resolve);
		} catch (ClassNotFoundException ignored) {
			// Ignored: we'll try others
		}

		if (checkOther) {
			for (ModuleClassLoader loader : loaders) {
				if (loader != this) {
					try {
						return loader.loadClass0(name, resolve, false);
					} catch (ClassNotFoundException ignored) {
						// We're trying others, safe to ignore
					}
				}
			}
		}

		throw new ClassNotFoundException(name);
	}
}