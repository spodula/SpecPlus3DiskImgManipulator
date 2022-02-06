package diskviewer.libs.disk.cpm;

/**
 * This object deals with CPM level information including CPM
 * directories, the boot sector, et al.
 * 
 * CPM file system as used on the Speccy/Amstrad CPC
https://www.seasip.info/Cpm/amsform.html
There are a few things need to identify the CPM parameters used on a disk.
   Firstly, check the minimum sector ID;
   		$41: CPC system disk - These have two tracks reserved for the CPM bootsector. (9216 bytes). This leaves 171k for files (minus 2k directory entries)
			numsides = 1
			numtracks = 40
			numsectors = 9
			sectorPow = 2; (512 bytes/sector)
			reservedTracks = 2;  
			blockPow = 3; (1024 bytes/block)
			dirBlocks = 2;
			rwGapLength = 0x2a;
			fmtGapLength = 0x52;
   		$C1: CPC data disk - These have NO tracks reserved, thus the directory starts on Track 0, first sector. This gives the full 180K for data (minus 2k directory entries)
			numsides = 1
			numtracks = 40
			numsectors = 9
			sectorPow = 2 (512 bytes/sector);
			reservedTracks = 0;
			blockPow = 3 (1024 bytes/block;
			dirBlocks = 2;
			rwGapLength = 0x2a;
			fmtGapLength = 0x52;
   		$01: Speccy +3 / CPC - These have a default set of parameters, but *CAN* be overridden.  As +3 disks have 1 reserver track, this gives 175 for data (Minus the usual 2k)
   			numsides = 1
			numtracks = 40
			numsectors = 9
			sectorPow = 2 (512 bytes/sector);
			reservedTracks = 1;
			blockPow = 3 (1024 bytes/block);
			dirBlocks = 2;
			rwGapLength = 0x2a;
			fmtGapLength = 0x52;
			
		These values can be overridden on the +3 and CPC if there is any data in the bootsector. (Track 0 Head 0 Sector 1) 
		The bootsector is arranged as follows if present. If its full of $e5, the defaults above should be used
		
		00    0 = +3 format 1=CPC system, 2=CPC data, 3=PCW range 
		01    Sidedness: 0=single sided, 1=double sided (Alternative sides) 2=Double sidded (Successive sides)   
   		02    Tracks per side
   		03    Sectors per track
   		04    Sector size shift. 1=256, 2=512, 3=1024 ect
   		05    Number of reserved tracks
   		06    Block size shift 2=512, 3=1024, 4=2048 ect
   		07    Number of directory blocks
   		08    Gap length (RW)
   		09    Gap length(Format)
   		0A-0E reserved
   		0F    Checksum fiddle byte for bootable disks.
   		
   			Note, for the checksum all the bytes in the bootsector are added together (Ignoring any carries).
   			This number should be:  3 = Spectrum +3 bootable disk
   			    					1 = Bootable PCW9512 disk
   			    					255 = Bootable PCW8256 disk
   			 
   			 
 */

import java.io.IOException;
import java.util.ArrayList;

import diskviewer.libs.disk.AMSDiskWrapper;
import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.DiskInfo;
import diskviewer.libs.disk.Sector;
import diskviewer.libs.disk.TrackInfo;

public class CPMDiskWrapper extends AMSDiskWrapper {
	public int disktype = 0; // 0=SS SD 3= DSDD
	public int numsectors = 0; // Sectors per track (9)
	public int sectorPow = 0; // Sector size represented by its (power of 2)+7, (usually 2 meaning 512 bytes)
	public int sectorSize = 0; // Calculated sector size from above (512)
	public int reservedTracks = 0; // Reserved tracks (Usually 1)
	public int blockPow = 0; // Sector size represented by its (power of 2)+7, (usually 3 meaning 1024 bytes)
	public int blockSize = 0; // Calculated Block size from above (1024)
	public int dirBlocks = 0; // reserved blocks for the directory entries (usually 2)
	public int rwGapLength = 0; // read/write gap length
	public int fmtGapLength = 0; // Format gap length
	public int fiddleByte = 0; // Fiddle byte used to make the checksum match
	public int checksum = 0; // calculated checksum. if this make the checksum add up to 3, its a bootable +3
								// disk
	public String diskformat = "";
	public Dirent[] dirents = null; // Directory entries.
	public DirectoryEntry[] DirectoryEntries = null;

