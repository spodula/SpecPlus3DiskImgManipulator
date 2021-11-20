package dialogs;

import java.util.ArrayList;

/**
 * Dialog for searching
 * 
 * 
 * 
 */

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
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import diskviewer.DskBrowserMainForm;
import diskviewer.libs.disk.Sector;
import diskviewer.libs.disk.TrackInfo;
import diskviewer.libs.disk.cpm.DirectoryEntry;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;

public class SearchDialog extends DiskReaderDialog {
	//
	Shell dialog = null;

	// current disk.
	private PlusThreeDiskWrapper CurrentDisk = null;

	// Result list box
	private List ResultList = null;

	//Main form so we can talk back to it. 
	private DskBrowserMainForm ownr = null;

	// Storage for search result
	private SearchResult SearchResults[] = new SearchResult[0];

	// if TRUE, the OK button was pressed, else FALSE
	private boolean IsOk = false;

	// basic constructor
	public SearchDialog(Shell parent, PlusThreeDiskWrapper disk, DskBrowserMainForm mainform) {
		super(parent);
		CurrentDisk = disk;
		ownr = mainform;
	}

	/**
	 * Create and display the dialog.
	 * 
	 * @return
	 */
	public boolean open() {
		IsOk = false;
		Shell parent = getParent();
		dialog = new Shell(parent, SWT.DIALOG_TRIM);
		dialog.setSize(500, 400);
		dialog.setText("Search");
		dialog.setLayout(new GridLayout(5, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		cp.setBackground(new Color(dialog.getDisplay(),0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 5;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Search", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		GetLabel(dialog, "Txt/AA BB CC", 2);
		Text searchval = GetText(dialog, "", 3);

		Button BtnSearch = GetButton(dialog, "Search ascii", GridData.FILL);
		GetLabel(dialog, "", 1);

		Button BtnSearchbin = GetButton(dialog, "Search Hex", GridData.FILL);

		Button btnCancel = GetButton(dialog, "Cancel", GridData.FILL);

		Button cbCaseSensitive = GetCheckbox(dialog, "Case insensitive", 1);

		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				IsOk = false;
				dialog.dispose();
			}
		});

		BtnSearch.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				DoSearch(searchval.getText(), cbCaseSensitive.getSelection());

			}
		});

		BtnSearchbin.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				DoSearchBin(searchval.getText());
			}
		});

		ResultList = new List(dialog, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.horizontalSpan = 5;
		data.verticalSpan = 3;

		ResultList.setLayoutData(data);

		ResultList.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				int i = ResultList.getSelectionIndex();
				SearchResult sr = SearchResults[i];
				if (sr.FileDets != null) {
					ownr.GotoFileByDirent(sr.FileNumber);
				} else {
					ownr.GotoTrack(sr.track, sr.head, sr.sector);
				}
			}
		});

		dialog.open();
		Display display = parent.getDisplay();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display = null;
		return (IsOk);
	}

	/**
	 * 	
	 */
	public void ForceDispose() {
		if (dialog != null) {
			if (!dialog.isDisposed()) {
				dialog.dispose();
			}
		}
	}

	/**
	 * Handler for the ASCII search function.
	 * 
	 * @param text
	 * @param ignorecase
	 */
	private void DoSearch(String text, boolean ignorecase) {
		int numresult = 0;
		ResultList.removeAll();
		ArrayList<SearchResult> searchr = new ArrayList<SearchResult>();

		for (TrackInfo t : CurrentDisk.Tracks) {
			for (Sector s : t.Sectors) {
				if (numresult < 100) {
					int i = CompareBytesAndString(s.data, text, ignorecase);
					if (i > -1) {
						SearchResult result = CalcResultText(t, s, i);

						searchr.add(result);
						ResultList.add(result.TextResult);
						numresult++;
						if (numresult == 100) {
							ResultList.add(".. results truncated.");
						}
					}
				}
			}
		}
		SearchResults = new SearchResult[searchr.size()];
		for (int i = 0; i < searchr.size(); i++) {
			SearchResults[i] = searchr.get(i);
		}

		if (numresult == 0) {
			ResultList.add("Not found.");
		}

	}

	/**
	 * Take the given Track, sector and location in the sector and return a result
	 * text.
	 * 
	 * @param FoundTrack
	 * @param FoundSector
	 * @param LocationInSector
	 * 
	 * @return
	 */
	private SearchResult CalcResultText(TrackInfo FoundTrack, Sector FoundSector, int LocationInSector) {
		SearchResult sr = new SearchResult(FoundSector.sectorID, FoundTrack.side, FoundTrack.tracknum,
				LocationInSector);
		String result = "C:" + FoundTrack.tracknum + " h:" + FoundTrack.side + " s:" + FoundSector.sectorID + ": "
				+ "byte: " + LocationInSector;
		// try to locate the file which owns this sector.
		if (CurrentDisk.IsValidCPMFileStructure) { // but only if we have a valid filesystem.
			if (FoundTrack.tracknum < CurrentDisk.reservedTracks) {
				if (FoundSector.sectorID == FoundTrack.minsectorID) {
					result = result + " Boot block.";
				}
			} else {
				// calculate the blocknum.
				int Track = ((FoundTrack.tracknum - CurrentDisk.reservedTracks) * CurrentDisk.numsides)
						+ FoundTrack.side;
				int chsNum = (Track * CurrentDisk.numsectors) + (FoundSector.sectorID - FoundTrack.minsectorID);
				int blocknum = (chsNum * CurrentDisk.sectorSize) / CurrentDisk.blockSize;
				result = result + " Block:" + blocknum;
				if (blocknum <= CurrentDisk.reservedblocks) {
					result = result + " Directory block.";
				} else {

					// Ok. which file has this block?
					int filenum = 0;
					for (DirectoryEntry fileDets : CurrentDisk.DirectoryEntries) {
						int blocks[] = fileDets.getBlocks();
						for (int blknum = 0; blknum < blocks.length; blknum++) {
							if (blocks[blknum] == blocknum) {
								sr.FileNumber = filenum;
								sr.FileDets = fileDets;
								// found it, add the filename
								result = result + " File: " + fileDets.filename();
								// locate the location in the file.
								int fileloc = blknum * CurrentDisk.blockSize + LocationInSector;
								// If its a +3 DOS file, skip the +3 Dos header.
								if (fileDets.GetPlus3DosHeader().IsPlusThreeDosFile) {
									fileloc = fileloc - 0x80;
								}
								if (fileloc > -1) {
									result = result + " Location: " + fileloc;
								} else {
									result = result + " File header byte: " + (fileloc + 0x80);
								}
								sr.locInFile = fileloc;
							}
						}
						filenum++;
					}
				}
			}
		}
		sr.TextResult = result;
		return (sr);
	}

	/**
	 * Try to find the given string in the given data array.
	 * 
	 */
	private int CompareBytesAndString(byte data[], String search, boolean ignorecase) {
		// Convert the byte array into a String.
		String dataArray = "";
		for (int i = 0; i < data.length; i++) {
			byte b = data[i];
			char c = (char) (b & 0xff);
			dataArray = dataArray + c;
		}
		// If not case sentitive, uppercase everything
		if (ignorecase) {
			dataArray = dataArray.toUpperCase();
			search = search.toUpperCase();
		}
		// and use a standard string function.
		return (dataArray.indexOf(search));
	}

	/**
	 * Search for a series of hex numbers.
	 * 
	 * @param text
	 */
	private void DoSearchBin(String text) {
		boolean error = false;
		// Parse the string into a byte array.
		String sValues[] = text.trim().split("\\s+");
		int bValues[] = new int[sValues.length];
		for (int i = 0; i < sValues.length; i++) {
			try {
				bValues[i] = Integer.parseInt(sValues[i], 16);
				if (bValues[i] < 0 || bValues[i] > 0xff) {
					System.out.println("Bad value: " + sValues[i] + " (Out of range)");
					MessageBox box = new MessageBox(this.getParent(), SWT.OK);
					box.setText("Bad hex number");
					box.setMessage("The value \"" + sValues[i] + "\" is out of range (Needs to be 00-FF)");
					box.open();
					error = true;
					break;
				}
			} catch (NumberFormatException E) {
				System.out.println("Bad value: " + sValues[i] + " (Invalid hex)");

				MessageBox box = new MessageBox(this.getParent(), SWT.OK);
				box.setText("Bad hex number");
				box.setMessage("The value \"" + sValues[i] + "\" is not a valid hex number.");
				box.open();
				error = true;
				break;
			}
		}
		// actually perform the search.
		if (!error) {
			// convert the byte array into a string
			String term = "";
			for (int i = 0; i < bValues.length; i++) {
				char s = (char) bValues[i];
				term = term + s;
			}
			DoSearch(term, false);
		}
		// now do the search.

		for (int b : bValues) {
			System.out.println(b);
		}

	}

}
