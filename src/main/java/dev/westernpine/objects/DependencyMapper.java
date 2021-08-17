package dev.westernpine.objects;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.westernpine.objects.module.JavaModule;

/**
 * This was a headache for 2 days straight... I cen't believe I wrote this, what a lucid dream.
 * @author Tyler
 *
 */
public class DependencyMapper {
	
	private final Comparator<JavaModule> comparator = new Comparator<JavaModule>() {
		@Override
		public int compare(JavaModule o1, JavaModule o2) {
			if(o1.isDependency(o2.getName()))
				return 1;
			return 0;
		}
	};
	
	private LinkedList<JavaModule> toMap;
	
	private Map<JavaModule, DependencyPath> removedChains;
	
	private Map<JavaModule, String> removedMissingDeendencies;
	
	public DependencyMapper(LinkedList<JavaModule> toMap) {
		this.toMap = toMap;
		this.removedChains = new HashMap<>();
		this.removedMissingDeendencies = new HashMap<>();
		map();
	}
	
	/**
	 * 
	 * @return The LinkedList of dependencies mapped in order of execution.
	 */
	public LinkedList<JavaModule> getMappedDependencies() {
		return this.toMap;
	}
	
	/**
	 * 
	 * @return A map of Modules with their corresponding dependency path that inevitably depends on itself.
	 */
	public Map<JavaModule, DependencyPath> getRemovedChains() {
		return this.removedChains;
	}
	
	/**
	 * 
	 * @return A map of Modules with their missing required dependency.
	 */
	public Map<JavaModule, String> getRemovedMissingDependencies() {
		return this.removedMissingDeendencies;
	}
	
	public static class DependencyPath {
		
		private DependencyPath parent;
		
		private String name;
		
		public DependencyPath(DependencyPath parent, String name) {
			this.parent = parent;
			this.name = name;
		}
		
		/**
		 * 
		 * @return Any parent dependeny path.
		 */
		public DependencyPath parent() {
			return this.parent;
		}
		
		/**
		 * 
		 * @return The name representing a dependency.
		 */
		public String name() {
			return this.name;
		}
		
		/**
		 * 
		 * @return True if the path of dependencies is linking to itself.
		 */
		public boolean containsSelf() {
			DependencyPath p = parent;
			while(p != null) {
				if(p.name.equals(name))
					return true;
				p = p.parent;
			}
			return false;
		}
		
		@Override
		public String toString() {
			return (parent == null ? "" : parent.toString() + ", ") + name;
		}
		
	}
	
	private DependencyMapper map() {
		
		//remove duplicate dependencies			
		toMap.stream().filter(module -> toMap.stream().filter(mod -> mod.getName().equals(module.getName())).count() > 1).map(JavaModule::getName).collect(Collectors.toList()).forEach(name -> toMap.removeIf(module -> module.getName().equals(name)));
		
		//Remove chained dependencies.
		Iterator<JavaModule> it = toMap.iterator();
		while(it.hasNext()) {
			JavaModule module = it.next();
			DependencyPath dpath = mapDuplicates(module, new DependencyPath(null, module.getName()));
			if(dpath.containsSelf()) {
				it.remove();
				removedChains.put(module, dpath);
			}
		}
		
		//Remove missing dependencies.
		while(true) {
			boolean removed = false;
			Iterator<JavaModule> it1 = toMap.iterator();
			A: while(it1.hasNext()) {
				JavaModule module = it1.next();
				for(String dependency : module.getDepends()) {
					if(!toMap.stream().map(JavaModule::getName).collect(Collectors.toList()).contains(dependency)) { //done here so we can update dependencies every time.
						it1.remove();
						removed = true;
						removedMissingDeendencies.put(module, dependency);
						break A;
					}
				}
			}
			if(removed)
				continue;
			break;
		}
		
		//Order dependencies.
		return map0();
	}
	
	private DependencyPath mapDuplicates(JavaModule module, DependencyPath dpath) {
		List<DependencyPath> dependencies = module.getAllDependencies().stream().map(dependency -> new DependencyPath(dpath, dependency)).collect(Collectors.toList());
		for(DependencyPath dePath : dependencies) {
			if(dePath.containsSelf())
				return dePath;
			Optional<JavaModule> dependencyModule = toMap.stream().filter(mod -> mod.getName().equals(dePath.name())).findAny();
			if(dependencyModule.isPresent()) {
				return mapDuplicates(dependencyModule.get(), dePath);
			}
			
		}
		return dpath;
	}
	
	//Recursion!
	private DependencyMapper map0() {
		if(toMap.size() < 2)
			return this;
		boolean remap = false;
		int at = 0;
		List<JavaModule> toRemove = new ArrayList<>();
		
		JavaModule addFirst = null;
		Iterator<JavaModule> it1 = toMap.iterator();
		A: while(it1.hasNext()) {
			JavaModule o1 = it1.next();
			Iterator<JavaModule> it2 = toMap.iterator();
			int index = 0;
			while(it2.hasNext()) {
				JavaModule o2 = null;
				while(index != at) {
					index++;
					o2 = it2.next();
				}
				o2 = it2.next();
				if(o1.equals(o2))
					continue;
				int result = comparator.compare(o1, o2);
				switch(result) {
				case 1:
					toRemove.add(o2);
					addFirst = o2;
					remap = true;
					break A;
				default:
					continue;
				}
			}
			at++;
		}
		if(!toRemove.isEmpty())
			toMap.removeAll(toRemove);
		if(addFirst != null)
			toMap.addFirst(addFirst);
		if(remap)
			return map0();
		return this;
	}
	
}