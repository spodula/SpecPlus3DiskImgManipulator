package diskviewer.libs;

/**
 * Speccy specific functions and constants
 * 
 * 
 * A note on the Speccy number format. 
 * 
 * on the Speccy, all numbers are 5 bytes long (plus a token at the start) 
 * Two formats are:
 *  
 * FP notation
 *  [EEEEEEEE] [Sxxxxxxx] [xxxxxxxx] [xxxxxxxx] [xxxxxxxx] 
 *  where E is the exponent -0x80, S is sign, and xxx is 31 bits of the mantissa with bit 32 (Where the sign is) assumed to be 1
 * INT notation (Positive ints 0-65535 only)
 *  [00000000] [00000000] [hhhhhhhh] [llllllll] [00000000]
 *  where h is the high byte and l is the low byte
 *  
 *  The presence of 0 in byte[0] is used to determine which is used.
 *   
 */

import diskviewer.libs.disk.cpm.Plus3DosFileHeader;

public class Speccy {
	// Start of the commands in the character set. Tokenising starts here.
	public static int SPECCY_CMD_START = 0xA3;

	// Prefix for numbers.
	public static byte NUMSTART = 0x0E;

	/**
	 * Testing for the number conversion
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// test for numeric encoding and decoding.
		System.out.println("These values should match within 31 bits ( Approx 9-10)");
		outputnum(-3212.0);
		outputnum(4096.0);
		outputnum(1323234.0);
		outputnum(23.0);
		outputnum(14096.1);
	}

	/**
	 * used for test purposes only. Encode and then re-encode a number to and from a
	 * speccy Floating point number and output it.
	 * 
	 * @param x
	 */
	static void outputnum(Double x) {
		// number
		System.out.print(" " + x + " ");
		// output the encoded value
		byte num[] = EncodeValue(x, false);
		for (byte n : num) {
			System.out.print(" " + (int) (n & 0xff));
		}
		// re-encode the number and output it. should match within 31 bits ( Approx 9-10
		// decimal places)
		int base = 1;
		if (num.length == 7)
			base = 2;
		System.out.println(" " + GetNumberAtByte(num, base));
	}

	// zx spectrum colours in HTML RGB
	public static int colours[] = { 0x000000, 0x0000D7, 0xD70000, 0xD700D7, 0x00D700, 0x00D7D7, 0xD7D700, 0xD7D7D7 };
	public static int coloursBright[] = { 0x000000, 0x0000FF, 0xFF0000, 0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00,
			0xFFFFFF };

	// zx Spectrum tokens from 0 to 255
	public static String[] tokens = { "", "", "", "", "", "", "&lt;tab&gt;", "", "&lt;left&gt;", "&lt;right&gt;", "",
			"", "", "<nl>", "<num>", "",
			// 10
			"&lt;ink&gt;", "&lt;paper&gt;", "&lt;flash&gt;", "&lt;bright&gt;", "&lt;inverse&gt;", "&lt;over&gt;",
			"&lt;at&gt;", "&lt;tab&gt;", "", "", "", "", "", "", "", "",
			// 20
			" ", "!", "\"", "#", "$", "%", "&amp;", "'", "(", ")", "*", "+", ",", "-", ".", "/",
			// 30
			"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", ":", ";", "&lt;", "=", "&gt;", "?",
			// 40
			"@", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
			// 50
			"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "[", "\\", "]", "^", "_",
			// 60
			"&", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o",
			// 70
			"p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z", "{", "|", "}", "~", "(C)",
			// 80
			// block characters:
			// BA
			// DC
			" ", "" + (char) 0x259D, // A
			"" + (char) 0x2598, // B
			"" + (char) 0x2580, // BA
			"" + (char) 0x2597, // C
			"" + (char) 0x2590, // C A
			"" + (char) 0x259a, // CB
			"" + (char) 0x259c, // CBA
			"" + (char) 0x2596, // D
			"" + (char) 0x259E, // D A
			"" + (char) 0x258C, // D B
			"" + (char) 0x258B, // D BA
			"" + (char) 0x2584, // DC
			"" + (char) 0x259F, // DC A
			"" + (char) 0x2599, // DCB
			"" + (char) 0x2588, // DCBA
			// 0x90
			"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P",
			// 0xA0
			"Q", "R", "S", " SPECTRUM ", " PLAY ", " RND ", " INKEY$ ", " PI ", " FN ", " POINT ", " SCREEN$ ",
			" ATTR ", " AT ", " TAB ", " VAL$ ", " CODE ",
			// 0xB0
			"VAL ", "LEN ", "SIN ", "COS ", "TAN ", "ASN ", "ACS ", "ATN ", "LN ", "EXP ", "INT ", "SQR ", "SGN ",
			"ABS ", "PEEK ", "IN ",
			// 0xC0
			"USR ", "STR$ ", "CHR$ ", "NOT ", "BIN ", " OR ", " AND ", " &lt;= ", "&gt;=", "&lt;&gt;", " LINE ",
			" THEN ", " TO ", " STEP ", " DEF FN ", " CAT ",
			// 0xD0
			" FORMAT ", " MOVE ", " ERASE ", " OPEN# ", " CLOSE# ", " MERGE ", " VERIFY ", " BEEP ", " CIRCLE ",
			" INK ", " PAPER ", " FLASH ", " BRIGHT ", " INVERSE ", " OVER ", " OUT ",
			// 0xE0
			" LPRINT ", " LLIST ", " STOP ", " READ ", " DATA ", " RESTORE ", " NEW ", " BORDER ", " CONTINUE ",
			" DIM ", " REM ", " FOR ", " GOTO ", " GOSUB ", " INPUT ", " LOAD ",
			// 0xF0
			" LIST ", " LET ", " PAUSE ", " NEXT ", " POKE ", " PRINT ", " PLOT ", " RUN ", " SAVE ", " RANDOMISE ",
			" IF ", " CLS ", " DRAW ", " CLEAR ", " RETURN ", " COPY " };

