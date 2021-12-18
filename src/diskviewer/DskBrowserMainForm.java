package diskviewer;

import java.io.BufferedReader;

/**
 * Create the main form.
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.filefilter.WildcardFileFilter;
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

import dialogs.CharacterArrayDialog;
import dialogs.FormCloseDialog;
import dialogs.NewDiskDialog;
import dialogs.NumericArrayDialog;
import dialogs.SearchDialog;
import dialogs.basicDialog;
import dialogs.screenDialog;
import diskviewer.libs.general;
import diskviewer.libs.disk.BadDiskFileException;
import diskviewer.libs.disk.ModifiedEvent;
import diskviewer.libs.disk.cpm.CPM;
import diskviewer.libs.disk.cpm.DirectoryEntry;
import diskviewer.libs.disk.cpm.Plus3DosFileHeader;
import diskviewer.libs.disk.cpm.PlusThreeDiskWrapper;
import diskviewer.pages.InBrowserButtons.DisplayFileAs;
import diskviewer.pages.InBrowserButtons.Fileop;
import diskviewer.pages.InBrowserButtons.ShowFile;
import diskviewer.pages.InBrowserButtons.ShowSector;

public class DskBrowserMainForm {
	// when scripting stop on error flag.
	private boolean StopOnError = false;

	// Verbose mode for output to the console.
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

	// Keep the search dialog, as we may need to close it. it can also remain open.
	private SearchDialog SearchDialog = null;

	// The wrapper around the current disk being edited.
	private PlusThreeDiskWrapper CurrentDisk = new PlusThreeDiskWrapper();

	// Object controlling the pages used for display
	private PageHandler pages = new PageHandler();

	// Constructor allowing to set Verbose
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
		toolbar.setBackground(new Color(shell.getDisplay(), 192, 192, 192));
		ToolItem itemNewDisk = new ToolItem(toolbar, SWT.PUSH);
		itemNewDisk.setText("New");
		itemNewDisk.setImage(getImage("newicon.png"));

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
		itemNewDisk.addListener(SWT.Selection, event -> {
			NewDiskDialog drd = new NewDiskDialog(shell, this);
			NewDiskDialog.disktype result = drd.open();
			if (result != null) {
				CurrentDisk.CreateDisk(result);
				try {
					if (SearchDialog != null) {
						SearchDialog.ForceDispose();
						SearchDialog = null;
					}
					// default to the filesystem page is there is a filesystem
					if (CurrentDisk.IsValidCPMFileStructure) {
						browser.setText(pages.GetPage(4, CurrentDisk));
					} else {
						// otherwise, the raw disk structure.
						browser.setText(pages.GetPage(1, CurrentDisk));
					}
					SetHeader("<New disk>");
				} catch (Exception E) {
					System.out.println("Error loading." + E.getMessage());
				}
			}
		});

		itemLoadFile.addListener(SWT.Selection, event -> {
			FileDialog fd = new FileDialog(shell, SWT.OPEN);
			fd.setText("Open DSK file");
			// fd.setFilterPath("C:/");
			String[] filterExt = { "*.dsk", "*.img", "*.*" };
			fd.setFilterExtensions(filterExt);
			String selected = fd.open();
			if (VerboseMode)
				System.out.println(selected);
			if (selected != null && !selected.isBlank()) {
				try {
					if (SearchDialog != null) {
						SearchDialog.ForceDispose();
						SearchDialog = null;
					}
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
			if (CurrentDisk.IsValid) {
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
			}
		});

		itemSearch.addListener(SWT.Selection, event -> {
			if (CurrentDisk.IsValid) {
				if (SearchDialog != null) {
					SearchDialog.ForceDispose();
				}
				SearchDialog = new SearchDialog(shell, CurrentDisk, this);
				SearchDialog.open();
			}
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

	/**
	 * 
	 * @param fileDets
	 */
	public void GotoFileByDirent(int filenumber) {
		pages.Files.filenum = filenumber + 1;

		pages.Files.filedisplay = -1;
		try {
			browser.setText(pages.Files.get(CurrentDisk));
		} catch (BadDiskFileException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param track
	 * @param head
	 */
	public void GotoTrack(int track, int head, int sector) {
		pages.TrackSummary.CurrentSide = head;
		pages.TrackSummary.CurrentTrack = track;
		pages.TrackSummary.CurrentSector = sector;

		browser.setText(pages.TrackSummary.get(CurrentDisk));

	}

	/**
	 * Run the given filename as a script.
	 * 
	 * @param scriptname
	 */
	public void script(String scriptname) {
		String line;
		BufferedReader in;

		File f = new File(scriptname);

		if (!f.exists()) {
			System.out.println("Error: " + scriptname + " does not exist.");
		} else if (!f.canRead()) {
			System.out.println("Error: " + scriptname + " exists but cannot be read");
		} else {
			try {
				in = new BufferedReader(new FileReader(f));
				line = "";
				boolean finish = false;
				while (line != null && !finish) {
					if (!line.isBlank() && (line.charAt(0) != '#')) {
						System.out.println("------------------------------------");
						System.out.println("CMD: " + line);
						boolean result = processCommand(line);
						if (!result && StopOnError) {
							System.out.println("Script terminated due to error.");
							finish = true;
						}
					}

					line = in.readLine();
				}
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}

		}
	}

	/**
	 * Decode and process one script line
	 * 
	 * @param line
	 */
	private boolean processCommand(String line) {
		boolean result = true;
		line = line.trim();
		String cmd = "";
		String params = "";
		int i = line.indexOf(' ');
		if (i != -1) {
			cmd = line.substring(0, i).toLowerCase();
			params = line.substring(i).trim();
		} else {
			cmd = line.toLowerCase();
			params = "";
		}

		if (cmd.equals("newdisk")) {
			if (params.isBlank() || !general.isNumeric(params)
					|| (Integer.valueOf(params) >= NewDiskDialog.physicaldisks.length)) {
				System.out.println(" Expecting newdisk [x]");
				System.out.println(" Where X is the disk type number.");
				int dnum = 0;
				for (NewDiskDialog.disktype d : NewDiskDialog.physicaldisks) {
					System.out.println("   " + dnum + ": " + d.Description);
					dnum++;
				}
				result = false;
			} else {
				CurrentDisk.CreateDisk(NewDiskDialog.physicaldisks[Integer.valueOf(params)]);
			}
		} else if (cmd.equals("load")) {
			if (params.isBlank()) {
				System.out.println(" Expecting load [filename]");
				System.out.println(" Where [filename] is the name of the disk");
				result = false;
			} else {
				try {
					CurrentDisk.load(params);
					if (!CurrentDisk.IsValid) {
						System.out.println(" Disk load error loading " + params);
						result = false;
					}
				} catch (Exception e) {
					System.out.println(" Disk load error loading " + params);
					System.out.println(" " + e.getMessage());
					result = false;
				}
			}
		} else if (cmd.equals("save")) {
			if (params.isBlank()) {
				System.out.println(" Expecting save [filename]");
				System.out.println(" Where [filename] is the name of the disk. ");
				result = false;
			} else {
				try {
					CurrentDisk.save(params);
				} catch (Exception e) {
					System.out.println(" Disk load error loading " + params);
					System.out.println(" " + e.getMessage());
					result = false;
				}
			}

		} else if (cmd.equals("addscr")) {
			if (params.isBlank()) {
				System.out.println(" Expecting addscr [bw (int)|col] [filename]");
				System.out.println(" Where bw xxx is black/white followed by the intensity cutoff between 1 and 254.");
				System.out.println("  eg. addscr bw 128 /home/bob/Pictures/*.gif");
				System.out.println(" and col means try to render as colour");
				System.out.println(" Filename can be a wildcard");
				result = false;
			} else {
				// parse the three parameters
				String typ = "";
				int bwInt = 0;

				i = params.indexOf(' ');
				if (i != -1) {
					typ = params.substring(0, i).toLowerCase();
					params = params.substring(i).trim();
				} else {
					typ = params.toLowerCase();
					params = "";
				}

				if (typ.equals("bw")) {
					i = params.indexOf(' ');
					if (i != -1) {
						try {
							bwInt = Integer.parseInt(params.substring(0, i));
						} catch (NumberFormatException E) {
							System.out.println("The parameter for addscr bw xxx is invalid.");
							result = false;
						}
						params = params.substring(i).trim();
					} else {
						try {
							bwInt = Integer.parseInt(params);
						} catch (NumberFormatException E) {
							System.out.println("The parameter for addscr bw xxx is invalid.");
							result = false;
						}
						params = "";
					}
					if (bwInt < 1 || bwInt > 254) {
						System.out.println("The parameter for addscr bw xxx needs to be between 1 and 254.");
						result = false;
					}
				} else if (!typ.equals("col")) {
					System.out.println("The parameter for addscr [typ] should be either col or bw.  ");
					result = false;
				}

				// if the parameters are correct, iterate the files.
				if (result) {
					File fn = new File(params);
					File folder = fn.getParentFile();
					String fname = fn.getName();

					FileFilter fileFilter = new WildcardFileFilter(fname);
					File[] files = folder.listFiles(fileFilter);

					screenDialog sd = new screenDialog(shell);
					sd.init();
					if (files != null && files.length > 0) {
						for (File f : files) {
							System.out.println(" Adding: " + f.getName());
							sd.LoadFile(f.getAbsolutePath());
							if (typ.equals("bw")) {
								sd.isBW = true;
								sd.ScaleImage(sd.dialog.getDisplay(), bwInt);
							}

							CurrentDisk.AddRawCodeFile(sd.NameOnDisk, 0x4000, sd.Screen);
						}
					} else {
						System.out.println("No files found.");
						result = false;
					}
				}
			}
		} else if (cmd.equals("addbasic")) {
			if (params.isBlank()) {
				System.out.println(" Expecting addbasic [raw|text] [start line] filename");
				System.out.println(" Where [raw|text] defines if basic is pre-encoded or ascii text");
				System.out.println(" and start line is the line number to start on (or 32768 if no auto-start");
				System.out.println("  eg. addscr raw 32768 /home/bob/basic.raw to add pre encoded basic");
				System.out.println("      addscr text 10 /home/bob/basic.raw to add ascii text as basic");

				System.out.println(" Filename can be a wildcard");
				result = false;
			} else {
				// parse the parameters
				String typ = "";
				int lineno = 0;

				String param[] = params.split(" ");

				if (param.length != 3) {
					System.out.println(" Expecting addbasic [raw|text] [start line] filename");
					result = false;
				} else {
					typ = param[0].toLowerCase();
					if (!typ.equals("raw") && !typ.equals("text")) {
						System.out.println(" Expecting first parameter to be either \"raw\" or \"text\" ");
						result = false;
					} else {
						try {
							lineno = Integer.parseInt(param[1]);
						} catch (NumberFormatException E) {
							System.out.println(
									" Lineno parameter (parameter 2) should be a number between 0 and 9999 or 32768");
							result = false;
						}
						if (!result) {
							if ((lineno < 0 || lineno > 9999) && (lineno != 32768)) {
								System.out.println(
										" Lineno parameter (parameter 2) should be a number between 0 and 9999 or 32768");
								result = false;
							}
						}
					}
				}

				if (result) {
					File fn = new File(param[2]);
					File folder = fn.getParentFile();
					String fname = fn.getName();

					FileFilter fileFilter = new WildcardFileFilter(fname);
					File[] files = folder.listFiles(fileFilter);

					if (files != null && files.length > 0) {
						if (typ.equals("text")) {
							basicDialog bd = new basicDialog(shell);
							bd.init();

							for (File f : files) {
								String filename = CPM.FixFullName(f.getName());
								System.out.println("Adding "+f.getAbsolutePath()+" as "+filename);
								// load and encode the file
								bd.LoadBasicFromFile(f.getAbsolutePath());
								// save to the disk.
								CurrentDisk.AddBasicFile(filename, bd.BasicAsBytes, lineno,
										bd.BasicAsBytes.length);
							}
						} else {
							// Load file to memory.
							for (File f : files) {
								String filename = CPM.FixFullName(f.getName());
								System.out.println("Adding "+f.getAbsolutePath()+" as "+filename);
								try {
									byte[] BasicAsBytes = new byte[(int) f.length()];
									InputStream in = new FileInputStream(f);
									try {
										for (int iloc = 0; iloc < BasicAsBytes.length; iloc++) {
											int chr = in.read();
											BasicAsBytes[iloc] = (byte) (chr & 0xff);
										}
									} finally {
										in.close();
									}
									// save to the disk.
									CurrentDisk.AddBasicFile(filename, BasicAsBytes, lineno,
											BasicAsBytes.length);

								} catch (IOException E) {
									System.out.println(" Error reading the file: " + E.getMessage());
									result = false;
								}
							}
						}
					} else {
						System.out.println("No files found.");
						result = false;
					}

				}
			}
		} else if (cmd.equals("addcode")) {
			if (params.isBlank()) {
				System.out.println(" Expecting addcode (startaddr) filename");
				System.out.println(" Where startaddr is the start address");
				System.out.println(" Filename can be a wildcard");
				result = false;
			} else {
				// parse the parameters
				int startaddr = 0;

				String param[] = params.split(" ");

				if (param.length != 2) {
					System.out.println(" Expecting addcode (startaddr) filename");
					result = false;
				} else {
					try {
						startaddr = Integer.parseInt(param[0]);
					} catch (NumberFormatException E) {
						System.out
								.println(" startaddr parameter (parameter 1) should be a number between 0 and 65535 ");
						result = false;
					}
					if (!result) {
						if ((startaddr < 0 || startaddr > 65535)) {
							System.out.println(
									" startaddr parameter (parameter 1) should be a number between 0 and 65535");
							result = false;
						}
					}
				}

				if (result) {
					File fn = new File(param[1]);
					File folder = fn.getParentFile();
					String fname = fn.getName();

					FileFilter fileFilter = new WildcardFileFilter(fname);
					File[] files = folder.listFiles(fileFilter);

					if (files != null && files.length > 0) {
						// Load file to memory.
						for (File f : files) {
							try {
								String filename = CPM.FixFullName(f.getName());
								System.out.println("Adding "+f.getAbsolutePath()+" as "+filename);

								byte[] codebytes = new byte[(int) f.length()];
								InputStream in = new FileInputStream(f);
								try {
									for (int iloc = 0; iloc < codebytes.length; iloc++) {
										int chr = in.read();
										codebytes[iloc] = (byte) (chr & 0xff);
									}
								} finally {
									in.close();
								}
								// save to the disk.
								CurrentDisk.AddRawCodeFile(filename, startaddr, codebytes);
							} catch (IOException E) {
								System.out.println(" Error reading the file: " + E.getMessage());
								result = false;
							}
						}
					} else {
						System.out.println("No files found.");
						result = false;
					}
				}
			}
		} else if (cmd.equals("rename")) {
			if (params.isBlank()) {
				System.out.println(" Expecting rename (from) (to)");
				System.out.println(" Filenames can NOT be a wildcard");
				result = false;
			} else {
				String param[] = params.split(" ");
				if (params.length()!=2) {
					System.out.println(" Expecting rename (from) (to)");
					System.out.println(" Filenames can NOT be a wildcard");
					result = false;					
				} else {
					DirectoryEntry file =  CurrentDisk.GetDirectoryEntry(CPM.FixFullName(param[0]));
					if (file == null) {
						System.out.println(" File \""+param[0]+"\" does not exist");
					} else {
						file.RenameTo(CPM.FixFullName(param[1]));
						System.out.println(" File renamed.");
					}
				}
			}
		} else if (cmd.equals("delete")) {
			if (params.isBlank()) {
				System.out.println(" Expecting delete (file)");
				System.out.println(" Filenames can not be a wildcard");
				result = false;
			} else {
				String param[] = params.split(" ");
				if (params.length()!=1) {
					System.out.println(" Expecting delete (file)");
					System.out.println(" Filenames can not be a wildcard");
					result = false;					
				} else {
					DirectoryEntry file =  CurrentDisk.GetDirectoryEntry(CPM.FixFullName(param[0]));
					if (file == null) {
						System.out.println(" File \""+param[0]+"\" does not exist");
					} else {
						file.SetDeleted(true);
						System.out.println(" File delete.");
					}
				}
			}
		} else if (cmd.equals("cat")) {
			System.out.println("Directory of disk "+CurrentDisk.LastFileName+"\r\n");
			
			System.out.println("Filename      Sz on Disk  typ +3Size type   flags");
			System.out.println("-------------------------------------------------");
			for(DirectoryEntry d:CurrentDisk.DirectoryEntries) {
				if (!d.IsDeleted) {
					System.out.print(padto(d.filename(),14));
					System.out.print(padto(String.valueOf(d.GetFileSize()),8));
					
					char attribs[] = new char[4];
					for(int x=0;x<attribs.length;x++) attribs[x] = ' ';
					
					if (d.dirents[0].GetReadOnly()) attribs[0] = 'R';
					if (d.dirents[0].GetSystem())   attribs[1] = 'S';
					if (d.dirents[0].GetArchive())  attribs[2] = 'A';
					System.out.print(String.valueOf(attribs));
					
					
					Plus3DosFileHeader hdr = d.GetPlus3DosHeader();
					if (hdr==null || !hdr.IsPlusThreeDosFile) {
						System.out.print(" CPM");
					} else {
						System.out.print(" +3 ");
					}
					
					System.out.print(padto(String.valueOf(hdr.filelength),6));
					
					if (hdr.filetype==Plus3DosFileHeader.FILETYPE_BASIC) {
						System.out.print(" BASIC  ");
						if (hdr.line==32768) {
							System.out.print("No autostart");
						}
						else System.out.print("line "+hdr.line);
						
					} else if (hdr.filetype==Plus3DosFileHeader.FILETYPE_CODE) {
						System.out.print(" CODE   ");
						System.out.print("Loadaddr: "+hdr.loadAddr);
						
					} else if (hdr.filetype==Plus3DosFileHeader.FILETYPE_NUMARRAY) {
						System.out.print(" NUMARR ");
						System.out.print("name:" +hdr.VarName);
						
					} else if (hdr.filetype==Plus3DosFileHeader.FILETYPE_CHRARRAY) {
						System.out.print(" CHRARR ");
						System.out.print("name:" +hdr.VarName);	
					}
					System.out.println();
				}
			}
			System.out.println();
			System.out.print(CurrentDisk.freeSpace+"K free. ");
			int numdirentsfree = CurrentDisk.maxDirEnts - CurrentDisk.usedDirEnts;
			System.out.println(numdirentsfree+" directory entries free.");
			

		} else if (cmd.equals("addnumericarray")) {
			if (params.isBlank()) {
				System.out.println(" Expecting addnumericarray (filename)");
				System.out.println(" Filenames can be a wildcard");
				result = false;
			} else {
				File fn = new File(params);
				File folder = fn.getParentFile();
				String fname = fn.getName();

				FileFilter fileFilter = new WildcardFileFilter(fname);
				File[] files = folder.listFiles(fileFilter);

				NumericArrayDialog nad = new NumericArrayDialog(shell);
				nad.init();
				
				if (files != null && files.length > 0) {
					// Load file to memory.
					for (File f : files) {
						try {
							String filename = CPM.FixFullName(f.getName());
							System.out.println("Adding "+f.getAbsolutePath()+" as "+filename);
							
							nad.LoadArrayFromFile(f);
							nad.AssembleArrayData();

							int varname = 10;
							varname = (varname & 0x1F) + 0x80;
							CurrentDisk.AddPlusThreeFile(filename, nad.ArrayAsBytes, varname * 0x100, 0,
									PlusThreeDiskWrapper.BASIC_NUMARRAY);
						} catch (Exception E) {
							System.out.println(" Error reading the file: " + E.getMessage());
							result = false;
						}
					}
				} else {
					System.out.println("No files found.");
					result = false;
				}
			}
		} else if (cmd.equals("addchararray")) {
			if (params.isBlank()) {
				System.out.println(" Expecting addchararray (filename)");
				System.out.println(" Filenames can be a wildcard");
				result = false;
			} else {
				File fn = new File(params);
				File folder = fn.getParentFile();
				String fname = fn.getName();

				FileFilter fileFilter = new WildcardFileFilter(fname);
				File[] files = folder.listFiles(fileFilter);

				CharacterArrayDialog cad = new CharacterArrayDialog(shell);
				cad.init();
				
				if (files != null && files.length > 0) {
					// Load file to memory.
					for (File f : files) {
						try {
							String filename = CPM.FixFullName(f.getName());
							System.out.println("Adding "+f.getAbsolutePath()+" as "+filename);
							
							cad.LoadArrayFromFile(f);
							cad.CalcMaxDim();
							cad.AssembleArrayData();

							int varname = 10;
							varname = (varname & 0x1F) + 0x80;
							CurrentDisk.AddPlusThreeFile(filename, cad.ArrayAsBytes, varname * 0x100, 0,
									PlusThreeDiskWrapper.BASIC_CHRARRAY);
						} catch (Exception E) {
							System.out.println(" Error reading the file: " + E.getMessage());
							result = false;
						}
					}
				} else {
					System.out.println("No files found.");
					result = false;
				}
			}
		} else if (cmd.equals("stop")) {
			if (params.toLowerCase().contentEquals("on error")) {
				StopOnError = true;
			} else if (params.toLowerCase().contentEquals("never")) {
				StopOnError = false;
			} else if (params.isEmpty()) { // force a stop.
				StopOnError = true;
				result = false;
			} else {
				System.out.println(" Expecting stop [on error|never|]");
				System.out.println("   stop on error    will stop when any error occurs");
				System.out.println("   stop never       will ignore errors from that point");
				System.out.println("   stop             immedate stop regardless of status");

				result = false;
			}
		} else {
			System.out.println("Bad command: "+cmd);
			result = false;
		}
		return (result);

	}

	/**
	 * pad a string to the given length. used for CAT command.
	 * 
	 * @param s
	 * @param length
	 * @return
	 */
	private String padto(String s,int length) {
		String result = s;
		while (result.length() < length) {
			result = result + " ";
		}		
		return(result);
	}

}