	// Calculated fields.
	public int maxblocks = 0; // Max number of blocks on the disk. (Minus the reserved tracks)
	public int reservedblocks = 0; // Blocks reserved for the Directory (Usually 2)
	public int usedblocks = 0; // Blocks that are allocated already
	public int maxDirEnts = 0; // Max number of entries in the directory
	public int usedDirEnts = 0; // Number of entries in the directory map that are used
	public int diskSize = 0; // Calculated max disk space in Kbytes
	public int freeSpace = 0; // Calculated free space in K
	public int BlockIDWidth = 1; // If a disk has > 256 blocks, DIRENTS are 2 bytes rather than 1.

	// Block availability map
	public boolean bam[] = null;

	// This flag is set to FALSE if a loaded disk seems to not appear to be a valid
	// CPM disk.
	// (EG, copy protected disk, ect)
	public boolean IsValidCPMFileStructure = false;

	/**
	 * This function is called to re-calculate the directory listing from the disks
	 * DIRENTS. It is used after the disk is loaded or modified.
	 */
	public void RecalculateDirectoryListing() {
		// Convert the DIRENTS into a directory listing.
		DirectoryEntry direntries[] = new DirectoryEntry[dirents.length]; // number of directory entries cannot be more
																			// than
																			// the DirEnts
		int nextdirentry = 0;
		for (int i = 0; i < dirents.length; i++) {
			Dirent d = dirents[i];
			int dType = d.getType();
			// only care about files
			if (dType == Dirent.DIRENT_FILE || dType == Dirent.DIRENT_DELETED) {
				// do we have a file called that already?
				DirectoryEntry file = null;
				int Directorynum = nextdirentry;
				for (int j = 0; j < nextdirentry; j++) {
					// if we have found the file, record where it is.
					if (direntries[j].filename().equals(d.GetFilename())) {
						file = direntries[j];
						Directorynum = j;
					}
				}
				// If we have not found a file, create a new one.
				if (file == null) {
					file = new DirectoryEntry(this, (dType == Dirent.DIRENT_DELETED), this.maxblocks);
					Directorynum = nextdirentry++;
				}
				file.addDirent(d);
				direntries[Directorynum] = file;
			}
		}
		// now transfer the array to the object
		DirectoryEntries = new DirectoryEntry[nextdirentry];
		for (int i = 0; i < nextdirentry; i++) {
			DirectoryEntries[i] = direntries[i];
		}
		//finally output any errors:
		for(DirectoryEntry d:DirectoryEntries) {
			if (!d.Errors.isEmpty()) {
				System.out.println("Filename: '"+d.filename()+"' - "+d.Errors);
			}
		}
		
	}

	/**
	 * 
	 * load the given disk. This calls the lower level AMS disk loader, then parses
	 * the CPM related information out of it if possible.
	 * 
	 * @param filename
	 */
	public void load(String filename) throws BadDiskFileException, IOException {
		try {
			// load the low level disk information
			super.load(filename);
			// parse High level stuff.
			ParseData();
		} catch (Exception E) {
			E.printStackTrace(System.err);
		}
	}

