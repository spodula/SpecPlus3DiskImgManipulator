package diskviewer;
//features
//TODO: Add basic file - Add variables into variable area?
//TODO: Add char array
//TODO: Add Number array
//TODO: disk text/hex  search
//TODO: support hcs (Head/Cyl/Sector - disk type)
//TODO: Add B&W mode into image add. 
//TODO: new file

//bugs
 

/**
 * Runnable object
 * @author Graham
 *
 */

public class dskViewer {
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

	public static void main(String[] args) {
		start(args);  
	}
	

}
