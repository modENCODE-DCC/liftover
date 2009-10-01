package org.modencode.tools.liftover;

@SuppressWarnings("serial")
public class MappingException extends Exception {

	public MappingException(String message, Exception cause) {
		super(message, cause);
	}
	public MappingException(String message) {
		super(message);
	}

}