	/**
	 * Try to decode the given string into a Speccy token.
	 * 
	 * @param token
	 * @return
	 */
	public static String DecodeToken(String token) {
		String uToken = token.toUpperCase().trim();
		String result = "";
		//Check for a number
		if (isNumeric(token)) {
			Double d = Double.parseDouble(token);
			result = String.valueOf(d);
			// Is an integer? if so, dont put leading zeroes in the text.
			if (d == Math.floor(d)) {
				// Encode as Integer
				int di = d.intValue();
				result = String.valueOf(di);
			}
			//Get the encoded value and add it to the result. 
			byte num[] = EncodeValue(d, false);
			for (byte n : num) {
				result = result + (char)n;
			}

		} else {
			// Check for a matching token.
			int tokenNum = 0;
			for (int i = SPECCY_CMD_START; i < Speccy.tokens.length; i++) {
				String possToken = Speccy.tokens[i].trim();
				possToken = possToken.replace("&lt;", "<");
				possToken = possToken.replace("&gt;", ">");
				if (!possToken.isEmpty() && possToken.equals(uToken)) {
					tokenNum = i;
				}
			}
			// if we find one, return this.
			if (tokenNum != 0) {
				result = result + (char) tokenNum;
			} else {
				// otherwise convert into Speccy ASCII (Mostly the same as real ascii with two
				// exceptions....
				token = token.replace("(C)", "" + 0x7f);

				for (int i = 0; i < token.length(); i++) {
					int chr = token.charAt(i);
					if (chr == '@') {
						chr = 0x40;
					}
					if (chr == '£') {
						chr = 0x60;
					}
					result = result + (char) chr;
				}
			}
		}
		return result;
	}