	/**
	 * This function parses the disk loaded into the track and sector data into
	 * Dirents and other CPM information. If it comes across any problems with the
	 * sector sizes or the lack of DIRENTS it marks it as invalid. If its an Amstrad
	 * style disk, it tries to parse the bootsector DPB information. Failing that,
	 * it defaults to a set of standard defaults for the defined Amstrad disk types.
	 */
	public void ParseData() {
		try {
			TrackInfo Track0 = GetTrack(0, 0);
			Sector BootSect = Track0.GetSectorBySectorID(Track0.minsectorID);
			// Parse out the boot block.
			if (Track0.minsectorID == 1) { // first sector=1 PCW/+3
				// if we have an invalid bootsector fiddle the data
				// fix GDS 22 Dec 2021 - Valid values for byte 1 are 0-3 only. This makes the
				// Khobrasoft SP7 disk load (Fossr some reaosn passed with b0 in sector 1)
				if ((BootSect.data[0] & 0xff) < 4) {
					// bootsector is valid so use that.
					disktype = BootSect.data[0] & 0xff;
					numsides = BootSect.data[1] & 0xff;
					numtracks = BootSect.data[2] & 0xff;
					numsectors = BootSect.data[3] & 0xff;
					sectorPow = BootSect.data[4] & 0xff;
					sectorSize = 128 << sectorPow;
					reservedTracks = BootSect.data[5] & 0xff;
					blockPow = BootSect.data[6] & 0xff;
					blockSize = 128 << blockPow;
					dirBlocks = BootSect.data[7] & 0xff;
					rwGapLength = BootSect.data[8] & 0xff;
					fmtGapLength = BootSect.data[9] & 0xff;
					fiddleByte = (int) (BootSect.data[15] & 0xff);
				} else {
					// Get physical values from the AMS disk wrapper and the count of sectors we
					// actually loaded.
					// For the rest, assume we are +3 disk and use the defaults.
					disktype = 0;
					numsides = this.GetDiskInfo().sides;
					numtracks = this.GetDiskInfo().tracks;
					numsectors = Track0.Sectors.length;
					sectorPow = 2;
					sectorSize = 512;
					reservedTracks = 1;
					blockPow = 3;
					blockSize = 1024;
					dirBlocks = 2;
					rwGapLength = 42;
					fmtGapLength = 82;
					fiddleByte = 0;
				}
				diskformat = "PCW/+3";
			} else if (Track0.minsectorID == 0x41) { // CPC system disk
				diskformat = "CPC System";
				disktype = 0;
				numsides = this.GetDiskInfo().sides;
				numtracks = this.GetDiskInfo().tracks;
				numsectors = Track0.Sectors.length;
				sectorPow = 2;
				sectorSize = 512;
				reservedTracks = 2;
				blockPow = 3;
				blockSize = 1024;
				dirBlocks = 2;
				rwGapLength = 0x2a;
				fmtGapLength = 0x52;
				fiddleByte = 0;
			} else if (Track0.minsectorID == 0xC1) { // CPC data disk. (No boot track)
				diskformat = "CPC Data";
				disktype = 0;
				numsides = this.GetDiskInfo().sides;
				numtracks = this.GetDiskInfo().tracks;
				numsectors = Track0.Sectors.length;
				sectorPow = 2;
				sectorSize = 512;
				reservedTracks = 0;
				blockPow = 3;
				blockSize = 1024;
				dirBlocks = 2;
				rwGapLength = 0x2a;
				fmtGapLength = 0x52;
				fiddleByte = 0;
			}

			// For some reason, this is occasionally not populated.
			if (numsides == 0) {
				numsides = 1;
			}
			DiskInfo di = GetDiskInfo();

			log("CPM data:");
			log("Diskformat: " + diskformat + " type:" + disktype + " sides:" + numsectors + " tracks:" + numtracks
					+ " sectors:" + numtracks + "\r\nSectorPow:" + sectorPow + " SectorSZ:" + sectorSize
					+ " Reserved tracks:" + reservedTracks + " BlocPOW:" + blockPow + " BlockSZ:" + blockSize
					+ "\r\nDir Blocks:" + dirBlocks + " rwGap:" + rwGapLength + " fmtGap:" + fmtGapLength);

			maxblocks = (numtracks - reservedTracks) * di.sides * numsectors * sectorSize / blockSize;
			reservedblocks = dirBlocks;
			maxDirEnts = dirBlocks * blockSize / 32;
			BlockIDWidth = 1;
			if (maxblocks > 255) {
				BlockIDWidth = 2;
			}

			// calculate the checksum
			checksum = 0;
			for (int i = 0; i < BootSect.data.length; i++) {
				int b = (int) BootSect.data[i] & 0xff;
				checksum = (int) (checksum + b) & 0xff;
			}

			// load the directory block
			int track = reservedTracks;
			TrackInfo FirstDirTrack = GetLinearTrack(track);
			int sector = FirstDirTrack.minsectorID;

			IsValidCPMFileStructure = true;
			// +3 disk sectors are always 512. If they are not, something funky is
			// happening so don't try to parse directory entries. So check first 10 or so
			// tracks (To avoid any copy protection on higher tracks)
			for (int tracknum = 0; tracknum < 20; tracknum++) {
				TrackInfo tr = GetLinearTrack(tracknum);
				for (Sector s : tr.Sectors) {
					if (s.ActualSize != 512) {
						IsValidCPMFileStructure = false;
					}
				}
			}
			Sector FirstSector = FirstDirTrack.GetSectorBySectorID(sector);
			if (!IsValidCPMFileStructure)
				System.out.println("Invalid CPM file structure (Sector size not valid for CPM)");

			// As an extra check, make sure there is sensible data in the first directory
			// entry.
			// This is either: all filler bytes, or byte 0=0,1-13 = ascii characters,
			boolean allFiller = true;
			for (int i = 0; i < 32; i++) {
				if (FirstSector.data[i] != FirstDirTrack.fillerByte) {
					allFiller = false;
				}
			}
			if (!allFiller) {
				int firstbyte = FirstSector.data[0] & 0xff;
				// first byte 0-31 = user num of valid file, 0xe5 = deleted file
				if ((firstbyte != 0xe5) && firstbyte > 31) {
					IsValidCPMFileStructure = false;
				} else {

					// Check for a valid filename
					for (int i = 1; i < 12; i++) {
						char c = (char) (FirstSector.data[i] & 0x7f);
						if (c != 0xe5) {
							// Flags appear on bit 7 on the extensions, shouldnt appear elsewere.
							if (((FirstSector.data[i] & 0x80) != 0) && i < 9) {
								IsValidCPMFileStructure = false;
							}
							// Check the rest of the characters
							if (!CPM.CharIsCPMValid(c) && c != ' ')
								IsValidCPMFileStructure = false;
						}
					}

				}
				if (!IsValidCPMFileStructure)
					System.out.println("Invalid CPM file structure (First Directory entry is invalid)");
			}

			// if we havent already decided the disk is invalid, parse out the BAM and
			// DIRENTS.
			if (IsValidCPMFileStructure) {
				int loc = 0;
				// set up the Block availability map.
				bam = new boolean[maxblocks];
				for (int i = 0; i < maxblocks; i++) {
					bam[i] = false;
				}
				// add in the reserved blocks
				for (int i = 0; i < reservedblocks; i++) {
					bam[i] = true;
				}

				int DirectoryBlockSectors = (blockSize / sectorSize) * dirBlocks;

				byte directory[] = new byte[DirectoryBlockSectors * sectorSize];
				for (int i = 0; i < DirectoryBlockSectors; i++) {
					Sector s = GetLinearTrack(track).GetSectorBySectorID(sector);
					// copy sector
					for (byte x : s.data) {
						directory[loc++] = x;
					}
					// select next sector. if we run out of sectors in a track, select the next
					// track.
					sector++;
					if (sector == GetLinearTrack(track).maxsectorID + 1) {
						track++;
						sector = GetLinearTrack(track).minsectorID;
					}
				}
				// now we have the directory block loaded, we need to split it into dirents.
				usedDirEnts = 0;
				int numDirEnts = (DirectoryBlockSectors * sectorSize) / 32;
				dirents = new Dirent[numDirEnts];
				int address = 0;
				for (int i = 0; i < numDirEnts; i++) {
					Dirent d = new Dirent(i);
					d.LoadDirentFromArray(directory, address);
					d.Is16BitBlockID = (maxblocks > 255);
					dirents[i] = d;
					if ((d.getType() != Dirent.DIRENT_UNUSED) && (d.getType() != Dirent.DIRENT_DELETED)) {
						usedDirEnts++;
					}
					address = address + 32;
				}

				// Convert the DIRENTS into a directory listing.
				RecalculateDirectoryListing();

				// calculate the disk size in Kbytes
				diskSize = blockSize * (maxblocks - reservedblocks) / 1024;

				// populate the BAM from the dirents
				// firstly from the reserved (Directory) blocks
				usedblocks = reservedblocks;
				for (int i = 0; i < reservedblocks; i++) {
					bam[i] = true;
				}
				// now from the files
				for (Dirent d : dirents) {
					int blocks[] = d.getBlocks();
					boolean isdeleted = (d.getType() == Dirent.DIRENT_DELETED);
					if (!isdeleted) {
						for (int i : blocks) {
							//used to output an error here, but detecting invalid blocks
							//is now done by the directory structure. 
							if (i < bam.length) {
								bam[i] = true;
							}
							usedblocks++;
						}
					}
				}

				// calculate the space left.
				freeSpace = (maxblocks - usedblocks) * blockSize / 1024;
			} else {
				DirectoryEntries = null;
				bam = null;
				usedblocks = 0;
				dirBlocks = 0;
				freeSpace = 0;
			}
		} catch (Exception E) {
			E.printStackTrace(System.err);
		}

	}

