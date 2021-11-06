package diskviewer.libs.disk;
/**
 * Representation of a single track including all its sectors. 
 * @author Graham
 *
 */

public class TrackInfo {
	//These are metadata from the file itself. 
	public String header="";
	
	//The track number we represent
	public int tracknum=0;
	
	//Side of the track
	public int side=0;
	
	//Sector size
	public int sectorsz=0;
	
	//Number of sectors
	public int numsectors=0;
	
	//Gap3Length between sectors
	public int gap3len=0;
	
	//Filler byte of the disk (usually 0xe5
	public int fillerByte=0;
	
	//Calculated Minimum and Maximum sector IDs . this is one of the 
	//ways a disk type is identified. 
	public int minsectorID=0;
	public int maxsectorID=0;
	
	//Sector data.
	public Sector[] Sectors;
	
	//Note, these are optional and may not be used. 
	//See: https://www.cpcwiki.eu/index.php/Format:DSK_disk_image_file_format
	
	//recording Data rate (SS/DS/DS2)
	public int datarate=0;
	
	//Recording mode. (FM/MFM)
	public int recordingmode=0;
	
	public String AsString() {
		String result = "";
		result = "Track:"+tracknum+
				" side:"+side+
				" sectorsz:"+sectorsz+
				" Numsectors:"+numsectors+" ("+Sectors.length+") "+
				" gap3len:"+gap3len+
				" Filler:"+fillerByte+
				" minsector:"+minsectorID+
				" maxsector:"+maxsectorID;
		
		return(result);
	}
	
	//functions to convert the above into displayable text. 
	/**
	 * Returns the data rate flag as text.
	 * See: https://www.cpcwiki.eu/index.php/Format:DSK_disk_image_file_format
	 * @return
	 */
	public String GetDataRate() {
		String result = "Not set";
		switch (datarate) {
		case 1:
			result = "SS";
			break;
		case 2:
			result = "DS (Alternating sides)";
			break;
		case 3:
			result = "DS (Successive sides";
			break;
		}
		return(result);
	}
	
	/**
	 * Returns the recording mode flag as text.
	 * See: https://www.cpcwiki.eu/index.php/Format:DSK_disk_image_file_format
	 * @return String description text
	 */
	public String GetRecordingMode() {
		String result = "Not set";
		switch (datarate) {
		case 1:
			result = "FM";
			break;
		case 2:
			result = "MFM";
			break;
		}
		return(result);
	}
	
	/**
	 * Sectors are stored in the order they are in the DSK file (Which should be interleaved)
	 * This function will get a given sector
	 * @param SectorID: Sector to find
	 * @return sector structure or NULL if not found.
	 */
	public Sector GetSectorBySectorID(int SectorID) {
		Sector Result = null;
		for(Sector s: Sectors) {
			if (s.sectorID == SectorID) {
				Result = s;
			}
		}
		if (Result==null) {
			System.out.println("Track "+tracknum+" Sector "+SectorID+" not found.");
		}
		return(Result);
	}
}
