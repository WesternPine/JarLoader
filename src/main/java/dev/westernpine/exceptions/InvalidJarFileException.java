package dev.westernpine.exceptions;

import java.io.File;

public class InvalidJarFileException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public InvalidJarFileException(File file) {
		super("The file " + file.getName() + " is not a valid jar file!");
	}

}
