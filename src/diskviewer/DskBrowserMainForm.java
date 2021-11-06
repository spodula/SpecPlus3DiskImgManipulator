package diskviewer;

/**
 * Create the main form.
 */

import java.io.File;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import dialogs.FormCloseDialog;
import dialogs.SearchDialog;
import diskviewer.libs.disk.ModifiedEvent;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.InBrowserButtons.DisplayFileAs;
import diskviewer.pages.InBrowserButtons.Fileop;
import diskviewer.pages.InBrowserButtons.ShowFile;
import diskviewer.pages.InBrowserButtons.ShowSector;

public class DskBrowserMainForm {
	//Verbose mode for output to the console.
	private boolean VerboseMode = false;
	
	// Last title used in the titlebar (Changes when a disk is loaded) this is used
	// so we can add (modified) to end as required.
	private String lasttitle = "Spectrum +3 disk viewer";

	// SWT display object
	private Display display = null;

	// SWT shell object
	private Shell shell = null;

	// Browser object used for output. I know this is lazy, but so am I :)
	private Browser browser = null;

	// The wrapper around the current disk being edited.
	private PlusThreeDiskWrapper CurrentDisk = new PlusThreeDiskWrapper();

	// Object controlling the pages used for display
	private PageHandler pages = new PageHandler();

	//Constructor allowing to set Verbose
	public DskBrowserMainForm(boolean VerboseMode) {
		super();
		this.VerboseMode = VerboseMode;
		CurrentDisk.VerboseMode = VerboseMode;
		pages.VerboseMode = VerboseMode;
	}
	
	/**
	 * Dialog loop, open and wait until closed.
	 */
	public void loop() {
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}

