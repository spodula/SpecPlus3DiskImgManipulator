package diskviewer.pages;

/**
 * Low level CPM file system page. Shows the directory entries, what blocks they use and the BAM
 */

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.cpm.DirectoryEntry;
import diskviewer.libs.disk.cpm.Dirent;
import diskviewer.libs.disk.cpm.Plus3DosFileHeader;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;

public class FileSystemPage extends page {
	String[] colours = { "#ffffff", "#00ffff", "#ffff00", "#ff6666", "#cc66ff", "#0099ff", "#cccc00", "#99ff99",
			"#00ccff", "#9999ff", "#cccc00", "#6699ff", "#ff8c66", "#b366ff", "#ffb366", "#ff668c", "#66ffff" };

	public int filenum = 0;
	public int filedisplay = 0;

	public String get(PlusThreeDiskWrapper disk) throws BadDiskFileException {
		String result = DEFAULT_TABLE_STYLESHEET;
		result = result + "<h1>File system</h1>";

		if (!disk.IsValidCPMFileStructure) {
			result = result + "<h2>CPM/+3DOS file system is invalid.</h2>";
		} else {
			result = result + "<button type=\"button\" onclick=\"Fileop('" + filenum
					+ "','10')\" style=\"width:200;height:40\">Add Headerless file</button>\r\n";
			result = result + "<button type=\"button\" onclick=\"Fileop('" + filenum
					+ "','11')\" style=\"width:200;height:40\">Add CODE file</button>\r\n";
			result = result + "<button type=\"button\" onclick=\"Fileop('" + filenum
					+ "','12')\" style=\"width:200;height:40\">Add BASIC from ASCII</button>\r\n";
			result = result + "<button type=\"button\" onclick=\"Fileop('" + filenum
					+ "','13')\" style=\"width:200;height:40\">Add image as SCREEN$</button>\r\n";

			result = result + "<table>\r\n";

			result = result + "<tr><th><b>Reserved Tracks</b></th><td>" + disk.reservedTracks + "</td>"
					+ "<th><b>Disk type</b></th><td>" + disk.diskformat + "</td>" + "<th><b>Total Blocks</b></th><td>"
					+ disk.maxblocks + "</td>" + "<th><b>Reserved Blocks</b></th><td>" + disk.reservedblocks + "</td>"
					+ "<th><b>Free space</b></th><td>" + disk.freeSpace + "Kb</td></tr>\r\n";
			result = result + "<tr><th><b>Max dirents</b></th><td>" + disk.maxDirEnts + "</td>"
					+ "<th><b>Used dirents</b></th><td>" + disk.usedDirEnts + "</td>"
					+ "<th><b>Disk data size</b></th><td>" + disk.diskSize + "Kb</td></tr>\r\n";

			result = result + "</table>\r\n<br>\r\n";
			result = result + "<table>\r\n";
			result = result + "<tr>";
			result = result + "<th></th>";
			result = result + "<th>Filename</th>";
			result = result + "<th>Logical size</th>";
			result = result + "<th>dirents</th>";
			result = result + "<th>blocks</th>";
			result = result + "</tr>\r\n";
			int i = 1;
			for (DirectoryEntry d : disk.DirectoryEntries) {
				String entries = "";
				for (Dirent entry : d.dirents) {
					if (!entries.isBlank()) {
						entries = entries + ", ";
					}
					entries = entries + String.valueOf(entry.entrynum);
				}
				String blocks = "";
				for (int block : d.getBlocks()) {
					if (!blocks.isBlank()) {
						blocks = blocks + ", ";
					}
					blocks = blocks + String.valueOf(block);

				}
				String deltext = "";
				if (d.IsDeleted) {
					deltext = " (Deleted)";
					if (d.IsComplete()) {
						deltext = deltext + " (Complete)";
					} else {
						deltext = deltext + " (Incomplete)";
					}
				}

				result = result + "<tr>" + "<td><button type=\"button\" onclick=\"ShowFile('" + i
						+ "')\" style=\"background-color:" + colours[i % colours.length] + ";width:40\">" + i
						+ "</button></td>" + "<td>" + d.filename() + deltext + "</td>" + "<td>" + d.GetFileSize()
						+ "</td>" + "<td>" + entries + "</td>" + "<td>" + blocks + "</td>" + "</tr>\r\n";
				i++;
			}
			result = result + "</table>\r\n<br>";
			if (disk.DirectoryEntries!=null || disk.DirectoryEntries.length == 0) {
				result = result +"<h2>No files</h2>"; 
			}

			// Now display the BAM in 32 block sections.
			// Create a BAM populated with filename numbers.
			int bam[] = new int[disk.maxblocks];
			for (i = 0; i < disk.reservedblocks; i++) {
				bam[i] = 999999;
			}
			// now from the files
			for (i = 0; i < disk.DirectoryEntries.length; i++) {
				int blocks[] = disk.DirectoryEntries[i].getBlocks();
				for (int block : blocks) {
					bam[block] = i + 1;
				}
			}

			// Display the BAM
			result = result + "<h1> Disk block map</h1><table>\r\n<tr><th></th>";
			for (i = 0; i < 32; i++) {
				result = result + "<th>" + toHexTwoDigit(i) + "</th>";
			}
			result = result + "</tr>\r\n<tr><th>000</th>";

			for (i = 0; i < disk.maxblocks; i++) {
				if (i % 32 == 0) {
					if (i != 0) {
						result = result + "</tr>\r\n<tr><th>" + toHexThreeDigit(i) + "</th>";
					}
				}
				if (bam[i] == 999999) {
					result = result + "<td>D</td>";
				} else {
					result = result + "<td style=\"background-color:" + colours[bam[i] % colours.length] + "\">"
							+ bam[i] + "</td>";
				}
			}
			result = result + "</tr>\r\n</table>\r\n";

			if (filenum > 0) {
				DirectoryEntry d = disk.DirectoryEntries[filenum - 1];
				String attrib = "";
				Dirent FirstDirEnt = d.dirents[0];
				if (FirstDirEnt.GetReadOnly())
					attrib = attrib + "R";
				if (FirstDirEnt.GetSystem())
					attrib = attrib + "S";
				if (FirstDirEnt.GetArchive())
					attrib = attrib + "A";

				result = result + "<h1>File " + d.filename() + "</h1>\r\n<h2>CPM level information</h2>";
				result = result + "<br>\r\n<table>";
				result = result + "<tr><th><b>Filename</b></th><td>" + d.filename() + "</td>"
						+ "<th><b>User#</b></th><td>" + FirstDirEnt.GetUserNumber() + "</td>"
						+ "<th><b>Attributes</b></th><td>" + attrib + "</td>" + "<th><b>Logical File size</b></th><td>"
						+ d.GetFileSize() + "b</td></tr>\r\n";
				result = result + "</table>\r\n";

				// Get the +3DOS header
				Plus3DosFileHeader header = d.GetPlus3DosHeader();
				if (!header.IsPlusThreeDosFile) {
					result = result + "<h2>Not a +3Dos file</h2>";
				} else {
					result = result + "<h2>Plus3DOS header information</h2>\r\n";

					result = result + "<table>";
					result = result + "<tr><th><b>Signature</b></th><td>" + header.Signature + "</td></tr>\r\n"
							+ "<tr><th><b>File size</b></th><td>" + header.fileSize + "</td></tr>\r\n"
							+ "<tr><th><b>Issue</b></th><td>" + String.valueOf(header.IssueNo) + "b</td></tr>\r\n";
					result = result + "<tr><th><b>SoftEOF byte</b></th><td>" + String.valueOf(header.SoftEOF)
							+ "</td></tr>\r\n" + "<tr><th><b>Version</b></th><td>" + String.valueOf(header.VersionNo)
							+ "</td></tr>\r\n" + "<tr><th><b>Checksum</b></th><td>" + String.valueOf(header.CheckSum)
							+ "</td></tr>\r\n";

					result = result + "</table>\r\n";
					result = result + "<h2>BASIC header information</h2>\r\n";

					String filetypes[] = { "Program", "Numeric array", "Char array", "Code" };

					result = result + "<table>";
					result = result + "<tr><th><b>File type</b></th><td>" + filetypes[header.filetype] + " ("
							+ header.filetype + ")</td></tr>\r\n";
					result = result + "<tr><th><b>File length</b></th><td>" + header.filelength + "</td></tr>\r\n";
					if (header.filetype == Plus3DosFileHeader.FILETYPE_BASIC) {
						result = result + "<tr><th><b>Run Line</b></th><td>" + header.line + "</td></tr>\r\n";
						result = result + "<tr><th><b>Variables offset</b></th><td>" + header.VariablesOffset + "</td></tr>\r\n";
					} else if (header.filetype == Plus3DosFileHeader.FILETYPE_CODE) {
						result = result + "<tr><th><b>Load address</b></th><td>" + header.loadAddr + "</td></tr>\r\n";
					} else {
						result = result + "<tr><th><b>Variable</b></th><td>" + header.VarName + "</td></tr>\r\n";
					}

					result = result + "</table>\r\n";
				}
				String BasicDisabled = "";
				String ArrayDisabled = "";
				String scrDisabled = "";
				if (header.filetype != Plus3DosFileHeader.FILETYPE_BASIC)
					BasicDisabled = "disabled";
				if ((header.filetype != Plus3DosFileHeader.FILETYPE_CHRARRAY)
						&& (header.filetype != Plus3DosFileHeader.FILETYPE_NUMARRAY))
					ArrayDisabled = "disabled";
				if ((header.filetype != Plus3DosFileHeader.FILETYPE_CODE) || (header.filelength != 6912))
					scrDisabled = "disabled";

				result = result + "<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','0')\" style=\"width:180\">RAW +3DOS Data</button></td>\r\n";
				result = result + "<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','1')\" style=\"width:180\">RAW File Data</button></td>\r\n";
				result = result + "<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','2')\" style=\"width:180\" " + BasicDisabled + ">BASIC</button></td>\r\n";
				result = result + "<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','3')\" style=\"width:180\" " + ArrayDisabled + ">ARRAY</button></td>\r\n";
				result = result + "<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','4')\" style=\"width:180\" " + scrDisabled + ">SCREEN$</button></td>\r\n";

				byte file[] = d.GetFileData();
				StringBuilder sb = new StringBuilder();

				try {
					sb.append("<br>\r\n<table>\r\n<tr><td></td><td>");
					sb.append(
							"<pre>00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F</pre></td><td><pre>0123456789abcdef</pre></td></tr>\r\n");
					int ptr = 0;
					if (filedisplay == 1)
						ptr = 0x80;

					int end = file.length - 1;
					String ascii = "";

					int displayptr = 0;

					sb.append("<tr><td>" + IntAndHex(displayptr) + "</td><td><pre>");
					while (ptr < file.length) {
						// Add in the ascii character
						if ((file[ptr] > 31) && (file[ptr] < 127))
							ascii = ascii + (char) file[ptr];
						else
							ascii = ascii + " ";

						// append the byte
						sb.append(toHexStringSpace(file[ptr++]));

						// go onto a newline if we have reached the end of the line.
						if (((ptr & 0x0f) == 0) && (ptr != (end + 1))) {
							// output the address.
							displayptr = ptr;
							if (filedisplay == 1)
								displayptr = ptr - 0x80;
							sb.append("</pre></td><td><pre>" + ascii + "</pre></td></tr>\r\n<tr><td>"
									+ IntAndHex(displayptr) + "</td><td><pre>");
							ascii = "";
						}
					}
					while ((ptr & 0x0f) != 0) {
						sb.append("-- ");
						ptr++;
					}

					sb.append("</pre></td><td><pre>" + ascii + "</pre></td></tr>\r\n");
					sb.append("<br>");
				} catch (Exception e) {
					e.printStackTrace();
				}
				result = result + sb.toString();

			}
		}

		return (result);

	}

}
