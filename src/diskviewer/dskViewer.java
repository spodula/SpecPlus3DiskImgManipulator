package diskviewer;

import java.io.IOException;
import java.util.ArrayList;


//TODO: Add basic file - Add variables into variable area?
//TODO: modify Attributes in the user interface.

//bugs

/**
 * Runnable object
 * 
 * @author Graham
 *
 */

public class dskViewer {

	public static void main(String[] args) {
		start(args);
	}

	public static void start(String[] args) {
		boolean verbose = false;
		String scriptname = "";
		String defaultfile = "";
		ArrayList<String>ParamScript = new ArrayList<String>();
		for (int i = 0; i < args.length; i++) {
			if (args[i].toUpperCase().equals("VERBOSE")) {
				verbose = true;
			}
			if (args[i].toUpperCase().startsWith("DISK=")) {
				defaultfile = args[i].substring(5);
				System.out.println("Using disk: " + defaultfile);
			}
			if (args[i].toUpperCase().startsWith("SCRIPT=")) {
				scriptname = args[i].substring(7);
				System.out.println("Using script: " + scriptname);
			}
			if (args[i].toUpperCase().startsWith("CMD=")) {
				String value= args[i].substring(4);
				ParamScript.add(value);
			}
		}

		// create the scene
		DskBrowserMainForm bmf = new DskBrowserMainForm(verbose);
		bmf.MakeForm();
		if (!defaultfile.isBlank()) {
			bmf.LoadDisk(defaultfile);
		}
		if (scriptname.isEmpty() && ParamScript.size() == 0) {
			bmf.loop();
		} else {
			bmf.ShowFormAfterScript=false;
			if (!ParamScript.isEmpty()) {
				String script[] = ParamScript.toArray(new String[ParamScript.size()]);
				bmf.ExecuteScript(script);
			}
			if (!scriptname.isBlank()) {
				bmf.script(scriptname);
			}
			if (!defaultfile.isBlank())
				try {
					bmf.CurrentDisk.save(defaultfile);
					if (!bmf.CurrentDisk.IsValid) {
						System.out.println(" Disk load error loading " + defaultfile);
					}
				} catch (IOException e) {
					System.out.println(" Disk load error loading " + defaultfile);
					System.out.println(e.getMessage());
				}
			if (bmf.ShowFormAfterScript) {
				if (bmf.CurrentDisk.IsValidCPMFileStructure) {
					bmf.browser.setText(bmf.pages.GetPage(4, bmf.CurrentDisk));
				} else {
					// otherwise, the raw disk structure.
					bmf.browser.setText(bmf.pages.GetPage(1, bmf.CurrentDisk));
				}
				bmf.loop();
			}
		}
	}

}
