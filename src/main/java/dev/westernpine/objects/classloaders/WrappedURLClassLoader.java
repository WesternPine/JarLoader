package dev.westernpine.objects.classloaders;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import sun.misc.Unsafe;

public abstract class WrappedURLClassLoader {
	
	private static final UnsupportedLoader UNSUPPORTED = new UnsupportedLoader();
	
	/**
	 * Create a wrapper for the specified class loader, to inject URLs into.
	 * @param classLoader The ClassLoader to wrap around and inject URLs into.
	 * @return A Wrapper of the given ClassLoader.
	 */
	public static WrappedURLClassLoader create(ClassLoader classLoader) {
		if(classLoader != null && classLoader instanceof URLClassLoader)
			if(classLoader instanceof JarClassLoader)
				return new JarLoader((URLClassLoader) classLoader);
			else if(ReflectiveLoader.ADD_URL_METHOD != null)
				return new ReflectiveLoader((URLClassLoader) classLoader);
			else if(UnsafeLoader.UNSAFE != null)
				return new UnsafeLoader((URLClassLoader) classLoader);
		//TODO: Instrumentation! :D
		return UNSUPPORTED;
	}
	
	private final URLClassLoader classLoader;
	
    private WrappedURLClassLoader(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    /**
     * 
     * @return The given ClassLoader.
     */
    public URLClassLoader getClassLoader() {
    	return this.classLoader;
    }
    
    /**
     * 
     * @return True if this Wrapper can inject URLs into the URLClassLoader.
     */
    public abstract boolean isSupported();
    
    /**
     * 
     * @param url Add a URL to the URL class loader to load classes from.
     */
    public abstract void addURL(URL url);
    
    /*
     * For any time it's unsupported.
     */
    private static class UnsupportedLoader extends WrappedURLClassLoader {
		protected UnsupportedLoader() {
			super(null);
		}
		@Override
		public boolean isSupported() {
			return false;
		}
		@Override
		public void addURL(URL url) {
			throw new UnsupportedOperationException();
		}
    }
    
    /*
     * Java 8-
     */
    private static class ReflectiveLoader extends WrappedURLClassLoader {
    	//We could have used `Class.forName("java.lang.Module");`, but with this way we can reuse a method we'e checking for anyways.
    	private static final Method ADD_URL_METHOD;
        static {
            Method method;
            try {
                method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
            } catch (ReflectiveOperationException e) {
            	method = null;
            }
            ADD_URL_METHOD = method;
        }
		private ReflectiveLoader(URLClassLoader classLoader) {
			super(classLoader);
			// TODO Auto-generated constructor stub
		}
		@Override
		public boolean isSupported() {
			return ADD_URL_METHOD != null;
		}
		@Override
		public void addURL(URL url) {
			try {
                ADD_URL_METHOD.invoke(super.classLoader, url);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
		}
    }
    
    /*
     * Java 9+
     */
    private static class UnsafeLoader extends WrappedURLClassLoader {
    	//We could have used `Class.forName("sun.misc.Unsafe");`, but with this way we can reuse an object we'e going to use anyways.
    	private static final Unsafe UNSAFE;
    	static {
    		Unsafe unsafe;
    		try {
    			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                unsafe = (Unsafe) unsafeField.get(null);
    		} catch (Throwable t) {
    			unsafe = null;
    		}
    		UNSAFE = unsafe;
    	}
        private final Collection<URL> unopenedURLs;
        private final Collection<URL> pathURLs;
		@SuppressWarnings("unchecked")
		private UnsafeLoader(URLClassLoader classLoader) {
			super(classLoader);
			Collection<URL> unopenedURLs;
            Collection<URL> pathURLs;
            try {
                Object ucp = getField(URLClassLoader.class, "ucp", classLoader);
                unopenedURLs = (Collection<URL>) getField(ucp.getClass(), "unopenedUrls", ucp);
                pathURLs = (Collection<URL>) getField(ucp.getClass(), "path", ucp);
            } catch (Throwable e) {
                unopenedURLs = null;
                pathURLs = null;
            }
            this.unopenedURLs = unopenedURLs;
            this.pathURLs = pathURLs;
		}
		@Override
		public boolean isSupported() {
			return UNSAFE != null;
		}
		@Override
		public void addURL(URL url) {
            this.unopenedURLs.add(url);
            this.pathURLs.add(url);
		}
		private static Object getField(final Class<?> clazz, String name, Object object) throws NoSuchFieldException {
            Field field = clazz.getDeclaredField(name);
            long offset = UNSAFE.objectFieldOffset(field);
            return UNSAFE.getObject(object, offset);
        }
    }
    
    /*
     * JarClassLoader
     */
    private static class JarLoader extends WrappedURLClassLoader {
    	private JarLoader(URLClassLoader classLoader) {
    		super(classLoader);
    	}
		@Override
		public boolean isSupported() {
			return true;
		}
		@Override
		public void addURL(URL url) {
			((JarClassLoader)super.classLoader).addURL(url);
		}
    }
}