package dialogs;
/**
 * Helper functions for dialogs.
 * 
 * 
 */

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DiskReaderDialog extends Dialog {
	public boolean VerboseMode = false;
	
	public String DefaultFolder="";
	
	public DiskReaderDialog(Shell parent) {
		super(parent);
	}
	
	/**
	 * Get a button.
	 * @param parent
	 * @param caption
	 * @return
	 */
	public Button GetButton(Composite parent, String caption, int flags) {
		Button result = new Button(parent, SWT.BORDER);
		GridData data = new GridData(flags);
		data.horizontalSpan = 1;
		result.setText(caption);
		result.setLayoutData(data);
		return(result);
	}
	
	/**
	 * Get a label 
	 * @param parent
	 * @param caption
	 * @param span
	 * @return
	 */
	public Label GetLabel(Composite parent, String caption, int span) {
		Label result = new Label(parent, SWT.NONE);
		result.setText(caption);
		GridData data = new GridData();
		data.horizontalSpan = span;
		result.setLayoutData(data);

		return (result);
	}
	
	/**
	 * 
	 * @param parent
	 * @param text
	 * @param span
	 * @return
	 */
	public Text GetText(Composite parent, String text, int span) {
		Text editbox = new Text(parent, SWT.BORDER);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = span;
		editbox.setText(text);
		editbox.setLayoutData(data);
		return(editbox);
	}

	/**
	 * Create a caption and edit text.
	 * 
	 * @param parent
	 * @param caption
	 * @param value
	 * @return
	 */
	public Text captionAndText(Composite parent, String caption, String value) {
		GetLabel(parent, caption, 2);
		Text editbox = GetText(parent,value,2);
		return (editbox);
	}
	
	/**
	 * Log a line of text to the console if in verbose mode.
	 * @param s
	 */
	public void log(String s) {
		if (VerboseMode) {
			System.out.println(s);
		}
	}
	
	public void logNoCR(String s) {
		if (VerboseMode) {
			System.out.print(s);
		}
	}

	/**
	 * Return a checkbox.
	 * 
	 * @param parent
	 * @param text
	 * @param span
	 * @return
	 */
	public Button GetCheckbox(Composite parent, String text, int span) {
		Button editbox = new Button(parent, SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = span;
		editbox.setText(text);
		editbox.setLayoutData(data);
		return(editbox);
	}

	
	/**
	 * Check to see if a given string is numeric. 
	 * @param strNum
	 * @return
	 */
	public static boolean isNumeric(String strNum) {
	    if (strNum == null) {
	        return false;
	    }
	    try {
	        Double.parseDouble(strNum);
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	    return true;
	}
	
	/**
	 * 
	 * @param filename
	 */
	public void SetDefaultFolderFromFile(String filename) {
		File f = new File(filename);
		DefaultFolder = f.getParent();
		if (!DefaultFolder.isBlank()) {
			DefaultFolder = DefaultFolder + System.getProperty("file.separator");
		}
		System.out.println(DefaultFolder);
	}
	
}
