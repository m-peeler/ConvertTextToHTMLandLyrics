package toHTMLandTXT;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TempFolderCreator {
	private  String _path;
	
	public TempFolderCreator(String path, String output) {
		_path = path;
	}
	
	public String convertRawToTempFile(String fullFileName, StringBuilder info){
		String json = "";

		// For the meta data tags to a HashMap key,value string
		HashMap<String, String> meta = null;

		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fullFileName));

			meta = handleMeta(bufferedReader);

			// generate song code
			json = buildSongContent(info, meta, bufferedReader);

		} catch (Exception exception) {
			// text file has some problems - punt to caller
			exception.printStackTrace();
		}

		// output this to a temporary text (HTML) file - in case something screws up
		Writer out = null;
		String tempFileName = _path + "zztemp/aa" + meta.get("title") + ".txt";
		
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileName), "UTF-8"));
			out.write(json);
		} catch (IOException exception) {
			exception.printStackTrace();
		} finally {
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		return tempFileName;
	}

	private  HashMap<String, String> handleMeta(BufferedReader bufferedReader) throws IOException {
		HashMap<String, String> meta = new HashMap<>();
		meta.put("title", "??");
		meta.put("capo", "0");
		meta.put("tune", "normal");
		meta.put("tempo", "111");
		meta.put("timeSig", "4/4");
		
		String aline = bufferedReader.readLine();
		// Scan metadata prologue until blank line
		while (aline.length() > 1) { // while line of file is not the blank
			String[] parts = aline.split(":", 200);
			if (parts[0].length() > 1) {
				meta.put(parts[0], remainingParts(parts)); // add any meta data
			}
			aline = bufferedReader.readLine();
		}
		return meta;
	}

	// make a semicolon separated string from the remaining parts of the array
	// (after part 0)
	//
	private  String remainingParts(String[] parts) {
		String[] modifiedArray = Arrays.copyOfRange(parts, 1, parts.length);
		return String.join(":", modifiedArray);
	}

	private  String buildSongContent(StringBuilder info, HashMap<String, String> meta,
			BufferedReader bufferedReader){
		
		String aline = "";
		StringBuilder json = new StringBuilder();
		json.append("{\"song\":{"); // start of song	
		json.append(buildPrologue(meta));
		
		try {
			aline = bufferedReader.readLine();
			if (!aline.equals("SONG"))	notSong(info);

			json.append(buildStanzas(info, bufferedReader));  //a dictionary of stanzas...keys are the stanza names
			json.append("}}"); // end of "song"; end of JSON
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return json.toString();
	}
	
	private void notSong(StringBuilder info) throws TextInputException {
		info.append("\nVerses should be preceded by SONG keyword \n");
		System.out.println(info); // echo error message
		throw new TextInputException(info.toString());
	}
	// Converts a raw "template" file into a JSON file for further processing
	// Template files are ASCII text
	


	//build the inner JSON portion of all stanzas
	private  String buildStanzas(StringBuilder info, BufferedReader bufferedReader)
			throws IOException, TextInputException {
		String aline;
		aline = bufferedReader.readLine(); // get verseName
		StringBuilder json = new StringBuilder();

		json.append("\"stanzas\":{");
		while (aline != null && aline.length() > 0) {
			String verseName = aline;
			if (verseName.length() > 8 || verseName.indexOf('[') < 0) {
				info.append("\nUnexpected (long?) verse name[" + verseName
						+ "]\nMake sure all chord lines have something-even blanks \n");
				System.out.println(info); // echo error message
				throw new TextInputException(info.toString());
			}
			verseName = verseName.replace("[", "");
			verseName = verseName.replace("]", "");
			verseName = verseName.trim();

			json.append(buildOneStanza(info, bufferedReader, verseName));
			aline = bufferedReader.readLine(); // get next verseName
		}
		//REMOVE LAST COMMA
		if (json.length() > 0) {
			json.setLength(json.length() - 1);
		}
		json.append("}");  //end of stanza dictionary
		return json.toString();
	}

	private  String buildOneStanza(StringBuilder info, BufferedReader bufferedReader,
			String verseName) throws IOException, TextInputException {

		StringBuilder json = new StringBuilder ("\""+verseName+"\":[");
		String chord = bufferedReader.readLine(); // check for more...

		while (chord != null && chord.length() > 0) {
			if (chord.indexOf('[') >= 0 || Pattern.matches("[H-Z]", chord) == true) {
				info.append("Verse:" + verseName + "...Chords contain bad characters @" + chord);
				throw new TextInputException(info.toString());
			}

			String lyric = bufferedReader.readLine();		
			json.append("\"" + combine(chord, lyric) + "\",");
			info.append("\n" + lyric);
			chord = bufferedReader.readLine(); // read next
		}

		//REMOVE LAST COMMA
		if (json.length() > 0) {
			json.setLength(json.length() - 1);
		}
		json.append("],"); //finish off the stanza
		return json.toString();
	}

	private  String buildPrologue(HashMap<String, String> meta) {

		StringBuilder json = new StringBuilder();

		for (Map.Entry<String, String> entry : meta.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			json.append ("\""+key+"\":\""+val+"\",");
		}

		return json.toString();
	}


	// take chord and lyric separate strings and combine them into one string
	private  String combine(String chords, String lyrics) {
		int chPos = 0; // position within chord string
		int lyPos = 0; // position within lyric string
		String chord;
		if (lyrics == null)
			lyrics = "";

		StringBuilder combined = new StringBuilder();
		chords = chords.replaceAll("\\h", " ");   //make all spaces (and nbsp #160) the same

		while (chPos < chords.length() || lyPos < lyrics.length()) {
			if (chPos < chords.length() && chords.charAt(chPos) != ' ') {
				int nextSpace = chords.indexOf(' ', chPos);

				if (nextSpace > 0) {
					chord = chords.substring(chPos, nextSpace);
					chPos = nextSpace;
				} else {
					chord = chords.substring(chPos);
					chPos = chords.length();
				}
				combined.append("|" + chord + "|");
			} else {
				chPos++;
			}
			if (lyPos < lyrics.length()) {
				combined.append(lyrics.charAt(lyPos));
				lyPos++;
			}
		}

		return combined.toString();
	}
	
}