	/**
	 * Encode a given number as a Speccy 6 byte floating point representation (as a
	 * special case, 7 byte if a negative integer) 
	 * 
	 * FP notation 
	 * [EEEEEEEE] [Sxxxxxxx] [xxxxxxxx] [xxxxxxxx] [xxxxxxxx] 
	 * where E is the exponent -0x80, S is sign,
	 * and xxx is 31 bits of the mantissa with bit 32 (Where the sign is) assumed to
	 * be 1
	 *  
	 * INT notation 
	 * [00000000] [00000000] [hhhhhhhh] [llllllll] [00000000]
	 * where h is the high byte and l is the low byte
	 * 
	 * 
	 * @param d number to encode
	 * @return encoded 6 byte array
	 */
	public static byte[] EncodeValue(Double d, boolean FloatInsteadOfNeg) {
		// Get the sign of the number and flip it (Common to both int and float
		// representation)
		byte result[] = new byte[6];
		result[0] = NUMSTART; // number starts with 0x0e
		boolean sign = (d < 0);

		// check to see if we can encode as a 16 bit integer.
		if ((d == Math.floor(d)) &&  ((d < 65536) && (d > -65536) && !FloatInsteadOfNeg) || ((d > 0) && (d<65535))) {
			// Encode as Integer
			int di = d.intValue();
			int base = 1;
			// we can only encode positive integers, so we put a negative sign in front for
			// negatives and neg the number.
			if (sign) {
				di = -di;
				result = new byte[7];
				result[0] = '-'; // -
				result[base++] = NUMSTART;
			}
			// fill in the array
			result[base++] = 0; // integer
			result[base++] = 0; //
			result[base++] = (byte) (di & 0xff);
			result[base++] = (byte) (di / 0x100);
			result[base++] = 0;
		} else {
			// encode as float
			// firstly if negative, make positive
			if (sign) {
				d = -d;
			}
			// work out the exponent of the number
			int exp = (int) (Math.log(d) / Math.log(2));
			if ((exp < -129) || (exp > 126)) {
				System.out.println("Number out of range");
			} else {
				// We need to move the number to the highest bits of a 32 bit number.
				Double dv = Math.pow(2, exp); // what we need to divide by to normalise the number (so its 1.xxxxx)
				long divisor = dv.longValue();

				Double dMantissa = d / divisor; // Now multiply it back up again so bit 32 is set. (Do it in two stages
												// because of java)
				dMantissa = dMantissa * 0x8000;
				dMantissa = dMantissa * 0x10000;
				dMantissa = dMantissa + 0.5; // add in the fiddle factor.
				long mantissa = Math.round(dMantissa); // and get it as long integer
				// We now have the value we need to write to the 4 mantissa bytes.

				// byte1 1 = exponent based around 0x80
				result[1] = (byte) ((exp + 0x81) & 0xff);

				// byte2 = highest bits of the mantissa with the final but removed.(It is
				// assumed)
				result[2] = (byte) ((mantissa >> 24) & 0x7f);
				// the highest bit of the mantissa bytes holds the sign
				if (sign)
					result[2] = (byte) (result[2] | 0x80);

				// the rest of the mantissa.
				result[3] = (byte) ((mantissa >> 16) & 0xff);
				result[4] = (byte) ((mantissa >> 8) & 0xff);
				result[5] = (byte) (mantissa & 0xff);
			}
		}

		return (result);
	}

	/**
	 * decode the number at the given byte.
	 * 
	 * @param line
	 * @param location
	 * @return
	 */
	public static double GetNumberAtByte(byte line[], int location) {
		int exponent = ((byte) line[location] & 0xff);
		double fVal = 0;
		// exponent of zero marks this an integer.
		if (exponent != 0) {
			// extract the sign
			boolean sign = (line[location + 1] & 0x80) == 0x80;
			// Extract the 32 bit number, merging "1" in the MSB.
			long value = ((byte) line[location + 1] & 0x7f) + 0x80;
			value = value * 256 + ((byte) line[location + 2] & 0xff);
			value = value * 256 + ((byte) line[location + 3] & 0xff);
			value = value * 256 + ((byte) line[location + 4] & 0xff);

			// Get the the exponent. Note 0x80=2^0
			exponent = exponent - 0x80;

			// output is a floating point value so transfer my long word there.
			fVal = value;

			// note, done like this in stages because Java treats these as 32 bit integers,
			// its too big to do on one go.
			fVal = fVal / 65536;
			fVal = fVal / 65536;
			// shift via the exponent... (2^exponent)
			fVal = fVal * Math.pow(2, exponent);
			// negate if required.
			if (sign) {
				fVal = -fVal;
			}
		} else {
			// Read the number[2,3] as a 16 bit int
			fVal = (line[location + 2] & 0xff) + ((line[location + 3] & 0xff) * 256);
		}
		return (fVal);
	}

	/**
	 * Decode a single basic line
	 * 
	 * @param sb               String builder to add the resultant text to
	 * @param line             the bytes containing the line
	 * @param offset           start of the line (After its line number and length
	 *                         words)
	 * @param linelength       Line length
	 * @param DisplayValueOnly if TRUE wont try to extract the real FP value stored
	 *                         after the text for numbers.
	 */
	public static void DecodeBasicLine(StringBuilder sb, byte line[], int offset, int linelength,
			boolean DisplayValueOnly) {
		int ptr = 0;
		// REMs are treated differently. Just output the characters. Don't try to
		// tokenise them.
		boolean inrem = ((int) (line[0] & 0xff) == 0xEA);
		while (ptr < linelength - 1) {
			int chr = (int) line[ptr] & 0xff;
			if (inrem || (chr != NUMSTART)) {
				String tokenvalue = Speccy.tokens[chr];
				if (DisplayValueOnly) {
					tokenvalue = tokenvalue.replace("&gt;", ">");
					tokenvalue = tokenvalue.replace("&lt;", "<");
					tokenvalue = tokenvalue.replace("GOSUB", "GO SUB");
					tokenvalue = tokenvalue.replace("GOTO", "GO TO");
				}
				
				sb.append(tokenvalue);
			} else {
				if (!DisplayValueOnly) {
					double fpv = Speccy.GetNumberAtByte(line, ptr + 1);
					sb.append("(");
					sb.append(String.valueOf(fpv));
					sb.append(")");
				}
				ptr = ptr + 5; // skip the number
			}
			ptr++;
		}
	}

