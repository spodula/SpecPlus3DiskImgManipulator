package dialogs;
/**
 * Dialog for adding a encoding a TEXT file as basic. 
 * Note, it does no syntax checking of the file, so if its not correct, 
 * you will end up with Nonsense in basic errors when trying to run it. 
 *  
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import diskviewer.libs.Speccy;
import diskviewer.libs.disk.cpm.CPM;

public class basicDialog extends DiskReaderDialog {
	//Filename of the source file
	public String filename = "";
	
	//Filename of the file on the disk.  
	public String NameOnDisk = "";
	
	//The listview used for the basic program on the form.  
	private List BasicLines = null;
	
	//Start line. 0x8000 means none defined.
	public int StartLine = 0x8000;

	//if TRUE, the OK button was pressed, else FALSE
	private boolean IsOk = false;
	
	//Tokenised basic program. (Only valid if IsOk is true.)
	public byte BasicAsBytes[] = new byte[1];
	
	//Next location in the array above when parsing the file.  
	public int TargetPtr=0;
	

	/**
	 * Test used to check conversion from ASCII to tokenised basic. 
	 * @param args
	 */
	public static void main(String[] args) {
		//String line = "540    DRAW 0,s: DRAW -s2,0: DRAW s34,s34: DRAW s34,-s34: DRAW -s2,0: DRAW 0,-s: DRAW -s2,0";
		//String line = "20 SAVE \"m:junk fred\" CODE 1,1";
		String line = "20 GO SUB 232";
		
		Display display = new Display();
		Shell shell = new Shell(display);
		basicDialog bd = new basicDialog(shell);
		bd.BasicAsBytes = new byte[32768];
		bd.TargetPtr = 0;
		String s = bd.DecodeBasicLine(line);
		System.out.println("Source: "+line);
		System.out.println("result: "+s);
		int ptr=0;
		String hex="0123456789abcdef";
		for(int i=0;i<bd.TargetPtr;i++) {
			int byt = (bd.BasicAsBytes[ptr++] & 0xff);
			int hi = byt / 0x10;
			int low = byt & 0x0f;
			System.out.print(hex.charAt(hi)+""+hex.charAt(low)+" ");
			if ((ptr & 0x0f) == 0) {
				System.out.println();
			}
		}
		System.out.println();
		
	}
	
	/**
	 * constructor
	 * @param parent
	 */
	public basicDialog(Shell parent) {
			super(parent);
	}
	

	/**
	 * Create and display the dialog.
	 * 
	 * @return
	 */
	public boolean open() {
		IsOk = false;
		Shell parent = getParent();
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setSize(700, 500);
		dialog.setText("Add BASIC file from text file");
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		
		
		
		cp.setBackground(new Color( dialog.getDisplay(), 0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 4;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Add a BASIC file.", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		Button BtnSelFile = GetButton(dialog, "File:", GridData.BEGINNING);
		Text FileNameEdit = GetText(dialog, "", 3);

		Text DiskName = captionAndText(dialog, "Name on Disk", "");
		
		Text txtStartline = captionAndText(dialog, "Start line (blank = none)", "");

		BasicLines = new List(dialog, SWT.BORDER | SWT.V_SCROLL);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 4;
		data.verticalSpan = 4;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		BasicLines.setLayoutData(data);

		BasicLines.add("Select a file");

		GetLabel(dialog, "", 2);

		BtnSelFile.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				FileDialog fd = new FileDialog(dialog, SWT.OPEN);
				fd.setText("Select file to add");
				fd.setFilterPath(DefaultFolder);
				fd.setFilterExtensions(new String[] { "*.*" });
				fd.setFileName("");
				String selected = fd.open();
				if (!selected.isBlank()) {
					SetDefaultFolderFromFile( selected );
					FileNameEdit.setText(selected);
					File f = new File(selected);

					// NameOnDisk
					DiskName.setText(CPM.FixFullName(f.getName()));

					LoadBasicFromFile(selected);
				}
			}
		});

		Button BtnOK = GetButton(dialog, "Add", GridData.FILL);
		BtnOK.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				filename = FileNameEdit.getText();
				NameOnDisk = DiskName.getText();
				boolean DoCancel=true;
				if (txtStartline.getText().isBlank()) {
					StartLine = 0x8000;
				} else {
					try {
						StartLine = Integer.parseInt(txtStartline.getText());
						if (StartLine > 9999) {
							throw new Exception("Bad number"); 
						}
					} catch (Exception E) {
						System.out.println("Bad start line");
						MessageBox box = new MessageBox(parent, SWT.OK);
						box.setText("Start line invalid");
						box.setMessage("The start line is invalid\r\nEither leave the field blank or enter a number between 0 and 9999");

						box.open();
						DoCancel = false;
					}
				}
				if (DoCancel) {
					IsOk = true;
					dialog.dispose();
				}
			}
		});

		Button btnCancel = GetButton(dialog, "Cancel", GridData.FILL);
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				filename = "";
				NameOnDisk = "";
				IsOk = false;
				dialog.dispose();
			}
		});

		dialog.open();
		Display display = parent.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		return (IsOk);
	}

	/**
	 * 
	 * @param filename
	 */
	private void LoadBasicFromFile(String filename) {
		int filelen = (int) new File(filename).length();
		BasicAsBytes = new byte[filelen*3];
		TargetPtr = 0;
		
		BasicLines.removeAll();
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			try {
				String line;
				while ((line = br.readLine()) != null) {
					String s = DecodeBasicLine(line);
					BasicLines.add(s);
				}
			} finally {
				br.close();
			}
			
			//allocate the final array, 
			byte newarray[] = new byte[TargetPtr];
			for(int i=0;i<TargetPtr;i++) {
				newarray[i] = BasicAsBytes[i];
			}
			BasicAsBytes = newarray;
			
			
		} catch (Exception E) {
			E.printStackTrace();
		}
	}

	/**
	 * Add to a byte array.
	 * @param s
	 * @return
	 */
	private String DecodeBasicLine(String Line) {
		ArrayList<Byte> NewLine = new ArrayList<Byte>();
		Line = Line.trim();
		String err = "";
		String pline = "";
		// split line
		ArrayList<String> TokenList = SplitLine(Line);
		// read the line number
		log("-------------------------------------------");
		log("Source: "+Line);
		if (TokenList.size() > 0) {
			// get the initial token
			String token = TokenList.get(0);
			int linenum = 0;
			try {
				linenum = Integer.parseInt(token);
			} catch (NumberFormatException nfe) {
				err = "Bad lineno: " + linenum;
			}
			pline = String.valueOf(linenum) + " ";
			if (err.isBlank()) {
				int tokenptr = 1;
				log(pline + " ");
				while (tokenptr < TokenList.size()) {
					token = TokenList.get(tokenptr++);
					logNoCR("Token: "+token+" ///");
					String tkn = Speccy.DecodeToken(token);
					for(int i=0;i<tkn.length();i++) {
						int c = tkn.charAt(i);
						if (i!=0) {
							logNoCR(",");
						}
						logNoCR(""+ (char) (c & 0xff));
						NewLine.add((byte) c );
					}
					logNoCR("\r\n");
				}

			}
			//Add in the EOL chararacter.
			NewLine.add((byte) 0x0d);
			
			
			//Add in the line number
			BasicAsBytes[TargetPtr++] = (byte) ((linenum / 0x100) & 0xff);
			BasicAsBytes[TargetPtr++] = (byte) (linenum & 0xff);
			//Add in the line size
			BasicAsBytes[TargetPtr++] = (byte) (NewLine.size() & 0xff);
			BasicAsBytes[TargetPtr++] = (byte) ((NewLine.size() / 0x100) & 0xff);
			//copy line into byte array
			for(byte b:NewLine) {
				BasicAsBytes[TargetPtr++] = b;
			}

		}
		
		return (Line);
	}

	/**
	 * split the given line into sections. Will do this using a state machine.
	 */

	private static int STATE_NONE = 0;
	private static int STATE_NUMBER = 1;
	private static int STATE_STRING = 2;
	private static int STATE_MISC = 3;
	private static int STATE_OPERATOR = 4;
	private static int STATE_REM = 5;
	/**
	 * split the given line into sections. Will do this using a state machine.
	 * 
	 * @param s
	 * @return
	 */
	private ArrayList<String> SplitLine(String line) {
		//some preprocessing
		line = CISreplace(line, "GO SUB", "GOSUB");
		line = CISreplace(line, "GO TO", "GOTO");
		line = CISreplace(line, "CLOSE #", "CLOSE#");
		line = CISreplace(line, "OPEN #", "OPEN#");
		
		int state = STATE_NONE;
		String curritem = "";
		ArrayList<String> result = new ArrayList<String>();
		for (int i = 0; i < line.length(); i++) {
			char chr = line.charAt(i);
			if (state == STATE_REM) {
				//if we have found a rem, just add everything from here on. 
				//Dont try to switch states going forward. 
				curritem = curritem + chr;
			} else if (state == STATE_NONE) {
				// if we are in state_none, swtich to another state.
				if (IsNumber(chr)) {
					state = STATE_NUMBER;
					curritem = curritem + chr;
				} else if (IsOperator(chr)) {
					state = STATE_OPERATOR;
					curritem = curritem + chr;
				} else if (chr == '"') {
					state = STATE_STRING;
					curritem = curritem + '"';
				} else if (chr != ' ') {
					state = STATE_MISC;
					curritem = curritem + chr;
				}
			} else if (state == STATE_NUMBER) {
				if (IsNumber(chr)) {
					curritem = curritem + chr;
				} else {
					result.add(curritem);
					curritem = "";
					// ok we are not a number, Lets decide the next state
					if (IsSeperator(chr)) {
						result.add(""+chr);
						state = STATE_MISC;
					} else if (IsOperator(chr)) {
						state = STATE_OPERATOR;
						curritem = curritem + chr;
					} else if (chr == '"') {
						curritem = curritem + '"';
						state = STATE_STRING;
					} else if (chr != ' ') {
						state = STATE_MISC;
						curritem = curritem + chr;
					}
				}
			} else if (state == STATE_STRING) {
				if (chr == '"') {
					state = STATE_NONE;
					curritem = curritem + '"';
					result.add(curritem);
					curritem = "";
				} else {
					curritem = curritem + chr;
				}
			} else if (state == STATE_MISC) {
				if  (curritem.toUpperCase().contentEquals("REM")) {
					result.add(curritem);
					curritem = "" + chr;
					state = STATE_REM;
				}
				if (IsNumber(chr)) {
					//are we a continuation of an identifier? If so, dont switch state.
					if (curritem.isEmpty()) {
						state = STATE_NUMBER;
						result.add(curritem);
						curritem = "" + chr;
					} else {
						curritem = curritem + chr;
					}
				} else if (IsOperator(chr)) {
					state = STATE_OPERATOR;
					result.add(curritem);
					curritem = "" + chr;
				} else if (chr == '"') {
					state = STATE_STRING;
					result.add(curritem);
					curritem = "\"";
				} else if (IsSeperator(chr)) {
					result.add(curritem);
					result.add("" + chr);
					curritem = "";

				} else if (chr != ' ') {
					curritem = curritem + chr;
				} else { // is space
					result.add(curritem);
					curritem = "";
				}
			} else if (state == STATE_OPERATOR) {
				if (IsOperator(chr)) {
					curritem = curritem + chr;
				} else {
					result.add(curritem);
					curritem = "";
					// ok we are not a number, Lets decide the next state
					if (IsSeperator(chr)) {
						result.add(""+chr);						
					} else if (IsNumber(chr)) {
						state = STATE_NUMBER;
						curritem = curritem + chr;
					} else if (chr == '"') {
						state = STATE_STRING;
						curritem = "\"";
					} else if (chr != ' ') {
						state = STATE_MISC;
						curritem = curritem + chr;
					}
				}

			}

		}
		result.add(curritem);

		// remove the spaces
		log("---------------");
		log(line);
		ArrayList<String> result2 = new ArrayList<String>();
		for (String sr : result) {
			sr = sr.trim();
			if (!sr.isBlank()) {
				result2.add(sr);
				logNoCR(";;;"+sr);
			}
		}
		log("");
		return (result2);
	}

	/**
	 * Is the character part of a number?
	 * 
	 * @param chr
	 * @return
	 */
	private boolean IsNumber(char chr) {
		String numbers = "0123456789.";
		return (numbers.indexOf(chr) > -1);
	}

	/**
	 * Is the character a logical or math operator?
	 * @param chr
	 * @return
	 */
	private boolean IsOperator(char chr) {
		String operators = "()+-/*<>&=";
		return (operators.indexOf(chr) > -1);
	}

	/**
	 * Is the character something used to separate statements?
	 * 
	 * @param chr
	 * @return
	 */
	private boolean IsSeperator(char chr) {
		String seperators = ":, ";
		return (seperators.indexOf(chr) > -1);
	}

	/**
	 * Case insensitive replace from https://stackoverflow.com/questions/5054995/how-to-replace-case-insensitive-literal-substrings-in-java
	 * 
	 *  @param source
	 *  @param target
	 *  @param replacement
	 */
	public String CISreplace(String source, String target, String replacement)
    {
        StringBuilder sbSource = new StringBuilder(source);
        StringBuilder sbSourceLower = new StringBuilder(source.toLowerCase());
        String searchString = target.toLowerCase();

        int idx = 0;
        while((idx = sbSourceLower.indexOf(searchString, idx)) != -1) {
            sbSource.replace(idx, idx + searchString.length(), replacement);
            sbSourceLower.replace(idx, idx + searchString.length(), replacement);
            idx+= replacement.length();
        }
        sbSourceLower.setLength(0);
        sbSourceLower.trimToSize();
        sbSourceLower = null;

        return sbSource.toString();
    }
	
}
