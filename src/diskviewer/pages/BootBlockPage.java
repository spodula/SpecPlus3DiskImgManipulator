package diskviewer.pages;

/**
 * This page tries to decode the boot block. It will also check to see if its bootable and 
 * will try to disassemble the boot code.
 */

import diskviewer.libs.ASMLib;
import diskviewer.libs.ASMLib.DecodedASM;
import diskviewer.libs.disk.Sector;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;

public class BootBlockPage extends page {
	public String get(PlusThreeDiskWrapper disk) {

		StringBuilder result = new StringBuilder();
		
		result.append(DEFAULT_TABLE_STYLESHEET + HEX_TABLE_STYLESHEET + "\r\n" +
					"<h1>Boot block</h1>\r\n" +
					"<table>\r\n");

		// All values except 0 and 3 are invalid.
		String disktype = "Not set";
		if (disk.disktype == 3) {
			disktype = "Double sided, Double density";
		} else if (disk.disktype == 3) {
			disktype = "Single sided, Single density";
		}
		result.append( "<tr><td><b>Hard Format type</b></td><td>" + disktype + "</td></tr>\r\n" );
		result.append( "<tr><td><b>Sides</b></td><td>" + String.valueOf(disk.numsides) + "</td></tr>\r\n");
		result.append( "<tr><td><b>Tracks per side</b></td><td>" + String.valueOf(disk.numtracks)
				+ "</td></tr>\r\n");
		result.append( "<tr><td><b>Sectors per track</b></td><td>" + String.valueOf(disk.numsectors)
				+ "</td></tr>\r\n");
		result.append( "<tr><td><b>Log2(sz)-7 (sector size)</b></td><td>"
				+ String.valueOf(disk.sectorPow) + " (" + disk.sectorSize + "  bytes/sector) </td></tr>\r\n");
		result.append( "<tr><td><b>Reserved blocks</b></td><td>" + String.valueOf(disk.reservedTracks)
				+ "</td></tr>\r\n");
	
		result.append( "<tr><td><b>Block size</b></td><td>" + String.valueOf(disk.blockPow)+" ("+String.valueOf(disk.blockSize)+" bytes block size)" + "</td></tr>\r\n");
		result.append( "<tr><td><b>No directory blocks</b></td><td>" + String.valueOf(disk.dirBlocks)
				+ "</td></tr>\r\n");
		result.append( "<tr><td><b>read/write gap length</b></td><td>" + String.valueOf(disk.rwGapLength)
				+ "</td></tr>\r\n");
		result.append( "<tr><td><b>format gap length</b></td><td>" + String.valueOf(disk.fmtGapLength)
				+ "</td></tr>\r\n");
		result.append( "<tr><td><b>Checksum fiddle byte</b></td><td>"
				+ String.valueOf(disk.fiddleByte) + "</td></tr>\r\n");
		// make into text (Note, don't really care about PCW disks in this context, but
		// its free information, so why not).
		String csumtype = "Not bootable";
		if (disk.checksum == 3) {
			csumtype = "Bootable Spectrum +3 disk";
		} else if (disk.checksum == 1) {
			csumtype = "Bootable PCW9512 disk";
		} else if (disk.checksum == 255) {
			csumtype = "Bootable PCW8256 disk";
		}
		result.append( "<tr><td><b>Checksum:</b></td><td>" + csumtype + " (#" + String.valueOf(disk.checksum)
				+ ")" + "</td></tr>\r\n");
		result.append( "</table>\r\n<br>\r\n");

		// Try to disassemble the boot sector.
		Sector BootSect = disk.Tracks[0].Sectors[0];
		result.append( "<table class=\"hex\">\r\n");
		result.append( "<tr><th colspan=4>" + "Disassembled boot block " + "</td></tr>\r\n");
		result.append( "<tr><th>Loaded address</th><th>Hex</th><th>Asm</th><th>chr</th></tr>\r\n");
		ASMLib asm = new ASMLib();
		int loadedaddress = 0xfe10;
		int realaddress = 0x0010;
		int data[] = new int[5];
		while (realaddress < 0x200) {
			String chrdata = "";
			for (int i = 0; i < 5; i++) {
				int d = 0;
				if (realaddress + i < 512) {
					d = (int) BootSect.data[realaddress + i] & 0xff;
				}
				data[i] = d;

				if ((d > 0x1F) && (d < 0x7f)) {
					chrdata = chrdata + (char) d;
				} else {
					chrdata = chrdata + "?";
				}
			}
			// decode instruction
			DecodedASM Instruction = asm.decode(data, loadedaddress);
			// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
			// (4x3)
			String hex = "";
			for (int j = 0; j < Instruction.length; j++) {
				hex = hex + toHexTwoDigit(data[j]) + " ";
			}
			while (hex.length() < 12) {
				hex = hex + "   ";
			}
			result.append( "<tr><td>" + IntAndHex(loadedaddress) + "</td><td>" + hex + "</td><td>"
					+ Instruction.instruction + "</td><td>" + chrdata.substring(0, Instruction.length)
					+ "</td></tr>\r\n");

			realaddress = realaddress + Instruction.length;
			loadedaddress = loadedaddress + Instruction.length;

		}
		result.append( "</table>\r\n<br>\r\n");

		// Output the raw sector data.

		result.append( "<table class=\"hex\">\r\n" +
						"<tr><th colspan=3>" + "<b>Raw Boot block data</b> " + "</td></tr>\r\n");
		// sector data
		String hexdata = "";
		String chardata = "";
		String starttext = "000-" + toHexThreeDigit(31);
		for (int i = 0; i < BootSect.data.length; i++) {
			if ((i % 32) == 0 && (i > 0)) {
				result.append( "<tr><td>" + starttext + "</td><td>" + hexdata + "</td><td>" + chardata
						+ "</td></tr>\r\n");
				hexdata = "";
				chardata = "";
				starttext = toHexThreeDigit(i) + "-" + toHexThreeDigit(i + 31);
			}
			hexdata = hexdata + " " + toHexTwoDigit(BootSect.data[i]);
			if ((BootSect.data[i] > 0x1F) && (BootSect.data[i] < 0x7f)) {
				chardata = chardata + (char) BootSect.data[i];
			} else {
				chardata = chardata + "?";
			}

		}
		result.append( "<tr><td>" + starttext + "</td><td>" + hexdata + "</td><td>" + chardata + "</td></tr>\r\n" +
		               "</table>\r\n<br>\r\n");

		return (result.toString());
	}

}