	/**
	 * Take the passed in FILE and append a basic listing to the stringbuilder
	 * 
	 * @param file             - data to parse
	 * @param sb               - string buiffer
	 * @param header           - +3 dos header, mainly used to determine the end of
	 *                         the program area.
	 * @param DisplayValueOnly - Dont display the actual value of any numbers after
	 *                         the real one.
	 */
	public static void DecodeBasicFromLoadedFile(byte file[], StringBuilder sb, Plus3DosFileHeader header,
			boolean DisplayValueOnly) {
		int ptr = 128; // skip the file header
		while (ptr < file.length) {
			int linenum = ((file[ptr++] & 0xff) * 256);
			linenum = linenum + (file[ptr++] & 0xff);
			int linelen = (int) file[ptr++] & 0xff;
			linelen = linelen + ((int) (file[ptr++] & 0xff) * 256);

			if (ptr >= header.VariablesOffset + 0x80) {
				// now into the variables area. Ignoring for the moment.
				ptr = file.length;
			} else {
				String sixdigit = String.valueOf(linenum);
				while (sixdigit.length() < 6) {
					sixdigit = sixdigit + " ";
				}

				sb.append(sixdigit);

				byte line[] = new byte[linelen];
				for (int i = 0; i < linelen; i++) {
					line[i] = file[ptr + i];
				}

				Speccy.DecodeBasicLine(sb, line, 0, linelen, DisplayValueOnly);

				// point to next line.
				ptr = ptr + linelen;

				sb.append(System.lineSeparator());
			}
		}
	}

	/**
	 * Copied from:
	 * https://stackoverflow.com/questions/1102891/how-to-check-if-a-string-is-numeric-in-java
	 * Returns if a given number is a valid number.
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Decode the variables area of a basic program.
	 * Uses the +3 header to determine where this starts.
	 * 
	 * @param file
	 * @param sb
	 * @param header
	 */
	public static void DecodeVariablesFromLoadedFile(byte[] file, StringBuilder sb, Plus3DosFileHeader header) {
		int ptr = 128; // skip the file header
		ptr = ptr + header.VariablesOffset;
		if (ptr >= file.length) {
			sb.append("No variables");
		} else {
			while (ptr < header.filelength+0x80) {
				int var = (int) (file[ptr++] & 0xff);
				int vartype = var /0x20;
				if (var == 0x00) {
					//anything after this marker is junk so just skip it. 
					sb.append("End of variables<br>\r\n");
					ptr = file.length;
				} else if (vartype == 1) {
					sb.append("unknown type");
				} else if (vartype == 2) { // string
					ptr = VariableType2(sb, ptr, var, file );
				} else if (vartype == 3) { // number (1 letter)
					ptr = VariableType3(sb, ptr, var, file );
				} else if (vartype == 4) { // Array of numbers
					ptr = VariableType4(sb, ptr, var, file );
				} else if (vartype == 5) { // Number who's name is longer than 1 letter
					ptr = VariableType5(sb, ptr, var, file );
				} else if (vartype == 6) { // array of characters
					ptr = VariableType6(sb, ptr, var, file );
				} else if (vartype == 7) { // for/next control variable
					ptr = VariableType7(sb, ptr, var, file );
				} else {
					System.out.print("UNKNOWN! $" + Integer.toHexString(var)+" at "+ptr);
				}				
				
			}
		}
	}
	
