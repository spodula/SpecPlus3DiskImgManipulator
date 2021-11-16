package dialogs;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class RenameDialog extends DiskReaderDialog {
	//Source filename
	public String OldFilename = "";
	
	//New filename
	public String NewFilename = "";

	//if TRUE, the OK button was pressed, else FALSE
	private boolean IsOk = false;

	//basic constructor
	public RenameDialog(Shell parent) {
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
		dialog.setSize(500, 120);
		dialog.setText("Rename "+OldFilename);
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		cp.setBackground(new Color(dialog.getDisplay(),0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 4;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Rename "+OldFilename, 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		Text DiskName = captionAndText(dialog, "New filename: ", OldFilename);

		GetLabel(dialog, "", 2);

		Button BtnOK = GetButton(dialog, "Rename", GridData.FILL);
		BtnOK.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				NewFilename = DiskName.getText();
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
				NewFilename = "";
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

}
