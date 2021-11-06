package diskviewer.libs.disk.cpm;

/**
 * This encapsulates a directory entry structure. Details are from here:
 * http://www.cpcwiki.eu/index.php/Disk_structure
 * 
 * A dirent is 32 bits. arranged as follows: 0: Status 1-8: filename 9-11:
 * Extension 12: Lower 4 bits are the extent number 13: Number of bytes in the
 * last used 128 byte record where 0=128) 14: Lower 5 bits are the upper bits of
 * the extent number 15: Number of 128 byte records used in the last logical
 * extent. All previous extents are considered to be full. 16-31: Block numbers.
 * 
 * In the extention, bit 7 is used for flags. bit 7 of [9] is read-only, [10] is
 * system and [11] is Archived
 * 
 * @author Graham
 *
 */

public class Dirent {
	// types of directory entry.
	public static int DIRENT_FILE = 0;
	public static int DIRENT_LABEL = 1;
	public static int DIRENT_UNUSED = 2;
	public static int DIRENT_DELETED = 3;
	public static int DIRENT_UNKNOWN = 4;

	// raw data to interpret on the fly.
	public byte rawdirent[] = new byte[32];
	public int entrynum = 0;

	// Determines if block IDs are treated as 8 bit or 16 bit values.
	public boolean Is16BitBlockID = false;

	Dirent(int number) {
		entrynum = number;
	}

	public void setType(int typ) {
		rawdirent[0] = 0;
		if (typ != DIRENT_FILE) {
			rawdirent[0] = (byte) (0xE5);
		}
	}

	/**
	 * return the type of directory entry. on the +3, we are unlikely to see
	 * anything other than FILE and UNUSED.
	 * 
	 * @return
	 */
	public int getType() {
		int rdi = (int) rawdirent[0] & 0xff;
		if (rdi < 32) {
			return (DIRENT_FILE);
		} else if (rdi == 32) {
			return (DIRENT_LABEL);
		} else if (rdi == 0xE5) {
			if (((int) rawdirent[1] & 0xff) != 0xE5) {
				return (DIRENT_DELETED);
			} else {
				return (DIRENT_UNUSED);
			}
		}
		return (DIRENT_UNKNOWN);

	}

	/**
	 * Get the user number. Usually 0
	 * 
	 * @return
	 */
	public int GetUserNumber() {
		if (getType() == DIRENT_FILE) {
			return (rawdirent[0] & 0x0f);
		}
		return (0);
	}

	/**
	 * Get the filename. Bit 7 is used for flags so is stripped off. Filenames are
	 * padded with spaces in the dirent, so it trims them as well.
	 * 
	 * @return
	 */
	public String GetFilename() {
		String result = "";
		// filename
		for (int i = 1; i < 9; i++) {
			result = result + (char) (rawdirent[i] & 0x7f);
		}
		result = result.trim();

		// extension
		String ext = "";
		for (int i = 9; i < 12; i++) {
			ext = ext + (char) (rawdirent[i] & 0x7f);
		}
		// add in the extension if there is one.
		if (!ext.isBlank()) {
			result = result + "." + ext;
		}

		return (result);
	}

	/**
	 * Set the filename
	 * 
	 * @param newFileName
	 */
	public void SetFilename(String newFileName) {
		newFileName = newFileName.toUpperCase();
		// seperate into filename and extension
		String filename = "";
		String extension = "";
		if (newFileName.contains(".")) {
			int i = newFileName.lastIndexOf(".");
			extension = newFileName.substring(i + 1);
			filename = newFileName.substring(0, i);
		} else {
			filename = newFileName;
		}
		// Make sure the filename is valid and pad out with spaces.
		filename = filename + "        ";
		filename = CPM.FixFilePart(filename.substring(0, 8).trim());
		filename = filename + "        ";

		// make sure the extension is valid and pad out with spaces
		extension = extension + "   ";
		extension = CPM.FixFilePart(extension.substring(0, 3).trim());
		extension = extension + "   ";

		// set the filename
		for (int i = 0; i < 8; i++) {
			rawdirent[i + 1] = (byte) (filename.charAt(i) & 0xff);
		}
		// set the extension
		for (int i = 0; i < 3; i++) {
			rawdirent[i + 9] = (byte) (extension.charAt(i) & 0xff);
		}

	}

	// Fetch the read only flag. (bit 7 of the first extension character)
	public boolean GetReadOnly() {
		return ((rawdirent[9] & 0x80) == 0x80);
	}

	// Fetch the read only flag. (bit 7 of the second extension character)
	public boolean GetSystem() {
		return ((rawdirent[10] & 0x80) == 0x80);
	}

	// Fetch the read only flag. (bit 7 of the third extension character)
	public boolean GetArchive() {
		return ((rawdirent[11] & 0x80) == 0x80);
	}

	/**
	 * Get an integer array of the blocks used in the file.
	 * 
	 * @return
	 */
	public int[] getBlocks() {
		int result[] = new int[0];
		// Block lists are only valid in DELETED or FILE dirents.
		if ((getType() == DIRENT_FILE) || (getType() == DIRENT_DELETED)) {

			int numextents = 0;
			int possextents[] = new int[16];
			int i = 16;
			while ((i < 32)) {
				int blocknum = (rawdirent[i++] & 0xff);
				//If 16 bit blocknum, read second byte
				if (Is16BitBlockID) {
					blocknum = blocknum + ((rawdirent[i++] & 0xff) * 0x100);
				}
				//set it if the block is not empty
				if (blocknum > 0) {
					possextents[numextents++] = blocknum;
				}
			}

			// transfer to results
			result = new int[numextents];
			for (i = 0; i < numextents; i++) {
				result[i] = (int) (possextents[i] & 0xff);
			}
		}

		return (result);
	}

	/**
	 * Load the dirent from the given array. (Basically just copy 32 bytes from the
	 * given location)
	 * 
	 * @param array
	 * @param start
	 */
	public void LoadDirentFromArray(byte array[], int start) {
		for (int i = 0; i < 32; i++) {
			rawdirent[i] = array[start + i];
		}
	}

	/**
	 * Get the logical extent number of this extent. CPM can have up to 512 extents
	 * on a file, but this is unlikely on the +3. The lower 4 bits of dirent[12]
	 * contain the LSB and the lower 5 bits of dirent[14] contain the upper 5 bits
	 * for a 9 bit number (0-511).
	 * 
	 * @return the current logical extent number.
	 */
	public int GetLogicalExtentNum() {
		return ((rawdirent[12] & 0x0f) + ((rawdirent[14] & 0x1f) * 0x0f));
	}

	/**
	 * Return the number of byte in the last logical block in the dirent. 
	 * Note, this value is stored as the number of records of 128 bits so 
	 * any CPM file must be a multiple of 128. 
	 * 
	 * @return
	 */
	public int GetBytesInLastLogicalBlock() {
		return (((int) (rawdirent[15] & 0xff)) * 128);
	}

}