	/**
	 * Handler for type 2 variables (Strings)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x40
	 * [1..2] String length lsb first
	 * [3..x] Characters making up the string. 
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType2(StringBuilder sb, int Address, int chr, byte[] file ) {
		int varname = chr & 0x3f;
		varname = varname + 0x40;
		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int length =  (msb*256) + lsb;
		String s = "";
		sb.append("String " + String.valueOf((char) varname) + "$ ("
				+ String.valueOf(length) + " bytes)= \"");
		while (length > 0) {
			char c = (char) file[Address++];
			s = s + DecodeToken(""+c);
			length--;
		}
		sb.append(s + "\"\r\n");
		return(Address);
	}
	
	/**
	 * Handler for type 3 variables (Numbers with a one character name)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x40
	 * [12345] Speccy floating point representation of the value.  
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType3(StringBuilder sb, int Address, int  chr, byte[] file ) {
		int varname = chr & 0x3f;
		varname = varname + 0x40;
		double value = GetNumberAtByte(file,Address);
		String txt = "Number " + String.valueOf((char) varname) + "=" + String.valueOf(value);
		sb.append(txt + "\r\n");
		Address = Address + 5;
		return(Address);
	}
	
	/**
	 * Handler for type 4 variables (Numeric arrays)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x40
	 * format is:
	 *  [0-1] data length (Used to quickly skip over variable when searching)
	 *  [2] Number of dimensions (1-255)
	 *  [3.4] First dimension size. (1-65535)
	 *  ..
	 *  [xx.yy] last dimension size
	 *  [ZZZZZ] Speccy FP representation of (1[,1[,1]]) 
	 *  [ZZZZZ] Speccy FP representation of (2[,1[,1]]) 
	 *  ... 
	 *  [ZZZZZ] Speccy FP representation of (<sz>[,1[,1]]) 
	 *  [ZZZZZ] Speccy FP representation of (1[,2[,1]]) 
	 *  and so on.
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType4(StringBuilder sb, int Address, int  chr, byte[] file ) { 
		try {
		int varname = chr & 0x3f;
		varname = varname + 0x40;
		String txt = "Number array " + String.valueOf((char) varname) + "(";
		Address = Address + 2;
		int dimensions =  (file[Address++] & 0xff);
	
		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			int lsb = (file[Address++] & 0xff);
			int msb = (file[Address++] & 0xff);
			dims[i] = (msb*256) + lsb;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			dimcounts[i] = 1;
		}
		txt = txt + ") = {\r\n  ";
		boolean first = false;
		boolean done = false;
		while (!done) {
			double val = GetNumberAtByte(file, Address);
			Address = Address + 5;
			txt = txt + "(";
			for (int i = 0; i < dimensions; i++) {
				if (i != 0) {
					txt = txt + ",";
				}
				txt = txt + String.valueOf(Math.round(dimcounts[i]));
			}
			txt = txt + ")=" + String.valueOf(val);
			boolean decdone = false;
			int dimid = dimensions - 1;
			while (!decdone) {
				int num = dimcounts[dimid];
				num++;
				if (num > dims[dimid]) {
					dimcounts[dimid] = 1;
					if (dimid == dimensions - 1) {
						txt = txt + "\r\n  ";
					} else {
						txt = txt + "\r\n\r\n  ";
					}
					dimid--;
					if (dimid == -1) {
						decdone = true;
						done = true;
					}
					first = true;
				} else {
					if (!first) {
						txt = txt + ", ";
					}
					first = false;
					dimcounts[dimid] = num;
					decdone = true;
				}
			}
		}
		txt = txt.trim() + "\r\n}\r\n";

		sb.append(txt); //.replace("\r\n", "<br>\r\n") + "<br>");
		} catch (Exception E) {
			E.printStackTrace();
			System.out.println(E.getMessage());
		}

		return(Address);
	}
	
	/**
	 * Handler for type 5 variables (Number with a name > 1)
	 * Format:
	 *   (Original char) [101XXXXX] where XXXXX = char of name - 0x40
	 *   [0] [000XXXXX] where XXXXX = char of name - 0x40
	 *   ...
	 *   [Y] [100XXXXX] where XXXXX = char of name - 0x40 (Bit 7 is set to terminate string)
	 *   [N1..N5] Speccy Floating point number
	 *   
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType5(StringBuilder sb, int Address, int  chr, byte[] file ) { 
		sb.append("Number ");
		boolean done = false;
		while (!done) {
			int varname = (chr & 0x3f);
			varname = varname +0x40;
			sb.append(String.valueOf((char) varname));
			chr = file[Address++];
			done = (chr & 0x80) == 0x80;
		}
		int varname = (chr & 0x3f) +0x40;
		sb.append(String.valueOf((char) varname));

		sb.append(" = ");

		double value = GetNumberAtByte(file, Address);
		sb.append(String.valueOf(value).toString() + "<br>");
		Address = Address + 5;
		return(Address);
	}
	
	/**
	 * Handler for type 6 variables (Character arrays)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x40
	 * format is:
	 *  [0-1] data length (Used to quickly skip over variable when searching)
	 *  [2] Number of dimensions (1-255)
	 *  [3.4] First dimension size.(1-65535)
	 *  ..
	 *  [xx.yy] last dimension size
	 *  [z] char for (1,0)
	 *  [z] char for (2,0)
	 *  ... 
	 *  [z] char for (<sz>,0)
	 *  [z] char for (1,1)  
	 *  and so on.
	 *  
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType6(StringBuilder sb, int Address, int  chr, byte[] file ) {  
		int varname = (chr & 0x3f);
		varname = varname +0x40;
		String txt = "Character array " + String.valueOf((char) varname) + "$(";
		Address = Address + 2;
		int dimensions =  (file[Address++] & 0xff);
		
		int dims[] = new int[dimensions];
		int dimcounts[] = new int[dimensions];
		for (int i = 0; i < dimensions; i++) {
			int lsb = (file[Address++] & 0xff);
			int msb = (file[Address++] & 0xff);
			dims[i] = (msb*256) + lsb;
			if (i > 0) {
				txt = txt + ",";
			}
			txt = txt + String.valueOf(dims[i]);
			dimcounts[i] = 1;
		}
		txt = txt + ") = {\r\n  ";
		boolean first = false;
		boolean done = false;
		while (!done) {
			char chracter =  (char) file[Address++];
			txt = txt + "(";
			for (int i = 0; i < dimensions; i++) {
				if (i != 0) {
					txt = txt + ",";
				}
				txt = txt + String.valueOf(Math.round(dimcounts[i])).toString();
			}
			txt = txt + ")=" + DecodeToken(""+chracter);
			boolean decdone = false;
			int dimid = dimensions - 1;
			while (!decdone) {
				int num = dimcounts[dimid];
				num++;
				if (num > dims[dimid]) {
					dimcounts[dimid] = 1;
					if (dimid == dimensions - 1) {
						txt = txt + "\r\n  ";
					} else {
						txt = txt + "\r\n\r\n  ";
					}
					dimid--;
					if (dimid == -1) {
						decdone = true;
						done = true;
					}
					first = true;
				} else {
					if (!first) {
						txt = txt + ", ";
					}
					first = false;
					dimcounts[dimid] = num;
					decdone = true;
				}
			}
		}
		txt = txt.trim() + "\r\n}\r\n";

		sb.append(txt); //.replace("\r\n", "<br>\r\n") + "<br>");
		return(Address);		
	}

	/**
	 * Handler for type 7 variables (FOR/NEXT variables)
	 * Variable name is parsed from the original marker (first 5 bytes) + 0x40
	 *  format is:
	 *  byte:
	 *  [0-4] 	Current Value of variable (Speccy FP representation)
	 *  [5-9] 	TO value  (Speccy FP representation)
	 *  [10-14] STEP value (Speccy FP representation)
	 *  [15] statement within FOR line to start looping from. (integer)
	 * 
	 * Like the previous ones, this is similar to the ZX81 8k Rom equivelent, except the addition of byte 15
	 * (As the zx81 does not allow multiple statements on a line) 
	 * 
	 * @param sb
	 * @param keys
	 * @param Address
	 * @param chr
	 * @return
	 * @throws Exception
	 */
	private static int VariableType7(StringBuilder sb, int Address, int  chr, byte[] file ) {  
		int varname = (chr & 0x3f);
		varname = varname +0x40;
		String txt = "For/Next " + String.valueOf((char) varname);

		txt = txt + " Value=" + String.valueOf(GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " Limit=" + String.valueOf(GetNumberAtByte(file, Address));
		Address = Address + 5;
		txt = txt + " Step=" + String.valueOf(GetNumberAtByte(file, Address));
		Address = Address + 5;
		
		int lsb = (file[Address++] & 0xff);
		int msb = (file[Address++] & 0xff);
		int line =  (msb*256) + lsb;
		txt = txt + " Loop line=" + String.valueOf(line);
		
		int statement = (file[Address++] & 0xff);
		
		txt = txt + " Next Statement in line: "+ String.valueOf(statement) +"\r\n";

		sb.append(txt);
		return(Address);
	}
	

}