	/**
	 * Get the given block
	 * 
	 * @param blockid
	 * @return
	 * @throws BadDiskFileException
	 */
	public byte[] GetBlock(int blockid) throws BadDiskFileException {
		byte result[] = new byte[blockSize];
		if (blockid >= maxblocks) {
			byte[] txt = "INVALID ".getBytes();
			int i=0;
			int ptr=0;
			while (ptr < blockSize) {
				result[ptr++] = txt[i++];
				if (i==txt.length) {
					i = 0;
				}
			}
		} else {

			int sectorsPerBlock = blockSize / sectorSize;
			// Convert the block into a sector
			int logicalsector = blockid * sectorsPerBlock;

			// calculate the cs. Note, we dont need to bother calculating head because its
			// already head order
			int track = (logicalsector / numsectors) + reservedTracks;
			track = track / GetDiskInfo().sides;
			int sector = (logicalsector % numsectors);

			// Copy each sector to the block
			int resultPtr = 0;
			for (int i = 0; i < sectorsPerBlock; i++) {
				// get the current sector
				TrackInfo CurrTrack = GetLinearTrack(track);
				Sector CurrSector = CurrTrack.GetSectorBySectorID(sector + CurrTrack.minsectorID);
				// Copy the data
				for (byte x : CurrSector.data) {
					result[resultPtr++] = x;
				}
				// Select the next sector.
				sector++;
				if (sector == CurrTrack.Sectors.length) {
					track++;
					sector = 0;
				}
			}
		}
		return (result);
	}

