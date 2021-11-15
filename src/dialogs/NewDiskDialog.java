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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import diskviewer.DskBrowserMainForm;

public class NewDiskDialog extends DiskReaderDialog {
	public static class disktype {
		public String Description;
		public int Sectors;
		public int Heads;
		public int Track;
		public String Header;
		public boolean IsExtended;
		public int MinSector;
		public int SectorSzShift;
		public int ReservedTracks;
		public int BlockSzShift;
		public int rwGapLength;
		public int fmtGapLength;
		public byte filler;
		public byte BootSector[];

		public disktype(String desc, int s, int h, int t, String header, boolean Extended, int minsect, int sectorsz,int reservedtr,int blocksz, int rwgap, int fmtgap, byte Fillerbyte, String bootsector, byte eob) {
			Description =desc;
			Sectors = s;
			Heads = h;
			Track = t;
			Header = header;
			IsExtended = Extended;
			MinSector = minsect;
			SectorSzShift = sectorsz;
			ReservedTracks = reservedtr;
			BlockSzShift =  blocksz;
			rwGapLength = rwgap;
			fmtGapLength = fmtgap;
			filler = Fillerbyte;
			
			BootSector = new byte[256];
			for(int i = 0;i<BootSector.length;i++) {
				BootSector[i] = 0;
			}

			String bootbytes[] = bootsector.split(" ");
			
			for(int i=0;i<bootbytes.length;i++)  {
				BootSector[i] = (byte) Integer.parseInt(bootbytes[i],16);
			}
			
			BootSector[BootSector.length-1] = eob;
			
		}
	}
	
	private static disktype physicaldisks[] = {
			new disktype("CF/2 180k Disk (40 tracks 1 head 9 sectors) Spectrum +3/PCW type 3",          9,1,40,"MV - CPCEMU Disk-File\r\nDisk-Info\r\n", false,1,2,1,3,42,82,(byte)0xe5,"00 00 28 09 02 01 03 02 2A 52",(byte)0),
			new disktype("CF/2 180k Disk (40 tracks 1 head 9 sectors) EXTENDED Spectrum +3/PCW type 3", 9,1,40,"EXTENDED CPC DSK File\r\nDisk-Info\r\n", true,1,2,1,3,42,82,(byte)0xe5,"00 00 28 09 02 01 03 02 2A 52",(byte)0)
	};
	
	
	DskBrowserMainForm Mainform = null;
	
	// basic constructor
	public NewDiskDialog(Shell parent, DskBrowserMainForm dbmf) {
		super(parent);
		Mainform = dbmf;
	}

	disktype result = null;


	/**
	 * Create and display the dialog.
	 * 
	 * @return
	 */
	public disktype open() {
		Shell parent = getParent();
		Shell dialog = new Shell(parent, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setSize(500, 200);
		dialog.setText("New disk");
		dialog.setLayout(new GridLayout(4, false));

		Composite cp = new Composite(dialog, SWT.NONE);
		cp.setBackground(new Color(0x80, 0x80, 0x80));
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false);
		data.horizontalSpan = 4;
		cp.setLayoutData(data);
		cp.setLayout(new GridLayout(1, false));

		Label label = GetLabel(cp, "Create a blank disk", 1);
		FontData fontData = label.getFont().getFontData()[0];
		Font font = new Font(dialog.getDisplay(),
				new FontData(fontData.getName(), fontData.getHeight() + 4, SWT.BOLD | SWT.ITALIC));
		label.setFont(font);

		GetLabel(dialog, "Disk type", 1);
	    Combo DiskType = new Combo(dialog, SWT.DROP_DOWN);
	    for(int i=0;i<physicaldisks.length;i++) {
	    	DiskType.add(physicaldisks[i].Description);
	    }
	    DiskType.select(0);
	    data = new GridData(SWT.FILL, SWT.NONE, true, false);
		data.horizontalSpan = 3;
		data.grabExcessHorizontalSpace = true;
		DiskType.setLayoutData(data);

		GetLabel(dialog, "", 2);

		Button BtnOK = GetButton(dialog, "Create", GridData.FILL);
		BtnOK.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
				int i = DiskType.getSelectionIndex();
				result = physicaldisks[i];
				
				dialog.dispose();
			}
		});

		Button btnCancel = GetButton(dialog, "Cancel", GridData.FILL);
		btnCancel.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent event) {
				widgetDefaultSelected(event);
			}

			public void widgetDefaultSelected(SelectionEvent event) {
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
