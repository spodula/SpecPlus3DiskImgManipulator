package diskviewer.pages;
/**
 * Provides the disk and track summary page. 
 */

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.DiskInfo;
import diskviewer.libs.disk.TrackInfo;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;

public class DiskSummaryPage extends page {
	public String get(PlusThreeDiskWrapper disk) {
		
		DiskInfo di;
		String result = "<h1>Summary of Raw Contents of AMS file</h1>\r\n";
		try {
			di = disk.GetDiskInfo();
			String tracksz = String.valueOf(di.tracksz);
			if (di.IsExtended) {
				tracksz = "(Extended format, can vary by sector)";
			}
			result = result + DEFAULT_TABLE_STYLESHEET;
			if (!disk.IsValidCPMFileStructure) {
				result = result + "<div style=\"color:RED\">Disk does not contain a CPM/+3 filesystem.</div>";
			}

			
			result = result + "<table>\r\n";
			result = result + "<tr><th><b>DiskID</b></th><td>" + di.DiskID + "</td></tr>\r\n";
			result = result + "<tr><th><b>Creator</b></th><td>" + di.Creator + "</td></tr>\r\n";
			result = result + "<tr><th><b>Tracks</b></th><td>" + String.valueOf(di.tracks) + "</td></tr>\r\n";
			result = result + "<tr><th><b>Sides</b></th><td>" + String.valueOf(di.sides) + "</td></tr>\r\n";
			result = result + "<tr><th><b>Track size</b></th><td>" + tracksz + "</td></tr>\r\n";
			result = result + "</table>\r\n";

			result = result + "<h1>Basic Track information</h1>\r\n";
			result = result + "<table>\r\n";
			result = result + "<tr>";
			result = result + "<th>Track</td>";
			result = result + "<th>Side</td>";
			result = result + "<th>Num sectors</th>";
			result = result + "<th>Sector size</th>";
			result = result + "<th>gap3Length</th>";
			result = result + "<th>Filler byte</th>";
			result = result + "<th>Data rate</th>";
			result = result + "<th>Recording mode</th>";
			result = result + "</tr>\r\n";				
			for (TrackInfo t : disk.Tracks) {
				result = result + "<tr>";
				result = result + "<td>"+String.valueOf(t.tracknum)+"</td>";
				result = result + "<td>"+String.valueOf(t.side)+"</td>";
				result = result + "<td>"+String.valueOf(t.numsectors)+"</td>";
				result = result + "<td>"+String.valueOf(t.sectorsz)+"</td>";
				result = result + "<td>"+String.valueOf(t.gap3len)+" ($"+toHexTwoDigit(t.gap3len)+")</td>";
				result = result + "<td>"+String.valueOf(t.fillerByte)+" ($"+toHexTwoDigit(t.fillerByte)+")</td>";
				result = result + "<td>"+t.GetDataRate()+"</td>";
				result = result + "<td>"+t.GetRecordingMode()+"</td>";
				result = result + "</tr>\r\n";				
			}
			result = result + "</table>\r\n";
			
			

			
		} catch (BadDiskFileException e) {
			result = "Disk information invalid.";
		}
		return(result);
	}
}
