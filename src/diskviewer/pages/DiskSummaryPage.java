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
		
		StringBuilder result = new StringBuilder();
		
		DiskInfo di;
		result.append("<h1>Summary of Raw Contents of AMS file</h1>\r\n");
		try {
			di = disk.GetDiskInfo();
			String tracksz = String.valueOf(di.tracksz);
			if (di.IsExtended) {
				tracksz = "(Extended format, can vary by sector)";
			}
			result.append(DEFAULT_TABLE_STYLESHEET);
			if (!disk.IsValidCPMFileStructure) {
				result.append("<div style=\"color:RED\">Disk does not contain a CPM/+3 filesystem.</div>");
			}

			
			result.append("<table>\r\n" +
			              "<tr><th><b>DiskID</b></th><td>" + di.DiskID + "</td></tr>\r\n" +
			              "<tr><th><b>Creator</b></th><td>" + di.Creator + "</td></tr>\r\n" +
			              "<tr><th><b>Tracks</b></th><td>" + String.valueOf(di.tracks) + "</td></tr>\r\n" +
			              "<tr><th><b>Sides</b></th><td>" + String.valueOf(di.sides) + "</td></tr>\r\n" +
			              "<tr><th><b>Track size</b></th><td>" + tracksz + "</td></tr>\r\n" +
			              "</table>\r\n");

			result.append("<h1>Basic Track information</h1>\r\n" +
			              "<table>\r\n" + 
			              "<tr>" +
			              "<th>Track</td>" +
			              "<th>Side</td>" +
			              "<th>Num sectors</th>"+
			              "<th>Sector size</th>" +
			              "<th>gap3Length</th>" +
			              "<th>Filler byte</th>" +
			              "<th>Data rate</th>" +
			              "<th>Recording mode</th>" +
			              "</tr>\r\n");
			
			for (TrackInfo t : disk.diskTracks) {
				result.append("<tr>" +
	              "<td>"+String.valueOf(t.tracknum)+"</td>" +
	              "<td>"+String.valueOf(t.side)+"</td>" +
	              "<td>"+String.valueOf(t.numsectors)+"</td>" +
	              "<td>"+String.valueOf(t.sectorsz)+"</td>" +
	              "<td>"+String.valueOf(t.gap3len)+" ($"+toHexTwoDigit(t.gap3len)+")</td>" +
	              "<td>"+String.valueOf(t.fillerByte)+" ($"+toHexTwoDigit(t.fillerByte)+")</td>" +
	              "<td>"+t.GetDataRate()+"</td>" +
	              "<td>"+t.GetRecordingMode()+"</td>" +
	              "</tr>\r\n");				
			}
			result.append("</table>\r\n");
			
			

			
		} catch (BadDiskFileException e) {
			result.append("Disk information invalid.");
		}
		return(result.toString());
	}
}