	/**
	 * Write the numbered block.
	 * 
	 * @param blockid
	 * @param rawbytes
	 * @throws BadDiskFileException
	 */
	public void WriteBlock(int blockid, byte[] rawbytes, int start) throws BadDiskFileException {
		int sectorsPerBlock = blockSize / sectorSize;
		// Convert the block into a sector
		int logicalsector = blockid * sectorsPerBlock;

		// calculate the cs. Note, we dont need to bother calculating head because its
		// already head order
		int track = (logicalsector / numsectors) + reservedTracks;
		track = track / GetDiskInfo().sides;
		int sector = (logicalsector % numsectors);
		// now write the sectors
		int ptr = start;
		for (int i = 0; i < sectorsPerBlock; i++) {
			// get the current sector
			TrackInfo CurrTrack = GetLinearTrack(track);
			Sector CurrSector = CurrTrack.GetSectorBySectorID(sector + CurrTrack.minsectorID);
			// Copy the data
			for (int j = 0; j < CurrSector.data.length; j++) {
				if (ptr == rawbytes.length) {
					CurrSector.data[j] = (byte) 0xe5;
				} else {
					CurrSector.data[j] = rawbytes[ptr++];
				}
			}
			// Select the next sector.
			sector++;
			if (sector == CurrTrack.Sectors.length) {
				sector = 0;
				track++;
			}
		}
	}

