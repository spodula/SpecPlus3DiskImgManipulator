package diskviewer.libs.disk;
/**
 * Just a generic little exception.
 * @author Graham
 *
 */

public class BadDiskFileException extends Exception {
    public String ExtraMessage="";
    
	public BadDiskFileException(String msg) {
		ExtraMessage=msg;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -7923158262539598735L;

}
