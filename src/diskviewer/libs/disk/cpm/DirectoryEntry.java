package diskviewer.libs.disk.cpm;

/**
 * Object wrapping a logical directory entry.
 *
 * This consists of one or more dirents (See dirent.java) 
 */

import java.util.ArrayList;

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.TrackInfo;

public class DirectoryEntry {
	// the raw dirents associated with this entry
	public Dirent[] dirents = null;

	// The disk its on.
	private CPMDiskWrapper ThisDisk = null;

	// is file deleted
	public boolean IsDeleted = false;
	
	//if TRUE, the directory entry is invalid.
	public boolean BadDirEntry = false;
	
	//used to validate Dirent. 
	private int maxBlocks=0;
	
	public String Errors="";
	
	/**
	 * Parse and return the filename from the first DIRENT.
	 * 
	 * @return
	 */
	public String filename() {
		if (dirents != null) {
			return (dirents[0].GetFilename());
		} else {
			return ("");
		}
	}

	/**
	 * Create the directory entry.
	 * 
	 * @param filename
	 * @param disk
	 */
	DirectoryEntry(CPMDiskWrapper disk, boolean IsDeleted, int maxBlocks) {
		this.ThisDisk = disk;
		this.IsDeleted = IsDeleted;
		dirents = new Dirent[0];
		this.maxBlocks = maxBlocks;
	}

	/**
	 * Add a DIRENT to the file.
	 * 
	 * @param d
	 */
	public void addDirent(Dirent d) {
		// Duplicate the dirent list and add the new one.
		Dirent[] newdirent = new Dirent[dirents.length + 1];
		for (int i = 0; i < dirents.length; i++) {
			newdirent[i] = dirents[i];
		}
		newdirent[dirents.length] = d;
		dirents = newdirent;
		//force a recalculation of if the file is valid. 
		getBlocks();
	}

	/**
	 * Get the dirent by number. Note this is required because i can't be certain
	 * DIRENTS are actually in order. (They may be, but i can't find any
	 * documentation saying so either way)
	 * 
	 * @param num
	 * @return
	 */
	public Dirent getExtentByNum(int num) {
		Dirent result = null;
		for (Dirent d : dirents) {
			if (d.GetLogicalExtentNum() == num) {
				result = d;
			}
		}
		return (result);
	}

	/**
	 * Get the list of blocks in the filename. Note we are doing it using a sub
	 * function to get the numbered extent, rather than just iterating because
	 * although all the disks i have looked at so far put the dirents for a given
	 * file consecutively, i can't find anything in any documentation that actually
	 * says this.
	 * 
	 * Added GDS 06 feb 2022 - Added code to check for bad blocks and extents and 
	 *  report them and properly ignore them. 
	 *  This is to fix loading of the Double Dragon disk which seems
	 *  to rely on this for some form of copy protection.
	 * 
	 * @return
	 */
	public int[] getBlocks() {
		BadDirEntry = false;
		ArrayList<Integer> al = new ArrayList<Integer>();
		String badExtents="";
		
		String badBlocks="";
		
		for (int i = 0; i < dirents.length; i++) {
			Dirent d = getExtentByNum(i);
			if (d == null) {
				badExtents = badExtents+", "+i;
			} else {
				int blocks[] = d.getBlocks();
				for (int block : blocks) {
					if (block != 0) {
						if (block >= maxBlocks) {
							badBlocks = badBlocks+", "+block;
						} 
						al.add(block);
					}
				}
			}
		}
		
		if (!badExtents.isEmpty()) {
			Errors = "File is invalid due to missing (Or re-used) directory extents: #"+badExtents.substring(2);
			BadDirEntry = true;
		}
		if (!badBlocks.isEmpty()) {
			if (!Errors.isBlank()) {
				Errors= Errors+"\r\n";
			}
			Errors= Errors + "File is invalid due to referencing invalid blocks: #"+badBlocks.substring(2);
			BadDirEntry = true;
		}

		// convert the arraylist into a int[] to return
		int[] result = new int[al.size()];
		for (int i = 0; i < al.size(); i++) {
			result[i] = al.get(i);
		}
		return (result);

	}