	/**
	 * This saves a file in the CPM format. Any higher level structures that +3DOS
	 * or AMSDOS required are added before passing to this.
	 * 
	 * @param filename
	 * @param rawbytes
	 * @return
	 * @throws BadDiskFileException
	 */
	public boolean SaveCPMFile(String filename, byte[] rawbytes) {
		// firstly check to see if the file exists. delete the old one if found.
		DirectoryEntry de = GetDirectoryEntry(filename);
		if (de != null) {
			de.SetDeleted(true);
			log("SaveCPMFile: Deleted the old version of " + filename);
		}

		try {
			log("SaveCPMFile: Saving " + filename + " length:" + rawbytes.length);
			// do we have enough space on the disk?
			if (rawbytes.length > freeSpace * 1024) {
				System.out.println("insufficient free space on disk. (" + String.valueOf(freeSpace * 1024)
						+ " bytes free, " + rawbytes.length + " bytes required)");
				return (false);
			}
			// Work out how many blocks the file will need
			int requiredblocks = Math.floorDiv(rawbytes.length, blockSize);
			if (rawbytes.length % blockSize > 0) {
				requiredblocks++;
			}
			log("Blocks required: " + requiredblocks);

			// Work out how many DIRENTS this will need
			int blocksPerDirent = 16 / BlockIDWidth;
			int direntsRequired = Math.floorDiv(requiredblocks, blocksPerDirent);
			if (requiredblocks % blocksPerDirent > 0) {
				direntsRequired++;
			}
			log("Dirents required: " + direntsRequired);

			// do we have enough dirents?
			int freedirents = maxDirEnts - usedDirEnts;
			if (direntsRequired > freedirents) {
				System.out.println("insufficient free directory entries. (" + String.valueOf(direntsRequired)
						+ " entries required, " + freedirents + " entries free)");
				return (false);
			}

			// Get a list of free blocks
			ArrayList<Integer> FreeBlocks = new ArrayList<Integer>();
			for (int i = reservedblocks; i < bam.length; i++) {
				if (!bam[i]) {
					FreeBlocks.add(i);
				}
			}
			log("Free blocks: " + FreeBlocks.size());

			// write the blocks in order and allocate BAM entries.
			ArrayList<Integer> NewBlocks = new ArrayList<Integer>();
			int ptr = 0;
			for (int i = 0; i < requiredblocks; i++) {
				int block = FreeBlocks.get(i);
				bam[block] = true;
				NewBlocks.add(block);
				WriteBlock(block, rawbytes, ptr);
				ptr = ptr + blockSize;
			}

			// Get a list of free dirents
			ArrayList<Integer> FreeDirents = new ArrayList<Integer>();
			for (int i = 0; i < dirents.length; i++) {
				int typ = dirents[i].getType();
				if (typ == Dirent.DIRENT_DELETED || typ == Dirent.DIRENT_UNUSED) {
					FreeDirents.add(i);
				}
			}
			log("Free Dirents: " + FreeDirents.size());

			// seperate the filename
			filename = filename.toUpperCase();
			String prefix = "   ";
			if (filename.indexOf('.') > -1) {
				prefix = filename.substring(filename.indexOf('.') + 1) + "   ";
				filename = filename.substring(0, filename.indexOf('.')) + "        ";
			}

			// write the dirents
			int block = 0;
			for (int direntNum = 0; direntNum < direntsRequired; direntNum++) {
				int allocatedDirent = FreeDirents.get(direntNum);
				Dirent d = dirents[allocatedDirent];

				// allocated file
				d.rawdirent[0] = 0;

				// filenames
				for (int j = 0; j < 8; j++) {
					d.rawdirent[j + 1] = (byte) filename.charAt(j);
				}
				for (int j = 0; j < 3; j++) {
					d.rawdirent[j + 9] = (byte) prefix.charAt(j);
				}

				// extent lower 4 bits
				d.rawdirent[0x0c] = (byte) (direntNum % 0x0f);

				// number of bytes used in the last dirent with 0=128
				d.rawdirent[0x0d] = (byte) (rawbytes.length & 0x7f);

				// Lower 5 bits are the upper bits of the extent number
				d.rawdirent[0x0e] = (byte) (direntNum / 0x10);

				// Number of 128 byte records used in the last logical extent. All previous
				// extents are considered to be full.
				int bytesPerDirent = blocksPerDirent * blockSize;
				int LastDirentBytes = rawbytes.length % bytesPerDirent; // extract the bytes in the last dirent.
				int recordsInLastDirent = LastDirentBytes / 128;
				if (rawbytes.length % bytesPerDirent != 0) {
					recordsInLastDirent++;
				}
				d.rawdirent[0x0f] = (byte) recordsInLastDirent;

				// copy block numbers to the DIRENTS
				for (int j = 0; j < blocksPerDirent; j++) {
					int blocknum = 0;
					if (block < NewBlocks.size()) {
						blocknum = NewBlocks.get(block++);
					}
					if (BlockIDWidth == 1) {
						d.rawdirent[j + 0x10] = (byte) (blocknum & 0xff);
					} else {
						int index = (j * 2) + 0x10;
						d.rawdirent[index] = (byte) (blocknum & 0xff);
						d.rawdirent[index + 1] = (byte) (blocknum / 0x100);
					}
				}
				if (VerboseMode) {
					int blocks[] = d.getBlocks();
					String s = "";
					for (int blk : blocks) {
						s = s + ", ";
						s = s + blk;
					}
					log("SaveCPMFile: Dirent: " + d.entrynum + " <- " + d.GetLogicalExtentNum() + " Blocks: "
							+ s.substring(2));
				}

			}
			// update sectors containing the dirents
			DirentsToSectors();

			// Add a directory entry.
			RecalculateDirectoryListing();

			// set modified
			setModified(true);

			// update free space markers
			usedDirEnts = usedDirEnts + direntsRequired;
			usedblocks = usedblocks + requiredblocks;
			freeSpace = (maxblocks - usedblocks) * blockSize / 1024;
			return (true);
		} catch (Exception E) {
			E.printStackTrace();
			return (false);
		}
	}

