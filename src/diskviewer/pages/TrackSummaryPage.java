package diskviewer.pages;
/**
 * Displays information about given tracks and allows viewing of raw sector data
 */

import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.DiskInfo;
import diskviewer.libs.disk.TrackInfo;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.libs.disk.Sector;

public class TrackSummaryPage extends page {
	public int CurrentSector = 0;
	public int CurrentTrack = 0;
	public int CurrentSide = 0;

	public String get(PlusThreeDiskWrapper disk) {
		DiskInfo di;
		String result = DEFAULT_TABLE_STYLESHEET + HEX_TABLE_STYLESHEET + "\r\n";
		result = result + "<h1>Track information</h1>\r\n";
		try {
			di = disk.GetDiskInfo();
			result = result + "<table>\r\n";
			result = result + "<tr><td><b>DiskID</b></td><td>" + di.DiskID + "</td></tr>\r\n";
			result = result + "</table>\r\n<br>\r\n";
			result = result + "<table style=\"border-collapse: collapse\">\r\n";
			for (int side = 0; side < disk.GetDiskInfo().sides; side++) {
				result = result + "<tr><td style=\";border: 1px solid #ddd;\"><b>Side " + String.valueOf(side)
						+ "</b></td><td style=\";border: 1px solid #ddd;\">";
				for (int track = 0; track < disk.GetDiskInfo().tracks; track++) {
					result = result + "<button style=\"width:40\" type=\"button\" onclick=\"ShowSector('"
							+ String.valueOf(side) + "','" + String.valueOf(track) + "')\">" + String.valueOf(track)
							+ "</button>";
					if ((track % 20) == 19) {
						result = result + "<Br>\r\n";
					}
				}
				result = result + "</td></tr>";
			}
			result = result + "</table>\r\n";

			// Show the selected track.
			int ch = CurrentTrack * di.sides + CurrentSide;
			TrackInfo CurrentTrack = disk.Tracks[ch];

			result = result + "<br><h2>Track " + String.valueOf(CurrentTrack.tracknum) + " Side "
					+ String.valueOf(CurrentTrack.side) + "</h2>\r\n";

			// iterate all the sectors:
			// Note that sectors are not consecutive and in copy protected disks, can be missing. 
			// We will ignore any sectors that dont exist. 
			for (int sectnum = CurrentTrack.minsectorID; sectnum < CurrentTrack.maxsectorID + 1; sectnum++) {
				Sector Sector = CurrentTrack.GetSectorBySectorID(sectnum);
				if (Sector != null) {

					result = result + "<table class=\"hex\">\r\n";
					result = result + "<tr><th colspan=3>" + "<b>Track</b> " + String.valueOf(Sector.track)
							+ ";<b>Head</b> " + String.valueOf(Sector.side) + ";<b>Sector</b> "
							+ String.valueOf(Sector.sectorID) + ";<b>FDC SZ</b> " + String.valueOf(Sector.Sectorsz)
							+ ";<b>FDC sr1</b> " + String.valueOf(Sector.FDCsr1) + ";<b>FDC sr2</b> "
							+ String.valueOf(Sector.FDCsr2) + ";<b>Actual size</b> " + String.valueOf(Sector.ActualSize)
							+ "</td></tr>\r\n";
					// sector data
					String hexdata = "";
					String chardata = "";
					String starttext = "000-" + toHexThreeDigit(31);
					for (int i = 0; i < Sector.data.length; i++) {
						if ((i % 32) == 0 && (i > 0)) {
							result = result + "<tr><td>" + starttext + "</td><td>" + hexdata + "</td><td>" + chardata
									+ "</td></tr>\r\n";
							hexdata = "";
							chardata = "";
							starttext = toHexThreeDigit(i) + "-" + toHexThreeDigit(i + 31);
						}
						hexdata = hexdata + " " + toHexTwoDigit(Sector.data[i]);
						if ((Sector.data[i] > 0x1F) && (Sector.data[i] < 0x7f)) {
							chardata = chardata + (char) Sector.data[i];
						} else {
							chardata = chardata + "?";
						}

					}
					result = result + "<tr><td>" + starttext + "</td><td>" + hexdata + "</td><td>" + chardata
							+ "</td></tr>\r\n";

					result = result + "</table>\r\n<br>\r\n";
				}
			}

		} catch (BadDiskFileException e) {
			result = "Disk information invalid.";
		}
		return (result);
	}
}
