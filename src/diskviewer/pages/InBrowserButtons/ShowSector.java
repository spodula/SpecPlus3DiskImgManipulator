package diskviewer.pages.InBrowserButtons;
/**
 * This unit contains the code implementing the ShowSector() callback 
 * in the HTML browser window. 
 * Expects two parameters:
 *  side
 *  track
 * 
 */

import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;

import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.TrackSummaryPage;

public  class ShowSector extends BrowserFunction {
	public TrackSummaryPage Trackpage;
	public PlusThreeDiskWrapper CurrentDisk;

	public ShowSector(Browser browser, String name) {
		super(browser, name);
	}

	@Override
	public Object function(Object[] arguments) {
		Trackpage.CurrentSide = Integer.parseInt((String) arguments[0]);
		Trackpage.CurrentTrack = Integer.parseInt((String) arguments[1]);
		this.getBrowser().setText(Trackpage.get(CurrentDisk));

		return null;
	}
}