<h1 align="center">JarLoader</h1>
<h3 align="center">A Java Runtime Jar and dependency management system!</h3>
  <div align="center">
    <a href="https://github.com/WesternPine/JarLoader/">
        <img src="https://img.shields.io/github/license/WesternPine/JarLoader">
    </a>
    <a href="https://jitpack.io/#WesternPine/JarLoader">
        <img src="https://jitpack.io/v/WesternPine/JarLoader.svg">
    </a>
  </div>

<hr>

# What is JarLoader?

Jar loader is an overly simplified and abstracted ClassLoader utility, maven dependency manager, and Module & Dependency Mapping utility, designed for simple jar loading at runtime. This is my first attempt at messing with class loaders, and creating a dependency mapping system to organize them all.

## Why would I use JarLoader?

There are actually several great reasons to use a utility like this! 

One such example, would be to load all dependencies at runtime, rather than building your jar with all the dependencies inside them. This reduces the overall filesize and complexity of the jar. Still need another reason? How about to create a plugin system for your personal project! Create your own API, and load in projects that depend on your project. Still need more reasons? Alright, how about live-updating code without even stopping the JVM... yea. Live-Loading jars at runtime is VERY powerful and versatile.

## What is a ClassLoader?

//CONTENT HERE (System, EXT, Application)

# Examples

__**NOTE**__ In the following examples, you'll see a 'true' value for one of the parameters for some functions. This value determines if the created ClassLoader should be saved to a set of other loaders that can be used to resolve classes not accessible to the created loader. If this is false, the created ClassLoader will only have access to classes loaded by the parent class loader, or classes explicitly specified, and the other loaders will not be able to use classes loaded by it. You can change this at any time by using #save() and #unsave().

Get a wrapper for the current class loader, that can inject URLs to look for classes at, and add a URL to it.
```
WrappedURLClassLoader loader = JarLoader.wrapLoader(this.class.getClassLoader()).addUrl(fileUrl);
```

Loading dependencies into the current ClassLoader:
```
JarLoader.loadDependency(this.class.getClassLoader(), new Dependency("org.jsoup", "jsoup", "1.13.1"), dependency -> new File(dependency.artifactId+"-"+dependency.version+".jar"));
//OR with a specific repository:
JarLoader.loadDependency(this.class.getClassLoader(), new Dependency("org.jsoup", "jsoup", "1.13.1").withRepository("Maven Central", "https://repo1.maven.org/maven2/"), dependency -> new File(dependency.artifactId+"-"+dependency.version+".jar"));
```

Make a new loader to load jar file classes. Then load a new File or URL.
```
JarClassLoader loader = JarLoader.newLoader(true).addFile(jarFile);
```

Make a new loader to load a jar file, and then load a class from that file.
```
Jar uninitialized = JarLoader.newLoaderWithClass(jarFile, "Package.Name.ClassName", true);
```

This is the fun one! Load a list of files as modules, remove any duplicate modules, remove any chained(whose dependencies essentially depend on themselves, directly or indirectly) module dependencies, remove modules missing their required dependencies, then order modules in order of initialization, and get the ordered list.
```
LinkedList<JavaModule> modules = new DependencyMapper(files).getMappedModules();
```

# !Exceptions!

Of course, not everything perfect. There are some things to know before hopping in and testing it out.
  1. Ignore this if you're wrapping a ClassLoader of the custom type JarClassLoader, as a specific implementation was put in place to get around the following issue: First off, this method intakes any ClassLoader, but it's only compatable with the URLClassLoader. This was done to help you, the developer, from doing checks and casts. Simply wrap, and check #isSupported(). Also, as of Java 9+, you will have to start the JVM with the flag: `--add-opens=java.base/java.net=ALL-UNNAMED` This is because Java's Module system not uses strong encapsulation, meaning a deep-reflective operation (such as the required `setAccessible` reflection operation) is now prohibited. You will need to use the flag mentioned previously to get around this.
  2. This library will need to be availible inside the initializing jar to operate! You can't use code that isn't there. You don't need to add this in your jar if its in the parent class loader.
  3. Modules automatically are saved to the loaders list. This is because of the dependency mapping and such, it only seems logical that they can find classes from each other. If you wish to disable this, just just the class loader from the module list, and unsave it.
  4. The module events (onLoad and onUnload consumers) are executed after they have been loaded/unloaded.
  5. Exceptions that occur with a module throw a ModuleLoadException. This exception does contain the original exception, as well as a message to help further diagnose the issue.

# Things To Note.

  1. Everything accessible is documented. Check the documentation if you need help understanding what something is or what it does.
  2. This has not been fully tested! This is more of a mock-up api, intended for special scenarios, or simple tasks. Please don't expect this to be a professionally written library without any flaws, because it isn't. Stuff will break.
  3. PLEASE, if you see something that can be improved, or something that needs correcting, please do so. I personally have a good grasp on class loaders now, but I'm VERY far away from fully understanding them.

# One Last Thing.

Special thanks to these GitHub accounts and their code for guiding me in this process.
  - https://github.com/slimjar
  - https://github.com/lucko
  - https://github.com/VelocityPowered

License
----

[MIT](https://choosealicense.com/)
