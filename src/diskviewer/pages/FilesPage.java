package diskviewer.pages;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import diskviewer.libs.ASMLib;

/**
 * 
 * Display the file page as set in filenum
 * filedisplay will show what it tries to treat the file as. 
 */

import diskviewer.libs.Speccy;
import diskviewer.libs.ASMLib.DecodedASM;
import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.cpm.DirectoryEntry;
import diskviewer.libs.disk.cpm.Dirent;
import diskviewer.libs.disk.cpm.Plus3DosFileHeader;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;

public class FilesPage extends page {
	public int filenum = 0;
	public int filedisplay = -1;
	public PlusThreeDiskWrapper LastDisk;

	public String get(PlusThreeDiskWrapper disk) throws BadDiskFileException {
		try {
			LastDisk = disk;
			StringBuilder sb = new StringBuilder();

			sb.append(DEFAULT_TABLE_STYLESHEET);

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
				sb.append("<button type=\"button\" onclick=\"ShowFile('" + i + "')\" style=\"width:100\">"
						+ d.filename() + "</button>");
				i++;
			}
			sb.append("\r\n<br>");

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
				String deletedtext = "";
				if (d.IsDeleted) {
					deletedtext = " (DELETED)";
				}

				sb.append("<h1>File " + d.filename() + deletedtext + "</h1>\r\n<h2>CPM level information</h2>");
				sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
						+ "','1')\" style=\"width:200;height:40\">Extract file</button>\r\n");
				sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
						+ "','2')\" style=\"width:200;height:40\">Extract With +3dos header</button>\r\n");
				if (d.IsDeleted) {
					sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
							+ "','8')\" style=\"width:200;height:40\">UnDelete file</button>\r\n");
				} else {
					sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
							+ "','3')\" style=\"width:200;height:40\">Delete file</button>\r\n");
				}
				sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
						+ "','4')\" style=\"width:200;height:40\">Rename file</button>\r\n");

				sb.append("<br>\r\n<br>\r\n<table>");
				sb.append("<tr><th><b>Filename</b></th><td>" + d.filename() + "</td>" + "<th><b>User#</b></th><td>"
						+ FirstDirEnt.GetUserNumber() + "</td>" + "<th><b>Attributes</b></th><td>" + attrib + "</td>"
						+ "<th><b>Logical File size</b></th><td>" + d.GetFileSize() + "b</td></tr>\r\n");
				sb.append("</table>\r\n");

				// Get the +3DOS header
				Plus3DosFileHeader header = d.GetPlus3DosHeader();
				if (!header.IsPlusThreeDosFile) {
					sb.append("<h2>Not a +3Dos file</h2>");
				} else {
					sb.append("<h2>Plus3DOS header information</h2>\r\n");

					sb.append("<table>");
					sb.append("<tr><th><b>Signature</b></th><td>" + header.Signature + "</td>\r\n"
							+ "<th><b>File size</b></th><td>" + header.fileSize + "</td>\r\n"
							+ "<th><b>Issue</b></th><td>" + String.valueOf(header.IssueNo) + "</td>\r\n");
					sb.append("<th><b>SoftEOF byte</b></th><td>" + String.valueOf(header.SoftEOF) + "</td>\r\n"
							+ "<th><b>Version</b></th><td>" + String.valueOf(header.VersionNo) + "</td>\r\n"
							+ "<th><b>Checksum</b></th><td>" + String.valueOf(header.CheckSum) + "</td></tr>\r\n");

					sb.append("</table>\r\n");
					sb.append("<h2>BASIC header information</h2>\r\n");

					String filetypes[] = { "Program", "Numeric array", "Char array", "Code" };

					sb.append("<table>\r\n");
					sb.append("<tr><th><b>File type</b></th><td>" + filetypes[header.filetype] + " (" + header.filetype
							+ ")</td>\r\n");
					sb.append("<th><b>File length</b></th><td>" + header.filelength + "</td>\r\n");
					if (header.filetype == Plus3DosFileHeader.FILETYPE_BASIC) {
						sb.append("<th><b>Run Line</b></th><td>" + header.line + "</td>\r\n");
						sb.append("<th><b>Variables offset</b></th><td>" + header.VariablesOffset + "</td>\r\n");
					} else if (header.filetype == Plus3DosFileHeader.FILETYPE_CODE) {
						sb.append("<th><b>Load address</b></th><td>" + header.loadAddr + "</td>\r\n");
					} else {
						sb.append("<th><b>Variable</b></th><td>" + header.VarName + "</td>\r\n");
						sb.append("<th><b>Checksum Valid</b></th><td>" + String.valueOf(header.ChecksumValid)
								+ "</td>\r\n");
					}

					sb.append("</tr></table>\r\n<br>\r\n");
				}
				String BasicDisabled = "";
				String ArrayDisabled = "";
				String scrDisabled = "";
				if (header.filetype != Plus3DosFileHeader.FILETYPE_BASIC)
					BasicDisabled = "disabled";
				if ((header.filetype != Plus3DosFileHeader.FILETYPE_CHRARRAY)
						&& (header.filetype != Plus3DosFileHeader.FILETYPE_NUMARRAY))
					ArrayDisabled = "disabled";

				sb.append("<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','0')\" style=\"width:160\">RAW +3DOS Data</button></td>\r\n");
				sb.append("<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','1')\" style=\"width:160\">RAW File Data</button></td>\r\n");
				sb.append("<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','2')\" style=\"width:160\" " + BasicDisabled + ">BASIC</button></td>\r\n");
				sb.append("<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','3')\" style=\"width:160\" " + ArrayDisabled + ">ARRAY</button></td>\r\n");
				sb.append("<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','4')\" style=\"width:160\" " + scrDisabled + ">SCREEN$</button></td>\r\n");
				sb.append("<button type=\"button\" onclick=\"DisplayFileAs('" + filenum
						+ "','5')\" style=\"width:160\" " + scrDisabled + ">Assembly</button></td><br>\r\n");

				// Select default page depending on what file type is in the header
				if (header.IsPlusThreeDosFile) {
					// If the header is present...
					if (filedisplay == -1) {
						if (header.filetype == Plus3DosFileHeader.FILETYPE_BASIC) {
							filedisplay = 2;
						} else if (header.filetype == Plus3DosFileHeader.FILETYPE_CHRARRAY) {
							filedisplay = 3;
						} else if (header.filetype == Plus3DosFileHeader.FILETYPE_NUMARRAY) {
							filedisplay = 3;
						} else {
							// special case for code files of length 6912, probably screen file.
							if (header.filelength == 6912) {
								filedisplay = 4;
							} else {
								filedisplay = 1;
							}
						}
					}
				} else {
					// No +3dos header just display raw file.
					filedisplay = 0;
				}

				byte file[] = d.GetFileData();
				if (filedisplay == 1 || filedisplay == 0) {
					DecodeFileAsCODE(sb, file, (filedisplay == 1));
				}
				if (filedisplay == 2) { // basic
					DecodeFileAsBASIC(sb, file, header);
				}
				if (filedisplay == 3) { // Array
					DecodeFileAsArray(sb, file, header, true);
				}
				if (filedisplay == 4) { // screen$
					DecodeFileAsScreen(sb, file);
				}
				if (filedisplay == 5) { // Disassembly
					DecodeFileAsAssembly(sb, file, header.loadAddr );
				}
			}

			return (sb.toString());
		} catch (Exception E) {
			System.out.println(E.getLocalizedMessage());
			E.printStackTrace();
			return (E.getLocalizedMessage());
		}
	}

	/**
	 * Decode the given file as a CODE file (Just create a hex dump) This is the
	 * default for files with no +3 header or CODE files.
	 * 
	 * @param sb
	 * @param file
	 * @param SkipHeader - Skip the 128 byte +3 dos header.
	 */
	private void DecodeFileAsCODE(StringBuilder sb, byte file[], boolean SkipHeader) {

		try {
			sb.append("<br>\r\n<table>\r\n<tr><td></td><td>");
			sb.append(
					"<pre>00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F</pre></td><td><pre>0123456789abcdef</pre></td></tr>\r\n");
			int ptr = 0;
			// If looking at the actual file, skip over the header. (first 128 bytes)
			if (SkipHeader)
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
					if (SkipHeader)
						displayptr = ptr - 0x80;
					sb.append("</pre></td><td><pre>" + ascii + "</pre></td></tr>\r\n<tr><td>" + IntAndHex(displayptr)
							+ "</td><td><pre>");
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

	}

	/**
	 * Decode the file as a Speccy basic file.
	 * 
	 * @param sb
	 * @param file
	 * @param header
	 */
	private void DecodeFileAsBASIC(StringBuilder sb, byte file[], Plus3DosFileHeader header) {
		sb.append("<br><h1>Program</h1>\r\n\r\n<pre>");
		sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
				+ "','6')\" style=\"width:400;height:40\">Save basic as text file</button>\r\n");

		Speccy.DecodeBasicFromLoadedFile(file, sb, header, false);
		sb.append("<br><h1>Variables area</h1><pre>");
		Speccy.DecodeVariablesFromLoadedFile(file, sb, header);

		sb.append("</pre>\r\n");
	}

	/**
	 * Decode the file as a saved array Both Numeric and Character arrays have the
	 * same basic format: 00 - Number of dimensions 01-02 - First dimension size
	 * .... XX-XX - Last dimension size Array data For character arrays, consecutive
	 * characters 1(1 bit) For Numeric arrays, consecuvtive numbers (5 bit)
	 * 
	 * Data is in the descending order of dimension.
	 * 
	 * @param sb
	 * @param file
	 * @param header
	 */
	private void DecodeFileAsArray(StringBuilder sb, byte file[], Plus3DosFileHeader header, boolean html) {
		int location = 0x80; // skip header

		// Number of dimensions
		int numDimensions = file[location++] & 0xff;

		// LOad the dimension sizes into an array
		int Dimsizes[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
			int dimsize = file[location++] & 0xff;
			dimsize = dimsize + (file[location++] & 0xff) * 0x100;
			Dimsizes[dimnum] = dimsize;
		}
		if (html) {
			if (header.filetype == Plus3DosFileHeader.FILETYPE_CHRARRAY) {
				sb.append("<h1>Character Array " + header.VarName + "$(");
			} else {
				sb.append("<h1>Numeric array " + header.VarName + "(");
			}

			for (int dimnum = 0; dimnum < numDimensions; dimnum++) {
				if (dimnum > 0)
					sb.append(",");
				sb.append(String.valueOf(Dimsizes[dimnum]));
			}
			sb.append(")</h1>\r\n");
			sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
					+ "','7')\" style=\"width:400;height:40\">Save array as text file</button>\r\n");

		}
		// count of what dimensions have been processed.
		int DimCounts[] = new int[numDimensions];
		for (int dimnum = 0; dimnum < numDimensions; dimnum++)
			DimCounts[dimnum] = 0;

		if (html) {
			sb.append("<pre>");
		}
		boolean complete = false;
		while (!complete) {
			for (int cc = 0; cc < Dimsizes[Dimsizes.length - 1]; cc++) {
				if (html) {
					sb.append("(");
					for (int ds = Dimsizes.length - 2; ds > -1; ds--) {
						sb.append(String.valueOf(DimCounts[ds] + 1) + ",");
					}
					sb.append(String.valueOf(cc + 1) + ")=");
				} else {
					if (cc != 0) {
						sb.append(",");
					}
				}
				if (header.filetype == Plus3DosFileHeader.FILETYPE_CHRARRAY) {
					sb.append(Speccy.tokens[file[location++] & 0xff]);
				} else {
					double x = Speccy.GetNumberAtByte(file, location);
					// special case anything thats an exact integer because it makes the arrays look
					// less messy when displayed.
					if (x != Math.rint(x)) {
						sb.append(x);
						sb.append(",");
					} else {
						sb.append((int) x);
					}
					location = location + 5;
				}
			}
			if (html) {
				sb.append("<br>\r\n");
			} else {
				sb.append("\r\n");
			}

			int diminc = Dimsizes.length - 2;
			boolean doneInc = false;
			while (!doneInc) {
				if (diminc == -1) {
					doneInc = true;
					complete = true;
				} else {
					int x = DimCounts[diminc];
					x++;
					if (x == Dimsizes[diminc]) {
						DimCounts[diminc] = 0;
						diminc--;
					} else {
						DimCounts[diminc] = x;
						doneInc = true;
					}
				}
			}

		}
		if (html) {
			sb.append("</pre>");
		}
	}

	/**
	 * Convert the file into an image. Image will always be 256x192
	 * 
	 * @param file
	 * @param filebase
	 * @return
	 */
	BufferedImage GetImageFromFile(byte file[], int filebase) {
		BufferedImage scrn = new BufferedImage(256, 192, BufferedImage.TYPE_3BYTE_BGR);
		// populate the image
		for (int yptn = 0; yptn < 192; yptn++) {
			// screen location in file = 0033111 222XXXXX where y = 33222111
			int y1 = yptn & 0x07;
			int y2 = (yptn & 0x38) / 0x08;
			int y3 = (yptn & 0xc0) / 0x40;

			int lineAddress = (y2 * 0x20) + (y1 * 0x100) + (y3 * 0x800);

			// Attribute address is linear starting from 0x1800
			int attributeAddr = ((yptn / 8) * 32) + 0x1800;

			int pixelX = 0;
			for (int xptn = 0; xptn < 32; xptn++) {
				// get the 8 bits of the current byte
				int pixels = file[lineAddress + xptn + filebase] & 0xff;

				// Get the colours of the pixels in the current block
				int attributes = file[attributeAddr + xptn + filebase] & 0xff;
				int pen = attributes & 0x7;
				int paper = (attributes & 0x38) / 0x08;

				// convert the colours from index to actual RGB values. (Taking into account
				// BRIGHT)
				if ((attributes & 0x40) == 0x40) {
					pen = Speccy.coloursBright[pen];
					paper = Speccy.coloursBright[paper];
				} else {
					pen = Speccy.colours[pen];
					paper = Speccy.colours[paper];
				}

				// plot 8 pixels of a line
				for (int px = 0; px < 8; px++) {
					boolean bit = (pixels & 0x80) == 0x80;
					pixels = pixels << 1;

					if (bit) {
						scrn.setRGB(pixelX++, yptn, pen);
					} else {
						scrn.setRGB(pixelX++, yptn, paper);
					}
				}
			}
		}
		return (scrn);
	}

	/**
	 * Decode the code section as a Speccy basic screen.
	 * 
	 * @param sb
	 * @param file
	 */
	private void DecodeFileAsScreen(StringBuilder sb, byte file[]) {
		if (file.length < 6912) {
			sb.append(("<h2>File not big enough to contain a screen</h2>"));
		} else {
			sb.append("<button type=\"button\" onclick=\"Fileop('" + filenum
					+ "','5')\" style=\"width:400;height:40\">Save screens as PNG/JPG/GIF</button>\r\n");
			int filebase = 0x80;

			try {
				// Create the image
				while (file.length - filebase > 0x1aff) {
					BufferedImage scrn = GetImageFromFile(file, filebase);
					// display the image
					String img = GetBase64ForImage(scrn);
					sb.append("<br><br><img width=\"512\" height=\"384\" src=\"data:image/png;base64," + img + "\">");
					// next image
					filebase = filebase + 0x1b00;
				} // while
			} catch (Exception E) {
				E.printStackTrace();
			}

		}

	}

	/**
	 * Convert the image into a Base64 array. unlike in the initial version, this
	 * doesn't use temp files. Modified from reference code:
	 * https://www.tabnine.com/code/java/methods/javax.imageio.ImageIO/createImageOutputStream
	 * 
	 * @param scrn
	 * @return
	 */
	private String GetBase64ForImage(BufferedImage scrn) {
		String result = "";
		try {
			// output stream for the stream of bytes
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			// Get the image writer class for PNG files
			ImageWriter imgWrtr = ImageIO.getImageWritersByFormatName("png").next();
			// Get the image data stream wrapping the ByteStream
			ImageOutputStream imgOutStrm = ImageIO.createImageOutputStream(output);
			try {
				// Tell the image writer where we are to output to the stream
				imgWrtr.setOutput(imgOutStrm);
				// Get the default compression parameters.
				ImageWriteParam pngWrtPrm = imgWrtr.getDefaultWriteParam();
				// Write the image to the stream
				imgWrtr.write(null, new IIOImage(scrn, null, null), pngWrtPrm);
			} finally {
				imgOutStrm.close();
			}
			// Now encode the output stream to Base64
			Base64.Encoder enc = Base64.getEncoder();
			byte[] encbytes = enc.encode(output.toByteArray());
			result = new String(encbytes, StandardCharsets.US_ASCII);

		} catch (IOException e) {
			// Dont want to stop anything from displaying, but *do* want to show the error.
			e.printStackTrace();
		}
		return (result);
	}

	/**
	 * Save the last file that was accessed either with or without its header.
	 * 
	 * @param witheheader
	 * @throws BadDiskFileException
	 * @throws IOException
	 */
	public void saveFile(String filename, boolean witheheader) throws BadDiskFileException, IOException {
		if (filenum > 0 && LastDisk != null) {
			DirectoryEntry d = LastDisk.DirectoryEntries[filenum - 1];
			byte file[] = d.GetFileData();
			byte filetowrite[] = file;
			if (!witheheader) {
				// if we dont want to write the header, create a new file without the header
				filetowrite = new byte[file.length - 0x80];
				for (int i = 0; i < file.length - 0x80; i++) {
					filetowrite[i] = file[i + 0x80];
				}
			}

			try {
				FileOutputStream fos = new FileOutputStream(filename);
				try {
					fos.write(filetowrite);
				} finally {
					fos.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Save the last loaded file as a PNG or series of PNGs
	 * 
	 * @param filename
	 * @throws BadDiskFileException
	 */
	public void saveFileAsScreen(String filename, String type) throws BadDiskFileException {
		if (filenum > 0 && LastDisk != null) {
			int fnum = 0;
			DirectoryEntry d = LastDisk.DirectoryEntries[filenum - 1];
			byte file[] = d.GetFileData();
			int filebase = 0x80;
			// remove the final .png
			filename = filename.toLowerCase();
			if (filename.endsWith("." + type)) {
				filename = filename.substring(0, filename.length() - 4);
			}

			try {
				// Create the image
				while (file.length - filebase > 0x1aff) {
					BufferedImage scrn = GetImageFromFile(file, filebase);
					// make a filename;
					String filetosave = filename + String.valueOf(fnum) + "." + type;
					if (fnum == 0) {
						filetosave = filename + "." + type;
					}
					fnum++;
					// Save image
					File outputfile = new File(filetosave);
					ImageIO.write(scrn, type, outputfile);

					// next image
					filebase = filebase + 0x1b00;
				} // while
			} catch (IOException e) {
				// Dont want to stop anything from displaying, but *do* want to show the error.
				e.printStackTrace();
			}

		}
	}

	/**
	 * Save a file to disk as BASIC
	 * 
	 * @param filename
	 * @throws BadDiskFileException
	 */
	public void saveFileAsBasic(String filename) throws BadDiskFileException {
		if (filenum > 0 && LastDisk != null) {
			DirectoryEntry d = LastDisk.DirectoryEntries[filenum - 1];
			byte file[] = d.GetFileData();
			Plus3DosFileHeader header = d.GetPlus3DosHeader();
			try {
				StringBuilder sb = new StringBuilder();

				Speccy.DecodeBasicFromLoadedFile(file, sb, header, true);

				FileOutputStream fos = new FileOutputStream(filename);
				try {
					fos.write(sb.toString().getBytes());
				} finally {
					fos.close();
				}

			} catch (IOException e) {
				// Dont want to stop anything from displaying, but *do* want to show the error.
				e.printStackTrace();
			}
		}
	}

	/**
	 * Undelete the current file.
	 */
	public void SetDeleteCurrentFile(boolean delete) {
		if (filenum > 0 && LastDisk != null) {
			DirectoryEntry d = LastDisk.DirectoryEntries[filenum - 1];
			d.SetDeleted(delete);
		}
	}

	/**
	 * Save array
	 * 
	 * @throws BadDiskFileException
	 * 
	 */
	public void saveFileAsTextArray(String filename) throws BadDiskFileException {
		if (filenum > 0 && LastDisk != null) {
			DirectoryEntry d = LastDisk.DirectoryEntries[filenum - 1];
			byte file[] = d.GetFileData();
			Plus3DosFileHeader header = d.GetPlus3DosHeader();
			try {
				StringBuilder sb = new StringBuilder();

				DecodeFileAsArray(sb, file, header, false);

				FileOutputStream fos = new FileOutputStream(filename);
				try {
					fos.write(sb.toString().getBytes());
				} finally {
					fos.close();
				}

			} catch (IOException e) {
				// Dont want to stop anything from displaying, but *do* want to show the error.
				e.printStackTrace();
			}
		}
	}

	/**
	 * Decode the code section as assembly.
	 * 
	 * @param sb
	 * @param file
	 */
	public void renameFile(String newFilename) {
		if (filenum > 0 && LastDisk != null) {
			DirectoryEntry d = LastDisk.DirectoryEntries[filenum - 1];
			d.RenameTo(newFilename);
		}
	}

	
	private void DecodeFileAsAssembly(StringBuilder sb, byte[] file, int start) {
		sb.append("<table class=\"hex\">\r\n"
					+ "<tr><th colspan=4>" + "Disassembled file" + "</td></tr>\r\n"
				    + "<tr><th>Loaded address</th><th>Hex</th><th>Asm</th><th>chr</th></tr>\r\n");
		ASMLib asm = new ASMLib();
		int loadedaddress = start;
		int realaddress = 0x0080;
		int data[] = new int[5];
		try {
		while (realaddress < file.length) {
			String chrdata = "";
			for (int i = 0; i < 5; i++) {
				int d = 0;
				if (realaddress + i < file.length) {
					d = (int) file[realaddress + i] & 0xff;
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
			sb.append("<tr><td>" + IntAndHex(loadedaddress) + "</td><td>" + hex + "</td><td>" 
					+ Instruction.instruction + "</td><td>" + chrdata.substring(0, Instruction.length)
					+ "</td></tr>\r\n");

			realaddress = realaddress + Instruction.length;
			loadedaddress = loadedaddress + Instruction.length;

		} //while
		} catch (Exception E) {
			System.out.println("Error at: "+realaddress + "("+loadedaddress+")");
			System.out.println(E.getMessage());
			E.printStackTrace();
		}
		sb.append("</table>\r\n<br>\r\n");
		

	}

}
