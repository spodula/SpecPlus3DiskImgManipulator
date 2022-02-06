package diskviewer.libs.disk;

import java.io.IOException;

public class DiskWrapper {
	//Creator for saving disks.
	public static String CREATOR = "Plus3DiskEditr";
	
	// ******************************************
	// Disk metadata
	// ******************************************
	// if TRUE, a valid disk is loaded
	//Last filename
	public String LastFileName = "";

	//CHS / HCS flag 
	public boolean CHSHCS=false;
	
	//Disk data. 
	public int numsides = 0; // Sides (Almost always 1 for +3 disks)
	public int numtracks = 0; // tracks (usually 40)
	
	// Tracks in the file.
	public TrackInfo diskTracks[] = null;
	
	//Gets the track number depending on whether it
	//is using CHS addressing or HCS addressing. 
	//+3 disks are always CHS, but i intend to use this for other things. 
	public TrackInfo GetLinearTrack(int track) {
		int newhead=0;
		int newtrack=0;
		if(!CHSHCS) { //CHS
			newhead = (track % numsides);
			newtrack = (track / numsides);
		} else {  //HCS
			newtrack = (track % numtracks);
			newhead =  (track / numtracks);
		}
		return(GetTrack(newtrack , newhead));
	}
	//get a particular track. 
	public TrackInfo GetTrack(int cyl, int head) {
		for (TrackInfo trk:diskTracks) {
			if (trk.side == head && trk.tracknum == cyl) {
				return(trk);
			}
		}
		return(null);
	}


	// ******************************************
	// Status flags.
	// ******************************************
	// if TRUE, a valid disk is loaded
	public boolean IsValid = false;

	
	// ******************************************
	// Verbose mode
	// ******************************************
	public boolean VerboseMode = true;

	public void log(String s) {
		if (VerboseMode) {
			System.out.println(s);
		}
	}
	// ******************************************
	// Modify tracking and callbacks
	// ******************************************
	// Modify event callback
	public ModifiedEvent OnModify = null;

	// private storage as to the current state
	private boolean DiskModified = false;

	// get modified
	public boolean getModified() {
		return (DiskModified);
	}

	// Update modified.
	public void setModified(boolean Modified) {
		DiskModified = Modified;
		if (OnModify != null) {
			OnModify.ModifiedChanged();
		}
	}
	// ******************************************
	// Default methods
	// ******************************************
	public void load(String filename)  throws BadDiskFileException, IOException {
		LastFileName = filename;
	}
	
	public void save(String filename) throws IOException  {
		LastFileName = filename;
	}

}