	/**
	 * Write the entire dirent storage back to their original sectors. Used after
	 * saving, deleting, renaming or undeleting a file.
	 * 
	 */
	private void DirentsToSectors() {
		int track = reservedTracks;
		TrackInfo direntTrack = GetLinearTrack(track);
		int sectorID = direntTrack.minsectorID;
		int sectorptr = 0;

		Sector sector = GetLinearTrack(track).GetSectorBySectorID(sectorID);

		for (Dirent d : dirents) {
			for (int i = 0; i < 0x20; i++) {
				sector.data[sectorptr++] = d.rawdirent[i];
			}
			if (sectorptr >= sectorSize) {
				// Select the next sector.
				sectorptr = 0;
				sectorID++;
				if (sectorID > direntTrack.maxsectorID) {
					track++;
					direntTrack = GetLinearTrack(track);
					sectorID = direntTrack.minsectorID;
				}
				sector = direntTrack.GetSectorBySectorID(sectorID);
			}
		}
	}

	/**
	 * Check to see if a file exists and return its directory entry if found
	 * 
	 * @param filename
	 * @return
	 */
	public DirectoryEntry GetDirectoryEntry(String filename) {
		DirectoryEntry result = null;
		for (DirectoryEntry d : DirectoryEntries) {
			if (d.filename().contentEquals(filename)) {
				result = d;
			}
		}

		return (result);
	}

	/**
	 * Create a blank CPM disk.
	 * 
	 * @param tracks
	 * @param heads
	 * @param spt
	 * @param minsector
	 * @param IsExtended
	 * @param fillerbyte
	 * @param ADFHeader
	 * @param bootSector
	 */
	public void CreateCPMDisk(int tracks, int heads, int spt, int minsector, boolean IsExtended, int fillerbyte,
			String ADFHeader, byte[] bootSector) {
		CreateAMSDisk(tracks, heads, spt, minsector, IsExtended, ADFHeader, fillerbyte);
		// write the bootsector
		if (bootSector != null) {
			TrackInfo track00 = GetLinearTrack(0);
			Sector s = track00.GetSectorBySectorID(track00.minsectorID);
			for (int i = 0; i < bootSector.length; i++) {
				s.data[i] = bootSector[i];
			}
		}
		// Now parse all the information into Dirents.
		ParseData();
	}

	/**
	 * Search for files given the CPM wildcard (?= any character, *=any to end)
	 * 
	 * @param wildcard
	 * @return
	 */
	public DirectoryEntry[] FetchDirEntries(String wildcard) {
		ArrayList<DirectoryEntry> results = new ArrayList<DirectoryEntry>();
		// convert the wildcard into a search array:
		// Split into filename and extension. pad out with spaces.
		String fname = wildcard.toUpperCase();
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
		extension = extension + "   ";

		// create search array.
		byte comp[] = new byte[12];

		// populate with filename
		boolean foundstar = false;
		for (int i = 0; i < 8; i++) {
			if (foundstar) {
				comp[i] = '?';
			} else {
				char c = filename.charAt(i);
				if (c == '*') {
					foundstar = true;
					comp[i] = '?';
				} else {
					comp[i] = (byte) ((int) c & 0xff);
				}
			}
		}

		// populate with extension
		foundstar = false;
		for (int i = 0; i < 3; i++) {
			if (foundstar) {
				comp[i + 8] = '?';
			} else {
				char c = extension.charAt(i);
				if (c == '*') {
					foundstar = true;
					comp[i + 8] = '?';
				} else {
					comp[i + 8] = (byte) ((int) c & 0xff);
				}
			}
		}

		// now search.
		for (DirectoryEntry de : DirectoryEntries) {
			// check the filename
			boolean match = true;
			for (int i = 0; i < 11; i++) {
				byte chr = de.dirents[0].rawdirent[i + 1];
				byte cchr = comp[i];
				if ((chr != cchr) && (cchr != '?')) {
					match = false;
				}
			}
			if (match) {
				results.add(de);
			}
		}

		DirectoryEntry res[] = new DirectoryEntry[results.size()];
		res = results.toArray(res);
		return (res);
	}

}
