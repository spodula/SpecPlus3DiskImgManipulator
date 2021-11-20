package dialogs;

import java.io.BufferedReader;
import java.io.IOException;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import diskviewer.libs.Speccy;
import diskviewer.libs.disk.cpm.CPM;

public class NumericArrayDialog extends DiskReaderDialog {
	// Filename of the source file
	public String filename = "";

	// Filename of the file on the disk.
	public String NameOnDisk = "";
	
	//variable name
	public char varname='F';

	// The listview used for the basic program on the form.
	private Table arraylist = null;

	// Tokenised basic program. (Only valid if IsOk is true.)
	public byte ArrayAsBytes[] = new byte[1];

	// Next location in the array above when parsing the file.
	public int TargetPtr = 0;

	// flag to mark if we should exit or not.
	public boolean IsOk = false;

	private int maxdim = 1;

	private ArrayList<String> lines = new ArrayList<String>();

	/**
	 * constructor
	 * 
	 * @param parent
	 */
	public NumericArrayDialog(Shell parent) {
		super(parent);
	}

	/**
	 * Create and display the dialog.
	 * 
	 * @return
	 */
	public boolean open() {
		Shell parent = getParent();
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setSize(700, 500);
		dialog.setText("Add Numeric array");
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);

		cp.setBackground(new Color(dialog.getDisplay(), 0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 4;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Add a Numeric array.", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		Button BtnSelFile = GetButton(dialog, "File:", GridData.BEGINNING);
		Text FileNameEdit = GetText(dialog, "", 3);
		Text DiskName = captionAndText(dialog, "Name on Disk", "");

		GetLabel(dialog, "Var Name.", 2);
		Text VarNameEdit = GetText(dialog, "X", 1);
		Label VarSizeLabel = GetLabel(dialog, "(x,x)", 1);

		arraylist = new Table(dialog, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 4;
		data.verticalSpan = 4;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		arraylist.setLayoutData(data);
		TableColumn column = new TableColumn(arraylist, SWT.NULL);
		column.setText("Select a file....");
		column.pack();
		arraylist.setLinesVisible(true);
		arraylist.setHeaderVisible(true);

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
					SetDefaultFolderFromFile(selected);
					FileNameEdit.setText(selected);
					File f = new File(selected);

					// NameOnDisk
					DiskName.setText(CPM.FixFullName(f.getName()));

					LoadArrayFromFile(f);

					DisplayWithCurrentSettings(dialog, VarSizeLabel);
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
				varname = (VarNameEdit.getText()+"X").toUpperCase().charAt(0);
				IsOk = true;
				AssembleArrayData(VarNameEdit.getText());

				dialog.dispose();
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
	 * Update the table from the current loaded file.
	 * 
	 */
	protected void DisplayWithCurrentSettings(Shell dialog, Label VarNameLabel) {
		Color red = dialog.getDisplay().getSystemColor(SWT.COLOR_RED);
		boolean FirstLineHeader = false;
		// clear table
		arraylist.setHeaderVisible(true);
		arraylist.removeAll();

		while (arraylist.getColumnCount() > 0) {
			arraylist.getColumn(0).dispose();
		}

		// get dimensions.
		maxdim = 1;
		for (String line : lines) {
			String columns[] = SplitLine(line, ", \t");
			if (columns.length > maxdim)
				maxdim = columns.length;
		}
		log("Max column width is " + maxdim);

		// Set the headers.
		String columns[] = SplitLine(lines.get(0), ", \t");
		for (int i = 0; i < maxdim; i++) {
			String s = String.valueOf("#" + (i + 1));
			if (FirstLineHeader) {
				if (i < columns.length) {
					s = columns[i];
				}
			}
			TableColumn column = new TableColumn(arraylist, SWT.NULL);
			column.setText(s);
		}
		// get the first line number.
		int startline = 0;
		if (FirstLineHeader) {
			startline = 1;
		}

		// for each line
		for (int i = startline; i < lines.size(); i++) {
			String linedata[] = SplitLine(lines.get(i), ", \t"); // split line
			// add to table
			TableItem item = new TableItem(arraylist, SWT.NULL);
			for (int col = 0; col < linedata.length; col++) {
				String data = linedata[col];
				item.setText(col, data);
				if (!isNumeric(data)) {
					item.setForeground(col, red);
				}
			}
		} // end for

		for (int i = 0; i < arraylist.getColumnCount(); i++) {
			arraylist.getColumn(i).pack();
		}
		String s = "(" + lines.size();
		if (maxdim > 1) {
			s = s + "," + maxdim;
		}

		VarNameLabel.setText(s + ")");
	}

	/**
	 * very basic parser.
	 *
	 * @param line
	 * @param splitby
	 * @return
	 */
	private String[] SplitLine(String line, String splitby) {
		ArrayList<String> al = new ArrayList<String>();
		String curritem = "";
		boolean InQuotes = false;
		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if (InQuotes) {
				if (c == '"') {
					InQuotes = false;
					if (!curritem.isEmpty()) {
						al.add(curritem);
						curritem = "";
					}
				} else {
					curritem = curritem + c;
				}
			} else {
				if (c == '"') {
					InQuotes = true;
					if (!curritem.isEmpty()) {
						al.add(curritem);
						curritem = "";
					}
				} else {
					if (splitby.indexOf(c) > -1) {
						if (!curritem.isEmpty()) {
							al.add(curritem);
							curritem = "";
						}
					} else {
						curritem = curritem + c;
					}

				}
			}
		}

		String result[] = line.split(",");
		return result;
	}

	/**
	 * Load the files into the string array.
	 * 
	 * @param f
	 */
	protected void LoadArrayFromFile(File f) {
		lines.clear();
		try {
			int numlines = 0;
			String line;
			BufferedReader br = new BufferedReader(new FileReader(f));
			try {
				while (((line = br.readLine()) != null) && numlines < 10000) {
					lines.add(line);
					numlines++;
				}
			} finally {
				br.close();
			}
			if (numlines == 10000) {
				System.out.println("Load stopped at 10000 lines. Too large");
			} else {
				System.out.println("Loaded " + numlines + " lines.");
			}
		} catch (IOException E) {
			System.out.println("Error reading file " + f.getName());
			System.out.println(E.getMessage());
		}
	}

	/**
	 * Assemble the data into a byte array. We are limited to 1 or 2 dimensions here. 
	 * This is a limitation of the CSV files. 
	 * 
	 * 
	 * @param text
	 */
	private void AssembleArrayData(String text) {
		//number of diumensions.
		int dimensions=1;
		if(maxdim > 1) {
			dimensions = 2;
		}
		
		int arraysize = (maxdim * lines.size() * 5) + (dimensions*2) + 1;
		System.out.println("Calcsize:  "+arraysize);
		ArrayAsBytes = new byte[arraysize];
		
		//dimensions.
		
		int ptr=1;
		//Each dimension.
		ArrayAsBytes[ptr++] = (byte) (lines.size() & 0xff);
		ArrayAsBytes[ptr++] = (byte) (lines.size() / 0x100);
		ArrayAsBytes[0] = 1;
		if (maxdim>1) {
			ArrayAsBytes[0] = 2;
			ArrayAsBytes[ptr++] = (byte) (maxdim & 0xff);			
			ArrayAsBytes[ptr++] = (byte) (maxdim / 0x100);
		}
		
		//for each item.
		for (int dim1 = 0; dim1 < lines.size();dim1++) {
			String line = lines.get(dim1);
			String numbers[] = SplitLine(line, ", \t");
			for (int dim2 = 0; dim2 < maxdim;dim2++) {
				String sNumber = numbers[dim2];
				if (!isNumeric(sNumber)) {
					sNumber = "0";
				}
				Double number = Double.valueOf(sNumber);
				byte[] newnum = Speccy.EncodeValue(number, true);
				for(int i=1;i<newnum.length;i++) {
					ArrayAsBytes[ptr++] = newnum[i]; 
				}
			}
		}
		System.out.println("final ptr: "+ptr);
	}

}
