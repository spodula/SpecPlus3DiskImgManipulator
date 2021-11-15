package diskviewer.libs.disk.cpm;
/**
 * This object handles the +3 specific handling of the CPM files.
 * It has functions to package the given files with the +3DOS header
 * and specific breakout functions to save two of the four file types (BASIC, CODE)
 */

import java.io.File;
import java.io.FileInputStream;

import java.io.InputStream;



public class PlusThreeDiskWrapper extends CPMDiskWrapper {

	final int PLUS3ISSUE = 1;
	final int PLUS3VERSION = 0;

	final int BASIC_BASIC = 0x00;
	final int BASIC_NUMARRAY = 0x01;
	final int BASIC_CHRARRAY = 0x02;
	final int BASIC_CODE = 0x03;
	final byte stdheader[] = { 'P', 'L', 'U', 'S', '3', 'D', 'O', 'S', 0x1a, PLUS3ISSUE, PLUS3VERSION };

	/**
	 * Add a Headerless file from the given filename. This is just a file without
	 * the +3DOS header
	 * 
	 * @param realfile
	 * @param filename
	 */
	public void AddHeaderlessFile(String realfile, String filename) {
		try {
			File f = new File(realfile);
			int filelen = (int) f.length();

			byte rawbytes[] = new byte[filelen];
			// Load file to memory.
			InputStream in = new FileInputStream(f);
			try {
				for (int i = 0x80; i < rawbytes.length; i++) {
					int chr = in.read();
					rawbytes[i] = (byte) (chr & 0xff);
				}
			} finally {
				in.close();
			}

			log("AddCodeFile: Saving Headerless file with length: " + filelen + " Data: " + rawbytes.length);
			SaveCPMFile(filename, rawbytes);

		} catch (Exception E) {
			E.printStackTrace();
		}
	}

	/**
	 * Save a passed in data with the given filename. as CODE
	 * 
	 * @param filename
	 * @param address
	 * @param data
	 */
	public void AddRawCodeFile(String filename, int address, byte[] data) {
		AddPlusThreeFile(filename, data, address, 0, BASIC_CODE);
	}

	
	/**
	 * 
	 * @param nameOnDisk
	 * @param basicAsBytes
	 * @param line
	 * @param basicoffset
	 */
	public void AddBasicFile(String nameOnDisk, byte[] basicAsBytes, int line, int basicoffset) {
		AddPlusThreeFile(nameOnDisk, basicAsBytes, line, basicoffset,BASIC_BASIC);
	}
	
	/**
	 * Add a given file as a +3DOS file. 
	 * 
	 * @param nameOnDisk
	 * @param bytes
	 * @param Var1
	 * @param Var2
	 * @param type
	 */
	public void AddPlusThreeFile(String nameOnDisk, byte[] bytes, int Var1, int Var2, int type) {
		try {
			int cpmlen = bytes.length + 0x80;
			// allocate memory for filename and +3Dos header
			byte rawbytes[] = new byte[cpmlen];
			// Load file to memory.
			for (int i = 0; i < bytes.length; i++) {
				rawbytes[i + 0x80] = bytes[i];
			}

			// Make the +3DOS header
			for (int i = 0; i < stdheader.length; i++) {
				rawbytes[i] = stdheader[i];
			}

			log("AddPlusThreeFile: filelen:" + bytes.length + " cpmlen: " + cpmlen);

			// Add in the file size
			for (int i = 0; i < 4; i++) {
				int byt = (cpmlen & 0xff);
				rawbytes[i + 11] = (byte) (byt & 0xff);
				cpmlen = cpmlen / 0x100;
			}
			// Now the +3 basic header
			rawbytes[15] = (byte) (type & 0xff);
			rawbytes[16] = (byte) ((bytes.length & 0xff) & 0xff);
			rawbytes[17] = (byte) ((bytes.length / 0x100) & 0xff);
			rawbytes[18] = (byte) ((Var1 & 0xff) & 0xff);
			rawbytes[19] = (byte) ((Var1 / 0x100) & 0xff);
			rawbytes[20] = (byte) ((Var2 & 0xff) & 0xff);
			rawbytes[21] = (byte) ((Var2 / 0x100) & 0xff);

			// Calculate the checksum.
			int checksum = 0;
			for (int i = 0; i < 127; i++) {
				checksum = checksum + (rawbytes[i] & 0xff);
			}
			rawbytes[127] = (byte) (checksum & 0xff);

			log("AddRawCodeFile: Saving file with basic length: " + bytes.length
						+ " CPM:" + cpmlen + " Data: " + rawbytes.length+" checksum: "+checksum+" val:"+rawbytes[127]);
			SaveCPMFile(nameOnDisk, rawbytes);
			
			int ptr=0;
			int eol=0;
			char hexstr[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
			while (ptr < rawbytes.length) {
				int msb = (rawbytes[ptr] & 0xf0) / 16;
				int lsb = (rawbytes[ptr] & 0x0f);
				char result[] = { hexstr[msb], hexstr[lsb],' '};
				ptr++;
				System.out.print(result);
				eol++;
				if (eol==16) {
					eol=0;
					System.out.println();
				}
			}
		} catch (Exception E) {
			E.printStackTrace();
		}

	}

	/**
	 * Create a blank disk.
	 * @param result
	 */
	public void CreateDisk(dialogs.NewDiskDialog.disktype result) {
		CreateCPMDisk(result.Track, result.Heads,result.Sectors, result.MinSector, result.IsExtended, result.filler, result.Header, result.BootSector);
		//fix the +3 information
	}

}