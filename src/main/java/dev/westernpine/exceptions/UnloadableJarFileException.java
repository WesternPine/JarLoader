package dev.westernpine.exceptions;

import java.io.File;

public class UnloadableJarFileException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public UnloadableJarFileException(File file) {
		super("The file " + file.getName() + " was not able to be loaded!");
	}

}
