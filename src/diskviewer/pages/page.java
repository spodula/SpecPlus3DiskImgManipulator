package diskviewer.pages;
/**
 * Base page containing useful utils for displaying stuff as html.
 */

import java.util.Vector;

public class page {
	public boolean VerboseMode=false;
	
	public static String DEFAULT_TABLE_STYLESHEET="<style>table {font-family: Arial, Helvetica, sans-serif; border-collapse: collapse;} "
			+ "td, th { border: 1px solid #ddd;padding: 8px;} "
			+ "th {  padding-top: 12px; padding-bottom: 12px; text-align: left; background-color: #cccccc; color: black;}</style>";
	public static String HEX_TABLE_STYLESHEET="<style>table.hex {font-family: Arial, Helvetica, sans-serif; border-collapse: collapse;} "
			+ "table.hex td { border: 1px solid #ddd;padding: 8px;font-family: \"Lucida Console\", \"Courier New\", monospace;} "
			+ "table.hex th { border: 1px solid #ddd;  padding-top: 2px; padding-bottom: 2px; text-align: left; background-color: #cccccc; color: black;}</style>";
	
	/**
	 * return a 2 digit hex number with additional space.
	 * 
	 * @param i
	 * @return
	 */
	char hexstr[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public String toHexTwoDigit(int i) {
		int msb = (i & 0xf0) / 16;
		int lsb = (i & 0x0f);
		char result[] = { hexstr[msb], hexstr[lsb]};

		return (new String(result));
	}

	public String toHexThreeDigit(int i) {
		int hsb = (i & 0xf00) / 256;
		int msb = (i & 0x0f0) / 16;
		int lsb = (i & 0x00f);
		char result[] = { hexstr[hsb],hexstr[msb], hexstr[lsb] };
		return (new String(result));
		
	}
	
	public String toHexStringSpace(int i) {
		int msb = (i & 0xf0) / 16;
		int lsb = (i & 0x0f);
		char result[] = { hexstr[msb], hexstr[lsb], ' ' };

		return (new String(result));
	}

	
	/**
	 * Provide a hex dump with HTML formatting, colouring and keys. GDS March 2018 -
	 * converted to use StringBuilder to speed it up.
	 * 
	 * @param start      - Start of values. Note displays from the lowest $10
	 * @param end        - End of values. Note, Displays entire final line with "--"
	 * @param Boundaries - Vector of colour boundary addresses. In order of address.
	 * @param Keys       - Text to be put in the key at the bottom
	 * @return - String containing the hex dump
	 * @throws Exception
	 */
	public String hexdumpHTML(int start, int end, Vector<Integer> Boundaries, Vector<String> Keys, int displacement,
			char[] pfile) throws Exception {
		String[] colours = { "#00ffff", "#ffff00", "#ff6666", "#cc66ff", "#0099ff", "#cccc00", "#99ff99", "#00ccff",
				"#9999ff", "#cccc00", "#6699ff", "#ff8c66", "#b366ff", "#ffb366", "#ff668c", "#66ffff" };

		start = start - displacement;
		end = end - displacement;
		int numcols = colours.length;
		int boundaryptr = 1;
		int nextboundary = -1;
		if (Boundaries.size() > 0) {
			nextboundary = Boundaries.get(0).intValue() - displacement;
		}
		StringBuilder sb = new StringBuilder();

		try {
			sb.append("<h2>" + IntAndHex(start) + " to " + IntAndHex(end) + "</h2>\r\n<table>\r\n<tr><td></td><td>");
			sb.append("<pre>00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F</pre></td></tr>\r\n");

			int ptr = start & 0xFFF0;
			int cptr = -1;

			sb.append("<tr><td>" + IntAndHex(ptr) + "</td><td><pre>");
			while (ptr <= end) {
				if (ptr < start) {
					sb.append("-- ");
				} else {
					/*
					 * check to see if we are on a boundary and change colours appropriately.
					 */
					if (nextboundary == ptr) {
						// Get the next boundary. (Or set to after the last character if at the end.)
						if (boundaryptr == Boundaries.size()) {
							nextboundary = end + 16;
						} else {
							nextboundary = Boundaries.get(boundaryptr++).intValue() - displacement;
						}
						// If not the first colour change, end the current colour.
						if (cptr != -1) {
							sb.append("</span>");
						}
						// Now, set the colour from the list.
						cptr++;
						sb.append("<span style=\"background-color:" + colours[cptr % numcols] + "\">");
					}
					// append the byte
					sb.append(toHexTwoDigit(pfile[ptr + displacement])+" ");
				}
				ptr++;
				// go onto a newline if we have reached the end of the line.
				if (((ptr & 0x0f) == 0) && (ptr != (end + 1))) {
					// end the current colour.
					if (cptr != -1) {
						sb.append("</span>");
					}
					// output the address.
					sb.append("</pre></td></tr>\r\n<tr><td>" + IntAndHex(ptr) + "</td><td><pre>");
					// now start the colour again.
					if (cptr != -1) {
						sb.append("<span style=\"background-color:" + colours[cptr % numcols] + "\">");
					}
				}
			}
			if (cptr != -1) {
				sb.append("</span>");
			}
			while ((ptr & 0x0f) != 0) {
				sb.append("-- ");
				ptr++;
			}

			sb.append("</pre></td></tr>\r\n</table>\r\n<br>\r\n");
			for (int i = 0; i < Keys.size(); i++) {
				String key = Keys.get(i);
				String color = colours[i & 15];
				sb.append("<span style=\"background-color:" + color + "\">" + key + "</span><br>\r\n");
			}
			sb.append("<br>");

		} catch (Exception e) {
			e.printStackTrace();
		}
		return (sb.toString());
	}

	/**
	 * Returns a string in the format "xxxx ($yyyy)" where xxxx is the decimal
	 * number and yyyy is the hex.
	 * 
	 * @param i
	 * @return
	 */
	public String IntAndHex(int i) {
		Integer ii = i;

		String result = ii.toString() + " ($" + Integer.toHexString(ii) + ")";

		return (result);
	}

}
