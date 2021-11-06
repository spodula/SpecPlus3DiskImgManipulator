package diskviewer.libs.disk;
/**
 * Representation of a single sector including its data.
 * @author Graham
 *
 */

public class Sector {
  //Equivelent to the C, H, R flags from the 765
  public int track=0;     //Cyl
  public int side=0;      //Head
  public int sectorID=0;  //Sector
  
  //the N parameter from the 765, IE, what the FDC *thinks* its read.
  public int Sectorsz=0;
  
  //Representation of the FDC status flags when the read is completed. Used for emulation. 
  public int FDCsr1=0;
  public int FDCsr2=0;
  
  //Actual size of the data. This may differ from the default size presented in the track. 
  public int ActualSize=0;
  
  //Raw data
  public byte[] data;
}
