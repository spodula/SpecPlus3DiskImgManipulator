package diskviewer;
//features

//TODO: Add basic file - Add variables into variable area?
//TODO: In disk building, add a single command mode of the form: disk=diskname command="<command>" which will load and save the disk
//TODO: modify Attributes. 

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
		for (int i = 0; i < args.length; i++) {
			if (args[i].toUpperCase().equals("VERBOSE")) {
				verbose = true;
			}
			if (args[i].toUpperCase().startsWith("SCRIPT=")) {
				scriptname = args[i].substring(7);
				System.out.println("Using script: " + scriptname);
			}
		}

		// create the scene
		DskBrowserMainForm bmf = new DskBrowserMainForm(verbose);
		bmf.MakeForm();
		if (scriptname.isEmpty()) {
			bmf.loop();
		} else {
			bmf.script(scriptname);
		}
	}

}
