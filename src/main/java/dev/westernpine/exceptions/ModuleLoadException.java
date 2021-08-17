package dev.westernpine.exceptions;

public class ModuleLoadException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ModuleLoadException(String reason, Throwable cause) {
		super(reason, cause);
	}

}