	/**
	 * Get the number of bytes (Note, a multiple of 128 bytes) file size on disk.
	 * 
	 * @return
	 */
	public int GetFileSize() {
		// As all except the last dirent will be a full 16k (16 blocks, 1k blocks)
		int filesize = (dirents.length - 1) * 16 * ThisDisk.blockSize;

		// Get the number of records used in the last dirent.
		Dirent lastdirent = getExtentByNum(dirents.length - 1);
		int bytesinllb = 0;
		if (lastdirent == null) {
			System.out.println("Cant get last dirent for " + filename());
		} else {
			bytesinllb = lastdirent.GetBytesInLastLogicalBlock();
		}
		return (bytesinllb + filesize);

	}

	/**
	 * Get the file content
	 * 
	 * @return
	 * @throws BadDiskFileException
	 */
	public byte[] GetFileData() throws BadDiskFileException {
		byte result[] = new byte[GetFileSize()];

		// get all the blocks
		int[] blocks = getBlocks();

		// find the last valid byte
		int eob = GetFileSize();

		// iterate each block
		int resultptr = 0;
		for (int i = 0; i < blocks.length; i++) {
			byte currentblock[] = ThisDisk.GetBlock(blocks[i]);
			// copy the contents until we get to end last record in the block. (The rest of
			// the data is invalid)
			for (byte x : currentblock) {
				if (resultptr < eob) {
					result[resultptr++] = x;
				}
			}
		}
		return (result);
	}

	/**
	 * parse the first block into a +3Dos header structure and return it.
	 * 
	 * @return
	 */
	public Plus3DosFileHeader GetPlus3DosHeader() {

		// Load the first block of the file
		Plus3DosFileHeader pdh = null;
		int[] blocks = getBlocks();
		// this fix an issue with zero length CPM files.
		// we will just return an invalid +3 data structure.
		// Eg, the alcatraz development disks, "New word" side A
		if (blocks.length ==0) {
			pdh = new Plus3DosFileHeader(new byte[256]);
		} else {
			byte Block0[] = null;
			try {
				if (blocks[0] >= ThisDisk.maxblocks) {
					//added for Double Dragon which has a directory entry 
					//with bad Block numbers, this prevents most of the files
					//appearing in the directory listing. 
					System.out.println("Block "+blocks[0]+" does not exist for entry: '"+filename()+"'");
					pdh = new Plus3DosFileHeader(new byte[256]);
				} else {
					Block0 = ThisDisk.GetBlock(blocks[0]);
					pdh = new Plus3DosFileHeader(Block0);					
				}
			} catch (BadDiskFileException e) {
				System.out.println("Cannot read first block of " + filename() + ".\r\n" + e.getMessage());
			}
		}
		return (pdh);
	}

	/**
	 * Check to see if the current directory entry is a complete file. Only applies
	 * to deleted files, Other files are assumed to be complete if the entries are complete.
	 * 
	 * @return
	 */
	public Boolean IsComplete() {
		if (!IsDeleted) {
			return (!BadDirEntry);
		} else {
			// Check to see if any of the blocks are marked as in-use by the BAM.
			boolean result = true;
			int blocks[] = getBlocks();
			for (int i : blocks) {
				if (i<ThisDisk.bam.length ) {
					if (ThisDisk.bam[i])
						result = false;
				} else {
					result = false;
				}
			}
			return (result);
		}
	}

