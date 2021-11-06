package diskviewer.pages.InBrowserButtons;
/**
 * This unit contains the code implementing the DisplayFileAs() callback 
 * in the html browser window. 
 * Expects two paramters:
 *   file number in the directory array
 *   file display we are trying. (1=raw, 2=raw without header, 3=BASIC, 4=array, 5= screen$ )
 */

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.FilesPage;

public class DisplayFileAs extends BrowserFunction {
	public FilesPage FilesPage;
	public PlusThreeDiskWrapper CurrentDisk;

	public DisplayFileAs(Browser browser, String name) {
		super(browser, name);
	}

	@Override
	public Object function(Object[] arguments) {
		FilesPage.filenum = Integer.parseInt((String) arguments[0]);
		FilesPage.filedisplay = Integer.parseInt((String) arguments[1]);
		System.out.println(FilesPage.filenum + "-" + FilesPage.filedisplay);
		try {
			this.getBrowser().setText(FilesPage.get(CurrentDisk));
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		}

		return null;
	}

}