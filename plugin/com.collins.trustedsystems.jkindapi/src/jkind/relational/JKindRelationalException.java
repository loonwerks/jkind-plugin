package jkind.relational;
import jkind.JKindException;

public class JKindRelationalException extends JKindException {

	private static final long serialVersionUID = -3207520589194343415L;

	public JKindRelationalException(String message) {
		super(message);
	}
	
	public JKindRelationalException(String message, Throwable t) {
		super(message, t);
	}
}