	/**
	 * Generate and display the main form.
	 */
	public void MakeForm() {
		display = new Display();
		shell = new Shell(display);

		shell.addListener(SWT.Close, new Listener() {
			public void handleEvent(Event event) {
				if (CurrentDisk.IsValid && CurrentDisk.getModified()) {
					FormCloseDialog fcd = new FormCloseDialog(shell);
					int result = fcd.open();
					if (result == FormCloseDialog.CLOSE) {
						event.doit = true;
					} else if (result == FormCloseDialog.CANCEL) {
						event.doit = false;
					} else if (result == FormCloseDialog.SAVEAS) {
						FileDialog fd = new FileDialog(shell, SWT.SAVE);
						fd.setText("Save DSK file");
						fd.setFileName(CurrentDisk.LastFileName);
						String selected = fd.open();
						if (selected != null && !selected.isBlank()) {
							try {
								CurrentDisk.save(selected);
								event.doit = true;
							} catch (IOException e) {
								e.printStackTrace();
								event.doit = false;
							}
						} else {
							event.doit = false;
						}
					} else if (result == FormCloseDialog.SAVE) {
						String selected = CurrentDisk.LastFileName;
						if (selected != null && !selected.isBlank()) {
							try {
								CurrentDisk.save(selected);
								event.doit = true;
							} catch (IOException e) {
								e.printStackTrace();
								event.doit = false;
							}
						} else
							event.doit = true;
					}
				}
			}
		});

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		shell.setLayout(gridLayout);
		shell.setText("Spectrum 3 disk viewer");

		ToolBar toolbar = new ToolBar(shell, SWT.NONE);
		toolbar.setBackground(new Color(192, 192, 192));
		ToolItem itemLoadFile = new ToolItem(toolbar, SWT.PUSH);
		itemLoadFile.setText("Load");
		itemLoadFile.setImage(getImage("loadicon.png"));
		ToolItem itemSaveFile = new ToolItem(toolbar, SWT.PUSH);
		itemSaveFile.setText("Save");
		itemSaveFile.setImage(getImage("saveicon.png"));
		ToolItem itemInformation = new ToolItem(toolbar, SWT.PUSH);
		itemInformation.setText("Information");
		itemInformation.setImage(getImage("diskinfo.png"));
		ToolItem itemTrackInfo = new ToolItem(toolbar, SWT.PUSH);
		itemTrackInfo.setText("Raw Tracks");
		itemTrackInfo.setImage(getImage("trackinfo.png"));
		ToolItem itemBootBlock = new ToolItem(toolbar, SWT.PUSH);
		itemBootBlock.setText("Boot block");
		itemBootBlock.setImage(getImage("bootblock.png"));
		ToolItem itemFileSystem = new ToolItem(toolbar, SWT.PUSH);
		itemFileSystem.setText("FileSystem");
		itemFileSystem.setImage(getImage("filesystem.png"));
		ToolItem itemSearch = new ToolItem(toolbar, SWT.PUSH);
		itemSearch.setText("Search");
		itemSearch.setImage(getImage("searchicon.png"));

		GridData data = new GridData();
		data.horizontalSpan = 3;
		toolbar.setLayoutData(data);

		// add in the functions for the HTML buttons in the browser window.
		try {
			browser = new Browser(shell, SWT.NONE);
			ShowSector s = new ShowSector(browser, "ShowSector");
			s.CurrentDisk = CurrentDisk;
			s.Trackpage = pages.TrackSummary;
			ShowFile sf = new ShowFile(browser, "ShowFile");
			sf.CurrentDisk = CurrentDisk;
			sf.FilesPage = pages.Files;
			DisplayFileAs df = new DisplayFileAs(browser, "DisplayFileAs");
			df.CurrentDisk = CurrentDisk;
			df.FilesPage = pages.Files;
			Fileop fo = new Fileop(browser, "Fileop");
			fo.CurrentDisk = CurrentDisk;
			fo.FilesPage = pages.Files;
		} catch (SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
			display.dispose();
			return;
		}

		CurrentDisk.OnModify = new ModifiedEvent() {
			@Override
			public void ModifiedChanged() {
				SetHeader("");
			}
		};

		data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		browser.setLayoutData(data);

		final Label status = new Label(shell, SWT.NONE);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		status.setLayoutData(data);

		/* event handling */
		itemLoadFile.addListener(SWT.Selection, event -> {
			FileDialog fd = new FileDialog(shell, SWT.OPEN);
			fd.setText("Open DSK file");
			//fd.setFilterPath("C:/");
			String[] filterExt = { "*.dsk", "*.img", "*.*" };
			fd.setFilterExtensions(filterExt);
			String selected = fd.open();
			if (VerboseMode)
				System.out.println(selected);
			if (selected != null && !selected.isBlank()) {
				try {
					CurrentDisk.load(selected);
					// default to the filesystem page is there is a filesystem
					if (CurrentDisk.IsValidCPMFileStructure) {
						browser.setText(pages.GetPage(4, CurrentDisk));
					} else {
						// otherwise, the raw disk structure.
						browser.setText(pages.GetPage(1, CurrentDisk));
					}
					SetHeader(new File(selected).getName());
				} catch (Exception E) {
					System.out.println("Error loading." + E.getMessage());
				}
			}
		});

		itemSaveFile.addListener(SWT.Selection, event -> {
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
			fd.setText("Save DSK file");
			String selected = fd.open();
			if (VerboseMode)
					System.out.println(selected);
			if (selected != null && !selected.isBlank()) {
				try {
					CurrentDisk.save(selected);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		itemSearch.addListener(SWT.Selection, event -> {
			SearchDialog sd = new SearchDialog(shell);
			sd.open();
		});
		
		
		

		itemInformation.addListener(SWT.Selection, event -> {
			browser.setText(pages.GetPage(PageHandler.PAGE_SUMMARY, CurrentDisk));
		});

		itemTrackInfo.addListener(SWT.Selection, event -> {
			browser.setText(pages.GetPage(PageHandler.TRACK_SUMMARY, CurrentDisk));
		});

		itemBootBlock.addListener(SWT.Selection, event -> {
			browser.setText(pages.GetPage(PageHandler.BOOT_BLOCK, CurrentDisk));
		});

		itemFileSystem.addListener(SWT.Selection, event -> {
			browser.setText(pages.GetPage(PageHandler.FILE_SYSTEM, CurrentDisk));
		});

	}

	/**
	 * Set the titlebar. If blank, will just refresh it, eg, with New modified
	 * status
	 * 
	 * @param header
	 */
	public void SetHeader(String header) {
		if (header.isBlank()) {
			header = lasttitle;
		} else {
			lasttitle = header;
		}
		if (CurrentDisk != null && CurrentDisk.getModified()) {
			header = header + " (Modified)";
		}
		shell.setText(header);
	}

	/**
	 * Fetch an image from the resources folder. If the program is being run from a
	 * JAR file, it looks in the root folder of the JAR. If its being run directly,
	 * it looks in the src/resources folder.
	 * 
	 * @param name
	 * @return
	 */
	private Image getImage(String name) {
		Image result = null;
		@SuppressWarnings("rawtypes")
		Class me = getClass();
		if (me.getResource(me.getSimpleName() + ".class").toString().startsWith("jar:")) {
			result = new Image(display, getClass().getResourceAsStream("/" + name));
		} else {
			result = new Image(display, new File("src/resources", name).getAbsolutePath());
		}
		return (result);
	}

}
