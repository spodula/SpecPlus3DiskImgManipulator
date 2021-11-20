package diskviewer;
//features
//TODO: Add basic file - Add variables into variable area?

//bugs


/**
 * Runnable object
 * @author Graham
 *
 */

public class dskViewer {
	
	public static void main(String[] args) {
		start(args);  
	}
	

	public static void start(String[] args) {
		boolean verbose = false;
		for(int i=0;i<args.length;i++) {
			if (args[i].toUpperCase().equals("VERBOSE")) {
				verbose = true;
			}
		}
		
		// create the scene
		DskBrowserMainForm bmf = new DskBrowserMainForm(verbose);
		bmf.MakeForm();
		bmf.loop();
	}


}