	/**
	 * Set the file to be deleted or not deleted.
	 * 
	 * @param deleted
	 */
	public void SetDeleted(boolean deleted) {
		// first, set all the dirents.
		for (Dirent d : dirents) {
			if (deleted) {
				d.setType(Dirent.DIRENT_DELETED);
			} else {
				d.setType(Dirent.DIRENT_FILE);
			}
		}
		// update the deleted flag
		IsDeleted = deleted;
		// fix sectors
		updateDirentSector();
		// update the BAM.
		int blocks[] = getBlocks();
		for (int i = 0; i < blocks.length; i++) {
			int blocknum = blocks[i];
			if (deleted) {
				ThisDisk.bam[blocknum] = false;
			} else {
				if (ThisDisk.bam[blocknum]) {
					System.out.println("Warning! Block " + blocknum + " Already in use!");
				} else {
					ThisDisk.bam[blocknum] = true;
				}
			}
		}
		ThisDisk.setModified(true);
	}

	/**
	 * set the write protect / system / archive flag
	 * 
	 * @param value - new flag value
	 * @param flag  - which flag (R/S/A)
	 */ 
	public void SetFlag(boolean value, char flag) {
		int bytenum = "RSArsa".indexOf(flag);
		if (bytenum > 2)
			bytenum = bytenum - 3;
		if (bytenum == -1)
			System.out.println("Unknown attribute: " + flag);
		else {
			// set the dirents
			bytenum = bytenum + 9;
			for (Dirent d : dirents) {
				if (value) {
					d.rawdirent[bytenum] = (byte) ((int) (d.rawdirent[bytenum] | 0x80) & 0xff);
				} else {
					d.rawdirent[bytenum] = (byte) ((int) (d.rawdirent[bytenum] & 0x7f));
				}
			}
		}

		// fix sectors
		updateDirentSector();
		ThisDisk.setModified(true);

	}

	/**
	 * update the sector containing the BAM
	 */
	private void updateDirentSector() {
		// update the sectors.
		int DirentsPerSector = ThisDisk.sectorSize / 32;
		for (Dirent d : dirents) {
			int sectornum = d.entrynum / DirentsPerSector;
			int locationwithinsector = (d.entrynum % DirentsPerSector) * 32;
			// Assumption: There are always the same number of sectors per track.
			int track = ThisDisk.reservedTracks;
			while (sectornum > ThisDisk.numsectors) {
				track++;
				sectornum = sectornum - ThisDisk.numsectors;
			}
			TrackInfo tr = ThisDisk.GetLinearTrack(track);
			int sectorindex = -1;
			for (int i = 0; i < tr.Sectors.length; i++) {
				if (tr.Sectors[i].sectorID == sectornum + tr.minsectorID) {
					sectorindex = i;
				}
			}
			if (sectorindex == -1) {
				System.out.println("Cannot find sector!");
			} else {
				tr.Sectors[sectorindex].data[locationwithinsector] = d.rawdirent[0];
			}
		}
	}

	/**
	 * Rename the current file
	 * 
	 * @param newFilename
	 */
	public void RenameTo(String newFilename) {
		// first, set all the dirents.
		for (Dirent d : dirents) {
			d.SetFilename(newFilename);
		}
		// update the deleted flag
		// update the sectors.
		int DirentsPerSector = ThisDisk.sectorSize / 32;
		for (Dirent d : dirents) {
			int sectornum = d.entrynum / DirentsPerSector;
			int locationwithinsector = (d.entrynum % DirentsPerSector) * 32;
			System.out.println("Dirent: " + d.entrynum + "Updating s" + sectornum + " loc " + locationwithinsector);
			// Assumption: There are always the same number of sectors per track.
			int track = ThisDisk.reservedTracks;
			while (sectornum > ThisDisk.numsectors) {
				track++;
				sectornum = sectornum - ThisDisk.numsectors;
			}
			TrackInfo tr = ThisDisk.GetLinearTrack(track);
			int sectorindex = -1;
			for (int i = 0; i < tr.Sectors.length; i++) {
				if (tr.Sectors[i].sectorID == sectornum + tr.minsectorID) {
					sectorindex = i;
				}
			}
			if (sectorindex == -1) {
				System.out.println("Cannot find sector!");
			} else {
				tr.Sectors[sectorindex].data[locationwithinsector] = d.rawdirent[0];
			}
		}
		ThisDisk.setModified(true);

	}
}
