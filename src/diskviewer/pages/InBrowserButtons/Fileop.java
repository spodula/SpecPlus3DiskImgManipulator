package diskviewer.pages.InBrowserButtons;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;

import dialogs.RenameDialog;
import dialogs.basicDialog;
import dialogs.codeDialog;
import dialogs.headerlessFileDialog;
import dialogs.screenDialog;
import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.FilesPage;

public class Fileop extends BrowserFunction {
	public FilesPage FilesPage;
	public PlusThreeDiskWrapper CurrentDisk;

	public Fileop(Browser browser, String name) {
		super(browser, name);
	}

	@Override
	public Object function(Object[] arguments) {
		int filenum = Integer.parseInt((String) arguments[0]);
		int optype = Integer.parseInt((String) arguments[1]);
		FilesPage.filenum = filenum;
		String defaultFileName = "";
		if (filenum > 0) {
			defaultFileName = FilesPage.LastDisk.DirectoryEntries[filenum - 1].filename();
		}
		boolean VerboseMode = CurrentDisk.VerboseMode;		

		try {
			if (optype == 1 || optype == 2) { // extract file without +3dos header (1) with +3dos header (2)
				FileDialog fd = new FileDialog(this.getBrowser().getShell(), SWT.SAVE);
				fd.setText("Save File");
				fd.setFileName(defaultFileName);
				String selected = fd.open();
				if (selected != null) {
					FilesPage.saveFile(selected, (optype == 2));
				}
			} else if (optype == 3) { // delete file
				FilesPage.SetDeleteCurrentFile(true);
			} else if (optype == 4) { // rename file
				RenameDialog rd = new RenameDialog(this.getBrowser().getShell());
				rd.OldFilename = defaultFileName;
				if (rd.open()) {
					FilesPage.renameFile(rd.NewFilename);

				}
			} else if (optype == 5) { // save as PNG images
				FileDialog fd = new FileDialog(this.getBrowser().getShell(), SWT.SAVE);
				fd.setText("Save File as PNG");
				fd.setFilterExtensions(new String[] { "*.png", "*.jpg", "*.gif" });
				fd.setFileName(defaultFileName);
				String selected = fd.open();
				if (selected != null) {
					String typ = "png";
					if (selected.toLowerCase().endsWith(".gif")) {
						typ = "gif";
					} else if (selected.toLowerCase().endsWith(".jpg")) {
						typ = "jpg";
					}
					FilesPage.saveFileAsScreen(selected, typ);
				}
			} else if (optype == 6) { // save as ASCII Basic
				FileDialog fd = new FileDialog(this.getBrowser().getShell(), SWT.SAVE);
				fd.setText("Save File as Ascii basic");
				fd.setFilterExtensions(new String[] { "*.bas", "*.*" });
				fd.setFileName(defaultFileName);
				String selected = fd.open();
				FilesPage.saveFileAsBasic(selected);
			} else if (optype == 7) { // save array as text file
				FileDialog fd = new FileDialog(this.getBrowser().getShell(), SWT.SAVE);
				fd.setText("Save Array as Ascii basic");
				fd.setFilterExtensions(new String[] { "*.txt", "*.*" });
				fd.setFileName(defaultFileName);
				String selected = fd.open();
				FilesPage.saveFileAsTextArray(selected);
			} else if (optype == 8) { // Undelete file
				FilesPage.SetDeleteCurrentFile(false);
			} else if (optype == 10) { // Add headerless file
				headerlessFileDialog hfd = new headerlessFileDialog(this.getBrowser().getShell());
				hfd.VerboseMode = VerboseMode;
				if (hfd.open()) {
					if (CheckForExistingFile(hfd.NameOnDisk))
							CurrentDisk.AddHeaderlessFile(hfd.filename, hfd.NameOnDisk);
					FilesPage.filenum = CurrentDisk.DirectoryEntries.length; 
				}
			} else if (optype == 11) { // Add CODE file
				codeDialog cd = new codeDialog(this.getBrowser().getShell());
				cd.VerboseMode = VerboseMode;
				if (cd.open()) {
					if (CheckForExistingFile(cd.NameOnDisk) )
					CurrentDisk.AddRawCodeFile(cd.NameOnDisk, cd.address, cd.CodeAsBytes);
					FilesPage.filenum = CurrentDisk.DirectoryEntries.length; 
				}
			} else if (optype == 12) { // Add BASIC file
				basicDialog bd = new basicDialog(this.getBrowser().getShell());
				bd.VerboseMode = VerboseMode;
				if (bd.open()) {
					if (CheckForExistingFile(bd.NameOnDisk) )
					CurrentDisk.AddBasicFile(bd.NameOnDisk, bd.BasicAsBytes, bd.StartLine, bd.BasicAsBytes.length);
					FilesPage.filenum = CurrentDisk.DirectoryEntries.length; 
				}
			} else if (optype == 13) { // Add PNG as SCREEN$
				screenDialog sd = new screenDialog(this.getBrowser().getShell());
				sd.VerboseMode = VerboseMode;
				if (sd.open()) {
					if (CheckForExistingFile(sd.NameOnDisk) )
					CurrentDisk.AddRawCodeFile(sd.NameOnDisk, 0x4000, sd.Screen);
					FilesPage.filenum = CurrentDisk.DirectoryEntries.length; 
				}
			}

			this.getBrowser().setText(FilesPage.get(CurrentDisk));
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 
	 * @param filename
	 * @return TRUE if save should proceed
	 */
	public boolean CheckForExistingFile(String filename) {
		if (CurrentDisk.GetDirectoryEntry(filename) != null) {
			MessageBox dialog = new MessageBox(this.getBrowser().getShell(),
					SWT.ICON_WARNING | SWT.ABORT | SWT.RETRY | SWT.IGNORE);
			dialog.setText("File exists");
			dialog.setMessage("File " + filename + " exists on the disk. \r\n"
					+ "Abort, Retry (Renaming old file), Ignore (Overwrite file)");
			int result = dialog.open();
			if (result == SWT.ABORT) { 
				return(false);
			} else if (result == SWT.IGNORE) {
				CurrentDisk.GetDirectoryEntry(filename).SetDeleted(true);
				return(true);
			}  else if (result == SWT.RETRY) {
				String fname = filename;
				String extention="";
				//remove the extension.
				int i=fname.indexOf('.');
				if (i>-1) {
					fname = fname.substring(0, i);
				} 
				//default extension
				extention = "BAK";
				
				//loop through until we get a file that doesnt exist. 
				int bknum=1;
				while (CurrentDisk.GetDirectoryEntry(fname+"."+extention) != null) {
					if (bknum<10) {
						extention = "BK"+String.valueOf(bknum++);
					} else {
						extention = "B"+String.valueOf(bknum++);
					}
				}
				
				CurrentDisk.GetDirectoryEntry(filename).RenameTo(fname+"."+extention);
				return(true);
			}
		} 
		return (true);
	}

}
