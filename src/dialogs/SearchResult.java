package dialogs;

import diskviewer.libs.disk.cpm.DirectoryEntry;

public class SearchResult {
		public int sector;
		public int head;
		public int track;
		public int location;
		public int locInFile;
		public int FileNumber;
		public DirectoryEntry FileDets;
		public String TextResult;
		
		public SearchResult(int s,int h,int t, int loc) {
			sector = s;
			head = h;
			track = t;
			location = loc;
			locInFile = 0;
			FileDets = null;
			TextResult = "";
			FileNumber = -1;
		}		
}
