package diskviewer.pages;

import diskviewer.libs.ASMLib;
import diskviewer.libs.ASMLib.DecodedASM;

/**
 * Displays information about given tracks and allows viewing of raw sector data
 */

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.DiskInfo;
import diskviewer.libs.disk.TrackInfo;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.libs.disk.Sector;

public class TrackSummaryPage extends page {
	public int CurrentSector = 0;
	public int CurrentTrack = 0;
	public int CurrentSide = 0;
	public boolean LastAsm = false;

	public String get(PlusThreeDiskWrapper disk) {
		String asmID = "0";
		String notAsmID = "1";
		String FlipASMText = "As assembly";
		ASMLib asm = null;
		if (LastAsm) {
			asmID = "1";
			notAsmID = "0";
			FlipASMText = "As Hex";
			asm = new ASMLib();
		}

		DiskInfo di;
		StringBuilder result = new StringBuilder();
		
		
		result.append(DEFAULT_TABLE_STYLESHEET + HEX_TABLE_STYLESHEET + "\r\n");
		result.append("<h1>Track information</h1>\r\n");
		result.append("<button style=\"width:180\" type=\"button\" onclick=\"ShowSector('"
				+ String.valueOf(CurrentSide) + "','" + String.valueOf(CurrentTrack) + "', '" + notAsmID + "')\">"
				+ FlipASMText + "</button><br>");

		try {
			di = disk.GetDiskInfo();
			result.append("<table>\r\n" + 
			              "<tr><td><b>DiskID</b></td><td>" + di.DiskID + "</td></tr>\r\n" +
			              "</table>\r\n<br>\r\n" +
			              "<table style=\"border-collapse: collapse\">\r\n");
			for (int side = 0; side < disk.GetDiskInfo().sides; side++) {
				result.append("<tr><td style=\";border: 1px solid #ddd;\"><b>Side " + String.valueOf(side)
						+ "</b></td><td style=\";border: 1px solid #ddd;\">");
				for (int track = 0; track < disk.GetDiskInfo().tracks; track++) {
					result.append("<button style=\"width:40\" type=\"button\" onclick=\"ShowSector('"
							+ String.valueOf(side) + "','" + String.valueOf(track) + "', '" + asmID + "')\">"
							+ String.valueOf(track) + "</button>");
					if ((track % 20) == 19) {
						result.append("<br>\r\n");
					}
				}
				result.append("</td></tr>");
			}
			result.append("</table>\r\n");

			// Show the selected track.
			int ch = CurrentTrack * di.sides + CurrentSide;
			TrackInfo CurrentTrack = disk.Tracks[ch];

			result.append("<br><h2>Track " + String.valueOf(CurrentTrack.tracknum) + " Side "
					+ String.valueOf(CurrentTrack.side) + "</h2>\r\n");

			// iterate all the sectors:
			// Note that sectors are not consecutive and in copy protected disks, can be
			// missing.
			// We will ignore any sectors that dont exist.

			for (int sectnum = CurrentTrack.minsectorID; sectnum < CurrentTrack.maxsectorID + 1; sectnum++) {
				Sector Sector = CurrentTrack.GetSectorBySectorID(sectnum);
				if (Sector != null) {

					if (!LastAsm) {
						//output as Hex
						result.append("<table class=\"hex\">\r\n");
						result.append("<tr><th colspan=3>" + "<b>Track</b> " + String.valueOf(Sector.track)
								+ ";<b>Head</b> " + String.valueOf(Sector.side) + ";<b>Sector</b> "
								+ String.valueOf(Sector.sectorID) + ";<b>FDC SZ</b> " + String.valueOf(Sector.Sectorsz)
								+ ";<b>FDC sr1</b> " + String.valueOf(Sector.FDCsr1) + ";<b>FDC sr2</b> "
								+ String.valueOf(Sector.FDCsr2) + ";<b>Actual size</b> "
								+ String.valueOf(Sector.ActualSize) + "</td></tr>\r\n");
						// sector data
						String hexdata = "";
						String chardata = "";
						String starttext = "000-" + toHexThreeDigit(31);
						for (int i = 0; i < Sector.data.length; i++) {
							if ((i % 32) == 0 && (i > 0)) {
								result.append("<tr><td>" + starttext + "</td><td>" + hexdata + "</td><td>"
										+ chardata + "</td></tr>\r\n");
								hexdata = "";
								chardata = "";
								starttext = toHexThreeDigit(i) + "-" + toHexThreeDigit(i + 31);
							}
							hexdata = hexdata + " " + toHexTwoDigit(Sector.data[i]);
							if ((Sector.data[i] > 0x1f) && (Sector.data[i] < 0x7f)) {
								chardata = chardata + (char) Sector.data[i];
							} else {
								chardata = chardata + "?";
							}

						}
						result.append("<tr><td>" + starttext + "</td><td>" + hexdata + "</td><td>" + chardata
								+ "</td></tr>\r\n");

						result.append("</table>\r\n<br>\r\n");
					} else {
						//output as ASM
						result.append("<table class=\"hex\">\r\n"
								+ "<tr><th colspan=4>Disassembled sector</td></tr>\r\n"
							    + "<tr><th>address in sector</th><th>Hex</th><th>Asm</th><th>chr</th></tr>\r\n");
					int realaddress = 0x0000;
					int data[] = new int[5];
					try {
					while (realaddress < Sector.data.length) {
						String chrdata = "";
						for (int i = 0; i < 5; i++) {
							int d = 0;
							if (realaddress + i < Sector.data.length) {
								d = (int) Sector.data[realaddress + i] & 0xff;
							}
							data[i] = d;

							if ((d > 0x1F) && (d < 0x7f)) {
								chrdata = chrdata + (char) d;
							} else {
								chrdata = chrdata + "?";
							}
						}
						// decode instruction
						DecodedASM Instruction = asm.decode(data, realaddress);
						// output it. - First, assemble a list of hex bytes, but pad out to 12 chars
						// (4x3)
						String hex = "";
						for (int j = 0; j < Instruction.length; j++) {
							hex = hex + toHexTwoDigit(data[j]) + " ";
						}
						while (hex.length() < 12) {
							hex = hex + "   ";
						}
						result.append("<tr><td>" + IntAndHex(realaddress) + "</td><td>" + hex + "</td><td>" 
								+ Instruction.instruction + "</td><td>" + chrdata.substring(0, Instruction.length)
								+ "</td></tr>\r\n");

						realaddress = realaddress + Instruction.length;

					} //while
					} catch (Exception E) {
						System.out.println("Error at: "+realaddress );
						System.out.println(E.getMessage());
						E.printStackTrace();
					}
					result.append("</table>\r\n<br>\r\n");

					}

				} // if
			} // for

		} catch (BadDiskFileException e) {
			result.append("Disk information invalid.");
		}
		return (result.toString());
	}
}
