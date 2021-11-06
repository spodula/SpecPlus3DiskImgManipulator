package diskviewer.libs.disk;

/**
 * Parser for the AMSDisk DISKINFO track (1st 256 bytes of the AMS file) See
 * https://www.cpcwiki.eu/index.php/Format:DSK_disk_image_file_format
 * 
 * @author Graham
 *
 */

public class DiskInfo {
	public String DiskID = "";
	public String Creator = "";
	public int tracks = 0;
	public int sides = 0;
	public int tracksz = 0;
	public boolean IsExtended;

	public int TrackSizes[] = null;
	public int LargestTrackSize = 0;

	/**
	 * Construct and populate the object
	 * 
	 * @param cpcDiskInfoBlock
	 * @throws BadDiskFileException
	 */
	DiskInfo(byte[] cpcDiskInfoBlock) throws BadDiskFileException {
		PopulateFromCPCDSKFile(cpcDiskInfoBlock);
	}

	/**
	 * decode and populate the object from the passed in 256 bytes. 
	 * 
	 * @param cpcDiskInfoBlock
	 * @throws BadDiskFileException
	 */
	void PopulateFromCPCDSKFile(byte[] cpcDiskInfoBlock) throws BadDiskFileException {
		if (cpcDiskInfoBlock.length != 256) {
			throw new BadDiskFileException("cpc Disk info block is the wrong size (Expecting 256 bytes)");
		}
		DiskID = "";
		for (int i = 0; i < 34; i++) {
			if (cpcDiskInfoBlock[i] > 0) {
				DiskID = DiskID + (char) cpcDiskInfoBlock[i];
			}
		}
		Creator =  "";
		for (int i = 34; i < 48; i++) {
			if (cpcDiskInfoBlock[i] > 0) {
				Creator = Creator + (char) cpcDiskInfoBlock[i];
			}
		}
		tracks = cpcDiskInfoBlock[48] & 0xff;
		sides = cpcDiskInfoBlock[49] & 0xff;
		tracksz = ((cpcDiskInfoBlock[51] & 0xff) * 256) + (cpcDiskInfoBlock[50] & 0xff);
		IsExtended = DiskID.contains("EXTENDED");
		TrackSizes = new int[tracks * sides];

		if (IsExtended) { // track sizes are per track for extended disks
			// are from the index for extended disks.
			int numtrack = 0;
			for (int i = 0; i < (tracks * sides); i++) {
				TrackSizes[i] = (cpcDiskInfoBlock[52 + i] & 0xff) * 256;
				if (LargestTrackSize < TrackSizes[i]) {
					LargestTrackSize = TrackSizes[i];
				}
				if (cpcDiskInfoBlock[52 + i] != 0) {
					numtrack++;
				}
			}
			tracks = numtrack;
		} else { // for non-extended disks, they are all the same as above.
			for (int i = 0; i < (tracks * sides); i++) {
				TrackSizes[i] = tracksz;
			}
			LargestTrackSize = tracksz;

		}
	}

	@Override
	public String toString() {
		String result = "DiskID: " + DiskID.trim() + " Creator: " + Creator.trim() + " Tracks: " + tracks + " Sides: "
				+ sides + " Tracksz: " + tracksz + " Largest:" + LargestTrackSize;

		return (result);
	}

}
