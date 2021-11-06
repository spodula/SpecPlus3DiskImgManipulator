package diskviewer.libs.disk.cpm;
/**
 * CPM-related utility functions. (Mostly around filenames)
 * 
 * @author graham
 *
 */

public class CPM {
	
	/**
	 * Create a filename that should work in CPM
	 * @param s
	 * @return
	 */
	public static String FixFullName(String s) {
		String fname = s.toUpperCase();
		String filename = "";
		String extension = "";
		if (fname.contains(".")) {
			int i = fname.lastIndexOf(".");
			extension = fname.substring(i + 1);
			filename = fname.substring(0, i);
		} else {
			filename = fname;
		}
		filename = filename + "        ";
		filename = FixFilePart(filename.substring(0, 8).trim());

		extension = extension + "   ";
		extension = FixFilePart(extension.substring(0, 3).trim());
		// NameOnDisk
		return(filename + "." + extension);
	}
	
	/**
	 * Ensure the part of the filename is valid for CPM
	 * 
	 * @param s
	 * @return
	 */
	public static String FixFilePart(String s) {
		char ss[] = s.toUpperCase().toCharArray();
		for (int i = 0; i < ss.length; i++) {
			if (!CharIsCPMValid(ss[i])) {
				ss[i] = '_';
			}
		}
		return (String.copyValueOf(ss));
	}

	/**
	 * Check for a valid character for a CPM file.
	 * @param c
	 * @return
	 */
	
	public static String Validchars = "ABCDEFGHIJKLMNOPQRSTUVQXYZ 0123456789 \"#$'@^_{}~`_";
	
	public static boolean CharIsCPMValid(char c) {
		boolean found = false;
		for(int j=0;j<Validchars.length();j++) {
			if (Validchars.charAt(j) == c) {
				found = true;
			}
		}
		return (found);
	}

}
