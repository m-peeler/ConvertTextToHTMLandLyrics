package toHTMLandTXT;

import java.io.IOException;
import java.util.Scanner;
/**
 * Creates a main class for the ConvertTextFileToHTMLandLyrics class
 * Only functionality in this class is a main method for interaction / use of ConvertTextFileToHTMLandLyrics.java
 * 
 * @author lukekvamme and taterosen
 * @date 09/01/22
 */
public class Main {
	
	public static void main(String[] args) throws IOException {		
		String PATH = "/Users/lukekvamme/Desktop/Computer Science/353 - Software Engineering/Refactoring Lab/";
		String HTML_OUTFILE_DIR = "/Users/lukekvamme/Desktop/Computer Science/353 - Software Engineering/Refactoring Lab/";
		
		TempFolderCreator temp = new TempFolderCreator(PATH, HTML_OUTFILE_DIR);
		ConvertToHTML html = new ConvertToHTML(HTML_OUTFILE_DIR);
		
		
		System.out.print("INPUT a text filename and hit enter:");

		Scanner sc = new Scanner(System.in);
		String songName = PATH + sc.nextLine();
		if (songName.indexOf("txt") < 0) {
			songName = songName + ".txt";
		}
		sc.close();
		
		StringBuilder outString = new StringBuilder();

		String tempFileName = temp.convertRawToTempFile(songName, outString);
		//convert that temp file json file into HTML
		html.convertToHTML(tempFileName);
		
		System.out.println("Conversion complete\n\n");

	}
}
