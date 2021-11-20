package dialogs;
/**
 * Dialog for adding a character array to the disk. 
 * Will probably never be used (As its easier to add a text file as a CODE file), but here for consistency. 
 */

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

import diskviewer.libs.disk.cpm.CPM;

public class CharacterArrayDialog extends DiskReaderDialog {
	// Filename of the source file
	public String filename = "";

	// Filename of the file on the disk.
	public String NameOnDisk = "";

	// variable name
	public char varname = 'F';

	// The listview used for the basic program on the form.
	private Table arraylist = null;

	// Tokenised basic program. (Only valid if IsOk is true.)
	public byte ArrayAsBytes[] = new byte[1];

	// Next location in the array above when parsing the file.
	public int TargetPtr = 0;

	// flag to mark if we should exit or not.
	public boolean IsOk = false;

	// Max number of items in the second dimension. (horizontal)
	private int maxdim2 = 1;

	// File storage.
	private ArrayList<String> lines = new ArrayList<String>();

	/**
	 * constructor
	 * 
	 * @param parent
	 */
	public CharacterArrayDialog(Shell parent) {
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

		Label label = GetLabel(cp, "Add a Text file as a char array.", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		Button BtnSelFile = GetButton(dialog, "File:", GridData.BEGINNING);
		Text FileNameEdit = GetText(dialog, "", 3);
		Text DiskName = captionAndText(dialog, "Name on Disk", "");

		GetLabel(dialog, "Var Name.", 2);
		Text VarNameEdit = GetText(dialog, "X", 1);
		Label VarSizeLabel = GetLabel(dialog, "$(x,x)", 1);

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
				varname = (VarNameEdit.getText() + "X").toUpperCase().charAt(0);
				IsOk = true;
				AssembleArrayData();

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
	 * @param dialog       - The main form
	 * @param VarNameLabel - the variable label so we can set (X,X) values
	 */
	private void DisplayWithCurrentSettings(Shell dialog, Label VarNameLabel) {
		// clear table
		arraylist.setHeaderVisible(true);
		arraylist.removeAll();

		while (arraylist.getColumnCount() > 0) {
			arraylist.getColumn(0).dispose();
		}

		// get second dimension from the file.
		maxdim2 = 1;
		for (String line : lines) {
			if (line.length() > maxdim2)
				maxdim2 = line.length();
		}
		log("Number of columns is: " + maxdim2);
		if (maxdim2 > 255) {
			maxdim2 = 255;
			log("Truncated to 255");
		}

		// Set the headers.
		for (int i = 0; i < maxdim2; i++) {
			String s = String.valueOf("#" + (i + 1));
			TableColumn column = new TableColumn(arraylist, SWT.NULL);
			column.setText(s);
		}

		// for each line
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (line.length() > 255) {
				line = line.substring(0, 255);
			}

			// pad line out to at least 255.
			line = line + "                                                                "
					+ "                                                                "
					+ "                                                                "
					+ "                                                                ";

			// add to table
			TableItem item = new TableItem(arraylist, SWT.NULL);
			for (int col = 0; col < line.length(); col++) {
				String data = "" + line.charAt(col);
				item.setText(col, data);
			}
		} // end for

		for (int i = 0; i < arraylist.getColumnCount(); i++) {
			arraylist.getColumn(i).pack();
		}
		String s = "(" + lines.size();
		if (maxdim2 > 1) {
			s = s + "," + maxdim2;
		}

		VarNameLabel.setText(s + ")");
	}

	/**
	 * Load the files into the string array.
	 * 
	 * @param FileToLoad
	 */
	private void LoadArrayFromFile(File FileToLoad) {
		lines.clear();
		try {
			int numlines = 0;
			String line;
			BufferedReader br = new BufferedReader(new FileReader(FileToLoad));
			try {
				while (((line = br.readLine()) != null) && numlines < 255) {
					lines.add(line);
					numlines++;
				}
			} finally {
				br.close();
			}
			if (numlines == 255) {
				System.out.println("Load stopped at 255 lines. Too large");
			} else {
				System.out.println("Loaded " + numlines + " lines.");
			}
		} catch (IOException E) {
			System.out.println("Error reading file " + FileToLoad.getName());
			System.out.println(E.getMessage());
		}
	}

	/**
	 * Assemble the data into a data array. We are limited to 1 or 2 dimensions.
	 * This is a limitation of the CSV files.
	 * 
	 * Format of the file is: 0: number of dimensions. 01-02: Dimension 1 size
	 * [03-04]: Dimension 2 size if applicable. (1 byte): char for (1[,1]) (1 byte):
	 * char for (2[,1]) ..... (1 byte): char for (N[,1]) (1 byte): char for (1[,2])
	 * 
	 */
	private void AssembleArrayData() {
		// number of diumensions. For char arrays, probably two.
		int dimensions = 1;
		if (maxdim2 > 1) {
			dimensions = 2;
		}

		// dimensions. ((X * Y) Data) + 2 bytes for each dimension + 1 for dimension no.
		int arraysize = (maxdim2 * lines.size()) + (dimensions * 2) + 1;
		System.out.println("calculate file size:  " + arraysize);
		ArrayAsBytes = new byte[arraysize];
		int ptr = 1;

		// Each dimension.
		ArrayAsBytes[ptr++] = (byte) (lines.size() & 0xff);
		ArrayAsBytes[ptr++] = (byte) (lines.size() / 0x100);
		ArrayAsBytes[0] = 1;
		if (maxdim2 > 1) {
			ArrayAsBytes[0] = 2;
			ArrayAsBytes[ptr++] = (byte) (maxdim2 & 0xff);
			ArrayAsBytes[ptr++] = (byte) (maxdim2 / 0x100);
		}

		// for each item.
		for (int dim1 = 0; dim1 < lines.size(); dim1++) {
			// pad line to at least 255 characters
			String line = lines.get(dim1) + "                                                                "
					+ "                                                                "
					+ "                                                                "
					+ "                                                                ";
			// write string to the array
			for (int dim2 = 0; dim2 < maxdim2; dim2++) {
				char c = line.charAt(dim2);
				ArrayAsBytes[ptr++] = (byte) c;
			}
		}
		System.out.println("final ptr: " + ptr);
	}

}
