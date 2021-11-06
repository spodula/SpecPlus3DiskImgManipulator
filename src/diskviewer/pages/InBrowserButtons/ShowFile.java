package diskviewer.pages.InBrowserButtons;
/**
 * This unit contains the code implementing the ShowFile() callback 
 * in the html browser window. 
 * expects a single parameter of the file number in the Directory entry list of the disk
 */

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.FilesPage;


public class ShowFile extends BrowserFunction {
	public FilesPage FilesPage;
	public PlusThreeDiskWrapper CurrentDisk;

	public ShowFile(Browser browser, String name) {
		super(browser, name);
	}

	@Override
	public Object function(Object[] arguments) {
		FilesPage.filedisplay = -1;
		FilesPage.filenum = Integer.parseInt((String) arguments[0]);
		try {
			this.getBrowser().setText(FilesPage.get(CurrentDisk));
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		}

		return null;
	}

}
