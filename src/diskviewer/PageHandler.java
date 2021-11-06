package diskviewer;

/**
 * Run the numbered page
 */

import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.BootBlockPage;
import diskviewer.pages.DiskSummaryPage;
import diskviewer.pages.FileSystemPage;
import diskviewer.pages.FilesPage;
import diskviewer.pages.TrackSummaryPage;

public class PageHandler {
	//Verbose mode. 
	boolean VerboseMode = false;

	//pages
	DiskSummaryPage DiskSummary = new DiskSummaryPage();
	public TrackSummaryPage TrackSummary = new TrackSummaryPage();
	BootBlockPage BootBlock = new BootBlockPage();
	FileSystemPage FileSystem = new FileSystemPage();
	FilesPage Files = new FilesPage();

	
	public static int PAGE_SUMMARY = 1;
	public static int TRACK_SUMMARY = 2;
	public static int BOOT_BLOCK = 3;
	public static int FILE_SYSTEM = 4;
	public static int FILE_STRUCTURE = 5;

	/**
	 * return the given page by number
	 * 
	 * @param page
	 * @param disk
	 * @return
	 */
	String GetPage(int page, PlusThreeDiskWrapper disk) {
		try {
			String result = "";
			if (disk == null || !disk.IsValid) {
				result = "<h1>No disk loaded</h1>";
			} else {
				switch (page) {
				case 1:
					DiskSummary.VerboseMode = VerboseMode;
					result = DiskSummary.get(disk);
					break;
				case 2:
					TrackSummary.VerboseMode = VerboseMode;
					result = TrackSummary.get(disk);
					break;
				case 3:
					BootBlock.VerboseMode = VerboseMode;
					result = BootBlock.get(disk);
					break;
				case 4:
					FileSystem.VerboseMode = VerboseMode;
					result = FileSystem.get(disk);
					break;
				case 5:
					Files.VerboseMode = VerboseMode;
					result = Files.get(disk);
				}
			}
			return ("<html><body>" + result + "</body></html>");
		} catch (Exception E) {
			E.printStackTrace();
			return ("");
		}
	}

}
