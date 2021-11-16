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

public class FormCloseDialog extends DiskReaderDialog {
	public static int CLOSE = 0;
	public static int SAVE = 1;
	public static int SAVEAS = 2;
	public static int CANCEL = 3;
	
	int result=0;
	
	/**
	 * constructor
	 * @param parent
	 */
	public FormCloseDialog(Shell parent) {
			super(parent);
	}
	

	/**
	 * Create and display the dialog.
	 * 
	 * @return
	 */
	public int open() {
		Shell parent = getParent();
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setSize(400, 110);
		dialog.setText("Are you sure you want to close?");
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		cp.setBackground(new Color(dialog.getDisplay(), 0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 4;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "There are unsaved changed. \r\nDo you want to Ignore, save or close?", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);


		Button BtnOK = GetButton(dialog, "Close", GridData.FILL);
		BtnOK.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				result = CLOSE;
				dialog.dispose();
			}
		});
		Button BtnSave = GetButton(dialog, "Save", GridData.FILL);
		BtnSave.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				result = SAVE;
				dialog.dispose();
			}
		});
		Button BtnSaveAs = GetButton(dialog, "Save as", GridData.FILL);
		BtnSaveAs.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				result = SAVEAS;
				dialog.dispose();
			}
		});

		Button btnCancel = GetButton(dialog, "Cancel", GridData.FILL);
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				result = CANCEL;
				dialog.dispose();
			}
		});

		dialog.open();
		Display display = parent.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

		return (result);
	}

	
}
