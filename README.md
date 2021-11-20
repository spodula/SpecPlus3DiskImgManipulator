# SpecPlus3DiskImgManipulator
Editor for Spectrum +3 disk images in AMS disk format.
Supports adding/deleting basic, code, and screen files. 

Editor supports:
Viewing of raw track/sector data,
Examining of bootblock including attempting to disassemble any boot code. 

Viewing of:
  * BASIC, 
  * Code, 
  * Screen$ files
  * Arrays (Numeric and char)
 
Adding of: 
  * Raw files
  * BASIC files (from a text file)
  * CODE files
  * Screen files, from GIF, JPG, PNG -> screen$ files
  * Numeric and character arrays

# Building: 
Supports Win64 and Linux x86_64 platforms. You may be able to build others by changing the reference in the POM. 

Install Maven on your platform and ensure you have Java 11 or OpenJDK11 installed
Copy the appropriate pom (either pom-linux.xml or pom-win64.xml to pom.xml)
CD to the source folder
mvn clean package

You should then be able to run the program using the command line:
java -jar target/SpecImgManipulator-linux-86_64.jar
or 
java -jar target\SpecImgManipulator-win64.jar

Or you should be able to double click on the JAR file.  
(Note, for linux, you may have to set the executable bit; chmod +x target/SpecImgManipulator-linux-86_64.jar)
