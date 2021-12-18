package dialogs;
/**
 * Dialog for adding a generic CODE file. 
 * 
 * 
 * 
 */
//dialog example from http://www.java2s.com/Code/JavaAPI/org.eclipse.swt.widgets/extendsDialog.htm

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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
import org.eclipse.swt.widgets.Text;

import diskviewer.libs.disk.cpm.CPM;

public class codeDialog extends DiskReaderDialog {
	//Filename of the source file
	public String filename = "";
	
	//Filename of the file on the disk. 
	public String NameOnDisk = "";

	//output variable - length of the code variable
	public int length = 0;
	
	//output variable - Load address
	public int address = 0;
	
	//if TRUE, the OK button was pressed, else FALSE
	private boolean IsOk = false;
	
	//Output for the file. 
	public byte CodeAsBytes[] = null;

	//basic constructor
	public codeDialog(Shell parent) {
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
		dialog.setSize(500, 230);
		dialog.setText("Add a CODE file");
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		cp.setBackground(new Color(dialog.getDisplay(),0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 4;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Add a CODE file.", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		Button BtnSelFile = GetButton(dialog, "File:", GridData.BEGINNING);
		Text FileNameEdit = GetText(dialog, "", 3);

		Text DiskName = captionAndText(dialog, "Name on Disk", "");
		Text LoadAddress = captionAndText(dialog, "Default load address", "32768");
		Text TextLen = captionAndText(dialog, "Length", "1024");

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
					TextLen.setText(String.valueOf(f.length()));
					LoadAddress.setText(String.valueOf(65536 - f.length()));

					String fname = f.getName().toUpperCase();
					String filename = "";
					String extension = "";
					if (fname.contains(".")) {
						int i = fname.lastIndexOf(".");
						extension = fname.substring(i + 1);
						filename = fname.substring(0, i);
					} else {
						filename = fname;
					}
					filename = filename + "        ";
					filename = CPM.FixFilePart(filename.substring(0, 8).trim());

					extension = extension + "   ";
					extension = CPM.FixFilePart(extension.substring(0, 3).trim());
					// NameOnDisk
					DiskName.setText(filename + "." + extension);
					
					LoadFile( f );

				}
			}
		});

		Button BtnOK = GetButton(dialog, "Add", GridData.FILL);
		BtnOK.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				address = Integer.parseInt(LoadAddress.getText());
				length = Integer.parseInt(TextLen.getText());
				filename = FileNameEdit.getText();
				NameOnDisk = DiskName.getText();
				IsOk = true;
				dialog.dispose();
			}
		});

		Button btnCancel = GetButton(dialog, "Cancel", GridData.FILL);
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				address = 0;
				length = 0;
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
		return (IsOk | (CodeAsBytes != null));
	}
	
	/**
	 * 
	 * @param f
	 */
	private void LoadFile(File f) {
		try {
			int filelen = (int) f.length();

			CodeAsBytes = new byte[filelen];
			// Load file to memory.
			InputStream in = new FileInputStream(f);
			try {
				for (int i = 0x80; i < CodeAsBytes.length; i++) {
					int chr = in.read();
					CodeAsBytes[i] = (byte) (chr & 0xff);
				}
			} finally {
				in.close();
			}
		} catch (Exception E) {
			E.printStackTrace();
			CodeAsBytes = null;
		}
	}


}
