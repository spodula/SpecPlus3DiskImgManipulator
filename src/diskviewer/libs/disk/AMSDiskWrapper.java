package diskviewer.libs.disk;
/**

 * Wrapper around an AMS file. (Used for Amstrad CPC and Spectrum +3 Disk images) 
 * Handles low level Sector/track parsing and reading the disk information 
 * structures.
 *
 * (NOTE: most of this information was gathered from  https://www.cpcwiki.eu/index.php/Format:DSK_disk_image_file_format )
 * 
 * AMS file:
 * The first 256 bytes are the "disk information block".
 * There are two types, Normal and Extended (I have found +3 disks in both) 
 * 
 * the main differences are:
 *   for NORMAL disks, the track size is defined in the DIB. 
 *   For EXTENDED disks, each track size is defined individually from byte $34 onwards. 
 *   Extended disks enable the encoding of copy-protected disks with variable sector and track sizes. 
 *   
 * 
 * Normal:
 * 	00-16 "MV - CPCEMU Disk-File\r\n"
 * 	17-21 "Disk-Info\r\n"
 * 	22-2f Name of the creator
 * 	30    Number of tracks
 * 	31    Number of sides
 * 	32-33 Track size 
 * 	34-FF Unused
 * 	
 * Extended:
 * 	00-16 "EXTENDED CPC DSK File\r\n"
 * 	17-21 "Disk-Info\r\n"
 * 	22-2f Name of the creator
 * 	30    Number of tracks
 * 	31    Number of sides
 * 	32-33 Not used
 * 	34-FF For each track, one byte representing MSB of track size, 
 * 				Eg, of the track length is 4864 (0x1300 = 9 sectors of 512 bytes + 256 bytes for the track header)
 * 					then the byte would be 13
 * 
 * From $100 onwards is the track data. 
 * For each track:
 *	00-0b "Track Info\r\n"
 *	0c-0f unused
 *	10    Track number
 *	11    Side number
 *	12-13 unused
 *	14    Sector size (1=256, 2=512, 3=1024 ect) 
 *	15    Number of sectors 
 *	16    Gap#3 length
 *	17    Filler byte
 *
 * Next from 18 onwards, is the sector list information.
 * Note that the sectors in the file are not nesserilly consecutive (Indeed i have found they are mostly interleaved)  
 * This list is the same order as the data in the file
 * There are 8 bytes per record
 *  00    Track (Equivelent to "C" parameter in the 765 FDC)
 *  01    Side   (Equivelent to "H" parameter in the 765 FDC)
 *  02    Sector ID (Equivelent to "R" parameter in the 765 FDC) (These are 1-9 for +3 disks, Others use $40-49 and $C0-C9)
 *  03    Sector size  (Equivelent to "N" parameter in the 765 FDC) should be the same as #14 above
 *  04    FDC status register 1 after reading
 *  05    FDC status register 2 after reading
 *  06-07 Actual data length of the sector in bytes
 *   
 * At the next $100 boundary, the actual data starts.
 * this is just a stream of data for each sector in the same order and with the same size as the data above. 
 * 
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AMSDiskWrapper extends DiskWrapper {
	public static int SECTORSIZE = 512;
	
	// Copy of the disk information block at the start of the DSK file
	private byte DiskInfoBlock[] = new byte[256];

	// Parsed version of above.
	private DiskInfo ParsedDiskInfo = null;


	/**
	 * Load a file..
	 * 
	 * @param filename
	 * @throws BadDiskFileException
	 * @throws IOException
	 */
	@Override
	public void load(String filename) throws BadDiskFileException, IOException {
		super.load(filename);
		log("AmsDiskWrapper: Loading " + filename);
		InputStream in = new FileInputStream(filename);
		try {
			// Read the ADF Disk info block into a bit of memory
			// This bit is always 256 bytes long.
			DiskInfoBlock = in.readNBytes(256);
			if (DiskInfoBlock.length != 256) {
					throw new BadDiskFileException("Disk file not big enough");
			}
			// Parse the disk information into a structure.
			ParsedDiskInfo = new DiskInfo(DiskInfoBlock);
			log("AmsDiskWrapper: Read and parsed AMS disk info block.");
			log("AmsDiskWrapper: " + ParsedDiskInfo.toString());
			numtracks = ParsedDiskInfo.tracks;
			numsides = ParsedDiskInfo.sides;
			CREATOR = ParsedDiskInfo.Creator;
			
			

			// Allocate enough space for all the tracks on both sides of the disk.
			// (+3 disks are usually single sided, but you can do funky things
			// with 720K disks so lets not assume Single sided)
			diskTracks = new TrackInfo[numsides * numtracks];
			int Tracknum = 0;
			log("AmsDiskWrapper: Allocated " + String.valueOf(ParsedDiskInfo.tracks * ParsedDiskInfo.sides)
					+ " slots for tracks. (cyl*head)");

			// Track sizes can be variable in the case of extended disks.
			// eg, in the case of some copy protection methods.
			// its easier to just load each track into an array.
			for (int tracknum = 0; tracknum < ParsedDiskInfo.tracks; tracknum++) {
				// Load the track
				byte CurrentRawTrack[] = in.readNBytes(ParsedDiskInfo.TrackSizes[tracknum]);
				if (CurrentRawTrack.length != ParsedDiskInfo.TrackSizes[tracknum]) {
						throw new BadDiskFileException("Disk file not big enough");
					}
				// *********************************************************
				// get the track header...
				// *********************************************************
				TrackInfo CurrentTrack = new TrackInfo();
				// Track-Info
				for (int i = 0; i < 12; i++) {
					CurrentTrack.header = CurrentTrack.header + (char) CurrentRawTrack[i];
				}
				// track number
				CurrentTrack.tracknum = (int) CurrentRawTrack[16] & 0xff;
				// side number
				CurrentTrack.side = (int) CurrentRawTrack[17] & 0xff;
				// Data rate (optional)
				CurrentTrack.datarate = (int) CurrentRawTrack[18] & 0xff;
				// Recording mode(optional)
				CurrentTrack.recordingmode = (int) CurrentRawTrack[19] & 0xff;
				// sector size
				CurrentTrack.sectorsz = (int) CurrentRawTrack[20] * 256;
				// Number of sectors
				CurrentTrack.numsectors = (int) CurrentRawTrack[21] & 0xff;
				// gap #3 length
				CurrentTrack.gap3len = (int) CurrentRawTrack[22] & 0xff;
				// Filler byte
				CurrentTrack.fillerByte = (int) CurrentRawTrack[23] & 0xff;

				// *********************************************************
				// Sector information list starts here.
				// *********************************************************
				CurrentTrack.Sectors = new Sector[CurrentTrack.numsectors];
				int sectorbase = 24;
				int minsector = 255;
				int maxsector = 0;
				for (int i = 0; i < CurrentTrack.numsectors; i++) {
					Sector CurrentSector = new Sector();
					// track
					CurrentSector.track = (int) CurrentRawTrack[sectorbase] & 0xff;
					// side
					CurrentSector.side = (int) CurrentRawTrack[sectorbase + 1] & 0xff;
					// sector id
					CurrentSector.sectorID = (int) CurrentRawTrack[sectorbase + 2] & 0xff;
					if (CurrentSector.sectorID > maxsector) {
						maxsector = CurrentSector.sectorID;
					}
					if (CurrentSector.sectorID < minsector) {
						minsector = CurrentSector.sectorID;
					}
					// sector sz
					CurrentSector.Sectorsz = (int) CurrentRawTrack[sectorbase + 3] & 0xff;
					// fdc status 1
					CurrentSector.FDCsr1 = (int) CurrentRawTrack[sectorbase + 4] & 0xff;
					// fdc statuis 2
					CurrentSector.FDCsr2 = (int) CurrentRawTrack[sectorbase + 5] & 0xff;
					// actual data length. Note this is only valid on EXTENDED format disks.
					// If not the case, the sector size read from the track block.
					CurrentSector.ActualSize = (int) (CurrentRawTrack[sectorbase + 7] & 0xff) * 256
							+ (int) (CurrentRawTrack[sectorbase + 6] & 0xff);
					if (!ParsedDiskInfo.IsExtended) {
						CurrentSector.ActualSize = CurrentTrack.sectorsz;
					}
					// Add sector
					CurrentTrack.Sectors[i] = CurrentSector;
					sectorbase = sectorbase + 8;
				}
				CurrentTrack.minsectorID = minsector;
				CurrentTrack.maxsectorID = maxsector;

				// The first sector is is after the track information block on the next $100
				// junction.
				sectorbase = sectorbase + 0x100;
				sectorbase = sectorbase - (sectorbase % 0x100);

				// *********************************************************
				// now the sector data
				// sectorbase should now point to the start of the first sector.
				// *********************************************************
				for (Sector sect : CurrentTrack.Sectors) {
					byte rawdata[] = new byte[sect.ActualSize];
					for (int i = 0; i < sect.ActualSize; i++) {
						rawdata[i] = CurrentRawTrack[sectorbase++];
					}
					sect.data = rawdata;
				}

				// Now add the completed track to the track list.
				if (VerboseMode) {
					log(CurrentTrack.AsString());
				} else {
					System.out.print(".");
				}
				diskTracks[Tracknum++] = CurrentTrack;
			}
			System.out.println(" " + String.valueOf(Tracknum) + " tracks");

		} finally {
			in.close();
		}
		setModified(false);
		IsValid = true;

	}

	/**
	 * Get the disk info structure.
	 * 
	 * @return
	 * @throws BadDiskFileException
	 */
	public diskviewer.libs.disk.DiskInfo GetDiskInfo() throws BadDiskFileException {
		return ParsedDiskInfo;
	}

	/**
	 * Save the current file to the given name. This assumes any DIRENT changes have
	 * updated their sectors.
	 * 
	 * @param filename
	 * @throws IOException
	 */
	@Override
	public void save(String filename) throws IOException {
		super.save(filename);
		int numtracks = 0;
		int numsectors = 0;
		FileOutputStream fos = new FileOutputStream(filename);
		try {
			// write the AMS header
			fos.write(DiskInfoBlock);
			// now write every track in sequence
			for (TrackInfo track : diskTracks) {
				// Create the track-info
				byte TrackInfoBlock[] = new byte[0x100];
				for (int i = 0; i < 12; i++) {
					TrackInfoBlock[i] = (byte) "Track-Info\r\n".charAt(i);
				}
				TrackInfoBlock[16] = (byte) (track.tracknum & 0xff);
				TrackInfoBlock[17] = (byte) (track.side & 0xff);
				TrackInfoBlock[18] = (byte) (track.datarate & 0xff);
				TrackInfoBlock[19] = (byte) (track.recordingmode & 0xff);
				TrackInfoBlock[20] = (byte) ((track.sectorsz / 256) & 0xff);
				TrackInfoBlock[21] = (byte) (track.numsectors & 0xff);
				TrackInfoBlock[22] = (byte) (track.gap3len & 0xff);
				TrackInfoBlock[23] = (byte) (track.fillerByte & 0xff);
				// each sector
				int ptr = 24;
				for (Sector s : track.Sectors) {
					TrackInfoBlock[ptr++] = (byte) (s.track & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.side & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.sectorID & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.Sectorsz & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.FDCsr1 & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.FDCsr2 & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.ActualSize & 0xff);
					TrackInfoBlock[ptr++] = (byte) (s.ActualSize / 0x100);
				}
				// write track info block.
				fos.write(TrackInfoBlock);
				// Now, each sector in order.
				for (Sector s : track.Sectors) {
					fos.write(s.data);
					numsectors++;
				}
				numtracks++;
			}
			System.out.println("Written " + numtracks + " Tracks and " + numsectors + " sectors.");
		} finally {
			fos.close();
		}
		setModified(false);

	}

	/**
	 * Create a blank AMS disk with the given tracks, heads, sectors per track, ect.
	 * 
	 * @param tracks
	 * @param heads
	 * @param spt
	 * @param minsector
	 * @param fillerbyte
	 * @throws BadDiskFileException
	 */
	public void CreateAMSDisk(int tracks, int heads, int spt, int minsector, boolean IsExtended, String ADFHeader,
			int fillerbyte) {
		// ADF header
		DiskInfoBlock = new byte[256];
		for (int i = 0; i < DiskInfoBlock.length; i++) {
			DiskInfoBlock[i] = 0;
		}

		// copy in the header.
		for (int i = 0; i < ADFHeader.length(); i++) {
			DiskInfoBlock[i] = (byte) ADFHeader.charAt(i);
		}

		// creator
		for (int i = 0; i < CREATOR.length(); i++) {
			DiskInfoBlock[0x22 + i] = (byte) CREATOR.charAt(i);
		}

		// tracks and sides.
		DiskInfoBlock[0x30] = (byte) tracks;
		DiskInfoBlock[0x31] = (byte) heads;

		if (!IsExtended) {
			// Size of track. (Sector size * sector + track info block (256) )
			int tracksz = (SECTORSIZE * spt) + 0x100;
			DiskInfoBlock[0x32] = (byte) (tracksz & 0xff);
			DiskInfoBlock[0x33] = (byte) (tracksz / 256);
		} else {
			// extended disks have a track size for each track.
			int tracksz = (SECTORSIZE * spt) + 0x100;
			// just want the highest byte.

			// write all the track sizes.
			tracksz = tracksz / 0x100;
			for (int i = 0; i < tracks * heads; i++) {
				DiskInfoBlock[0x34 + i] = (byte) tracksz;
			}
		}
		try {
			ParsedDiskInfo = new DiskInfo(DiskInfoBlock);

			diskTracks = new TrackInfo[tracks * heads];

			// write each track.
			for (int tracknum = 0; tracknum < tracks; tracknum++) {
				for (int headnum = 0; headnum < heads; headnum++) {
					// track information block
					TrackInfo newtrack = new TrackInfo();
					newtrack.header = "Track-Info\r\n";
					newtrack.tracknum = tracknum;
					newtrack.side = headnum;
					newtrack.gap3len = 0x4e; ////////////////////////////////////
					newtrack.fillerByte = fillerbyte;
					newtrack.minsectorID = minsector;
					newtrack.maxsectorID = minsector + spt - 1;
					newtrack.datarate = 0;
					newtrack.recordingmode = 0;

					// sectors
					newtrack.Sectors = new Sector[spt];
					for (int sectnum = 0; sectnum < spt; sectnum++) {
						Sector NewSector = new Sector();

						NewSector.track = tracknum;
						NewSector.side = headnum;
						NewSector.sectorID = sectnum + minsector;

						NewSector.Sectorsz = 0x02;

						NewSector.FDCsr1 = 0;
						NewSector.FDCsr2 = 0;

						NewSector.ActualSize = 512;

						NewSector.data = new byte[512];
						for (int i = 0; i < 512; i++) {
							NewSector.data[i] = (byte) fillerbyte;
						}
						newtrack.Sectors[sectnum] = NewSector;
					}
					diskTracks[(tracknum * heads) + headnum] = newtrack;
				} // headnum
			} // tracknum
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		}
		IsValid = true;
	}
}
