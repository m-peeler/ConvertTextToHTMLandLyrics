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
import java.util.Scanner;
import java.util.regex.Pattern;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



public class ConvertTextFileToHTMLandLyrics { 
	private static String _path;
	private static String _html_output_directory;
	
	public ConvertTextFileToHTMLandLyrics(String path, String output) {
		_path = path;
		_html_output_directory = output;
	}
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static HashMap<String, String> handleMeta(BufferedReader bufferedReader) throws IOException {
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

	private static String buildSongContent(StringBuilder info, HashMap<String, String> meta,
			BufferedReader bufferedReader) throws IOException {
		
		String aline;
		StringBuilder json = new StringBuilder();
		json.append("{\"song\":{"); // start of song	
		json.append(buildPrologue(meta));
		
		aline = bufferedReader.readLine();
		if (!aline.equals("SONG")) {
			info.append("\nVerses should be preceded by SONG keyword \n");
			System.out.println(info); // echo error message
			throw new TextInputException(info.toString());
		}
		
		json.append(buildStanzas(info, bufferedReader));  //a dictionary of stanzas...keys are the stanza names
		json.append("}}"); // end of "song"; end of JSON
		return json.toString();
	}
	
	// Converts a raw "template" file into a JSON file for further processing
	// Template files are ASCII text
	public static String convertRawToTempFile(String fullFileName, StringBuilder info) throws IOException {
		String json = "";

		// For the meta data tags to a HashMap key,value string
		HashMap<String, String> meta = null;

		try {
			@SuppressWarnings("resource")
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fullFileName));

			meta = handleMeta(bufferedReader);

			// generate song code
			json = buildSongContent(info, meta, bufferedReader);

		} catch (TextInputException exp) {
			// text file has some problems - punt to caller
			throw exp;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// output this to a temporary text (HTML) file - in case something screws up
		Writer out = null;
		String tempFileName = _path + "zztemp/a" + meta.get("title") + ".txt";
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempFileName), "UTF-8"));
			out.write(json);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				out.close();
		}

		return tempFileName;
	}


	//build the inner JSON portion of all stanzas
	private static String buildStanzas(StringBuilder info, BufferedReader bufferedReader)
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

	private static String buildOneStanza(StringBuilder info, BufferedReader bufferedReader,
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

	private static String buildPrologue(HashMap<String, String> meta) {

		StringBuilder json = new StringBuilder();

		for (Map.Entry<String, String> entry : meta.entrySet()) {
			String key = entry.getKey();
			String val = entry.getValue();
			json.append ("\""+key+"\":\""+val+"\",");
		}

		return json.toString();
	}


	// make a semicolon separated string from the remaining parts of the array
	// (after part 0)
	//
	private static String remainingParts(String[] parts) {
		String[] modifiedArray = Arrays.copyOfRange(parts, 1, parts.length);
		return String.join(":", modifiedArray);
	}

	// take chord and lyric separate strings and combine them into one string
	private static String combine(String chords, String lyrics) {
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
	
	
/////////////////////////////////////////////////makes html and txt files//////////////////////////////////////////////////////
	
	
	
	// Converts a file from the raw text file
	// into an HTML formatted file for scroll display
	// OUTPUT is an HTML file with the: title-of-the-song.html AND a lyrics file:
	// title.txt
	public static String convertToHTML(String fullFileName) throws IOException {
		StringBuilder infoString = new StringBuilder();
		StringBuilder html = new StringBuilder();
		JsonObject song;
		BufferedReader bufferedReader = null;
		
		try {
			bufferedReader = new BufferedReader(new FileReader(fullFileName));
			String jline = bufferedReader.readLine();
			JsonObject obj = (JsonObject) JsonParser.parseString(jline); 
			song = obj.getAsJsonObject("song");
			
			String header = build_html_header(song);
			html.append (header);

			String body = build_html_stanzas(song);
			html.append(body);
			
			String outro = build_html_outro(song);
			html.append(outro);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			infoString.append("File Not Found Exception ERROR:");
			return infoString.toString();
		} catch (IOException e) {
			e.printStackTrace();
			infoString.append("IO Exception ERROR:");
			return infoString.toString();
		} catch (Exception e) {
								//probably text has quotes in it
			infoString.append("UNKNOWN ERROR:Invalid file format (Perhaps double-quotes?)");
			return infoString.toString();
		} finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}

		// output this to a text (HTML) file!
		JsonElement temp = song.get("title");
		String title = temp!=null?temp.getAsString():"Unknown";
		String outfilename = title;
		
		saveToTextFile(_html_output_directory, outfilename + ".html", html.toString());

		// Output lyrics to text file
		String lyrics = pullLyricsFrom(html.toString());
		saveToTextFile(_html_output_directory, outfilename + ".txt", lyrics);

		return infoString.toString();
	}


	private static String build_html_header(JsonObject song) {
		JsonElement temp = song.get("title");
		String title = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("asPerformedBy");
		String asPerformedBy = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("authors");
		String authors = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("tune");
		String tune = temp!=null?temp.getAsString():"normal";

		temp = song.get("chordedIn");
		String chordedIn = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("media");
		String media = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("capo");
		String capo = temp!=null?temp.getAsString():"0";

		temp = song.get("tempo");
		String tempo = temp!=null?temp.getAsString():"111";
		
		int t = Integer.parseInt(tempo);
		//non-linear approximation of scroll delay time needed
		//   tempo      60, 70, 80, 90,100,110,120,130
		int[] times = {300,250,210,160,110, 90, 70, 50};
		int x = (t-60)/10;   //130=7  60=0
		if (x < 0) x =0;
		if (x > times.length-1) x=times.length-1;
		int delayTime = times[x];

		temp = song.get("timesig");
		String timesig = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("keyword");
		String keyword = temp!=null?temp.getAsString():"Unknown";

		temp = song.get("event");
		String event = temp!=null?temp.getAsString():"Unknown";

		String variant = "master";  
		
		assert (title != null);
		String s = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + title
		+ "</title> <link rel=\"stylesheet\" href=\"..\\SongSupport\\view.css\">"
		+ "<script src=\"..\\SongSupport\\scroll.js\"></script>"
		+ "<meta name=\"keywords\" content=\""+keyword+"\">"
		+ "<meta name=\"events\" content=\""+event+"\">"
		+"</head>\n";

		// add content to include a "transposition" pull-down menu
		s += "<body>"
				+ "<select id=\"xpose\" onchange=\"transpose()\"><option value=\"-5\">-5</option><option value=\"-4\">-4</option>"
				+ "<option value=\"-3\">-3</option><option value=\"-2\">-2</option><option value=\"-1\">-1</option>"
				+ "<option value=\"0\" selected>0</option><option value=\"1\">1</option><option value=\"2\">2</option>"
				+ "<option value=\"3\">3</option><option value=\"4\">4</option><option value=\"5\">5</option>"
				+ "<option value=\"6\">6</option></select> ";

		s += "<script>SCROLL_INTERVAL="+delayTime+";</script>"; /// does it use this instead?

		// Title line and metadata
		s += "\n<div class=\"title\">" + title + "<span></span><div class=\"authors\">"
				+ authors + "</div></div>" + "<div class=\"info\">" + "Key: " + chordedIn
				+ "&nbsp; &nbsp; Capo: " + capo + "&nbsp; &nbsp; Tempo:" + tempo
				+ "&nbsp;&nbsp;" + timesig + "&nbsp;&nbsp;&nbsp;<i>(Variant: " + variant;
		if (tune != null && !tune.equals("normal")) {
			s += "nbsp;" + tune;
		}

		s += ")</i>"; // finish metadata line

		if (media != null && media.length() > 2) {
			s = s + "<br><a href=\"" + media + "\" target=\"_blank\">media link</a>";
		}

		s = s + "</div>";
		return s;
	}


	private static String build_html_stanzas (JsonObject song) {
		JsonObject stanzas = song.getAsJsonObject("stanzas");
		JsonElement temp = song.get("order");
		String orderstr = temp!=null?temp.getAsString():"Unknown";
		String[] order = orderstr.split(",");

		StringBuilder results = new StringBuilder("\n<div class='stanzas'>");

		//build stanzas in order listed
		for(int i=0; i<order.length; i++) {
			String stanzaName = order[i];  //find stanza name in order list
			JsonArray currentStanza = stanzas.getAsJsonArray(stanzaName); 

			if (currentStanza != null) {
				results.append ("\n<div class='stanza'>"
						+"<div class='stanzaName'>"  + stanzaName + "</div>\n");

				for (int line=0; line < currentStanza.size(); line++) {
					String aline = currentStanza.get(line).getAsString();
					///System.out.println (aline);
					String oneLine = process_aline(aline);
					results.append(oneLine);
				}
			}
			else {
				results.append ("\n<div class='stanza'>"
						+"<h1>?? Missing "  + stanzaName + "</h1>\n");
			}
			
			results.append("</div>\n\n"); //end of one stanza
		}

		results.append("</div>"); //end of all stanzas
		return results.toString();
	}



	// Parameter contents is a string with the HTML document contents (see earlier
	// procedure)
	// Write an output string to a file fname, in directory dir
	private static void saveToTextFile(String dir, String fname, String contents) throws IOException {
		Writer out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir + fname), "UTF-8"));
			out.write(contents);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (out != null)
				out.close();
		}
	}

	// process end of HTML string - put CCLI addendum
	private static String build_html_outro(JsonObject song) {
		String s = "";
		JsonElement temp = song.get("title");
		String title = temp!=null?temp.getAsString():"Unknown";
		temp = song.get("ccli");
		String ccli = temp!=null?temp.getAsString():"?";
		if (ccli.length()>2) {
			s = s + "<br><br>ccli:" + ccli + " " + title;
		} else {
			// ask for ccli
			System.out.println ("WARNING: No CCLI value for song:"+title);
		}
		return s + "</body></html>";
	}

	// processes one line of stanza into HTML
	private static String process_aline(String s) {

		StringBuilder str = new StringBuilder(s.trim());

		StringBuilder tempLyrics = new StringBuilder();
		StringBuilder tempChords = new StringBuilder();

		String currCh = ""; // current chord string

		int j = 0; // position in stanza string (always increases)

		String CHORD_DELIMITER = "|";
		String BLANK_FILLER = "___________________________________________________";

		// while not finished processing all chars...
		while (j < str.length()) {
			// find next chord position
			int nextpos = str.indexOf(CHORD_DELIMITER, j); // index from j
			// if more chords to process...
			if (nextpos != -1) {
				// add lyrics segment
				tempLyrics.append(str.substring(j, nextpos));

				// add corresponding blanks in chord string
				int np = nextpos;
				int len = currCh.length();

				if (((np - j - len) > 1) || np == 0) {
					tempChords.append(BLANK_FILLER.substring(0, np - j));// fill with blanks
				} else { // one blank minimum separation-if not at beginning
					tempChords.append("_");
				}
				j = nextpos; // move to chord position

				// find position of end of chord
				nextpos = str.indexOf(CHORD_DELIMITER, j + 1);

				currCh = "";
				if (nextpos != -1) { // if end of chord exists
					// get chord characters
					currCh = str.substring(j + 1, nextpos);

					j = nextpos + 1;
				} else {
					j = str.length(); // if missing end of chord, simply append it
				}

				String transposableChords = convertToTransposable(currCh);
				tempChords.append(transposableChords);

			} else { // if no more chords on line, simply add remaining lyrics
				tempLyrics.append(str.substring(j));
				j = str.length();
			}

		}

		while (tempLyrics.toString().indexOf("’") >= 0) { // â€™
			int i = tempLyrics.toString().indexOf("’");
			tempLyrics.replace(i, i + 1, "'");
		}
		/// Allow for HTML transpose as needed
		String chordline = "\n<div class=\"song_chords\">" + tempChords.toString().replaceAll("_", "&nbsp;") + "</div>";
		String lyricline = "\n<div class=\"song_lyrics\">" + tempLyrics.toString() + "</div>";

		return chordline + lyricline;
	}

	// pulls lyrics from the html string, previously generated
	// this is slightly more automatic than pulling lyrics from text file XML
	// hash symbols are added to make this Apple Keynote compatible for auto
	// insertion of slides
	public static String pullLyricsFrom(String fullhtml) {
		StringBuilder lyrics = new StringBuilder();
		String[] parts = fullhtml.split("</div>"); // new lines with divs
		String LYRIC_DIV = "<div class=\"song_lyrics\">";
		String TITLE_DIV = "<title>";
		String AUTHOR_DIV = "<div class=\"authors\">";

		Boolean newSlide = false;
		for (String line : parts) {
			int i = line.indexOf(LYRIC_DIV);
			if (i > -1) {
				String ly = line.substring(i + LYRIC_DIV.length());
				// check that new slide marker only appears with real lyric stanzas
				if (newSlide && ly.length() > 0 && !ly.equals("&nbsp;")) {
					lyrics.append("\n");
					newSlide = false;
				}
				if (ly.length() > 0 && !ly.equals("&nbsp;"))
					lyrics.append(ly + "\n");
			}
			// add blank line between stanzas
			i = line.indexOf("class='stanza'>");
			if (i > -1) {
				newSlide = true;
			}
			// add Title line
			i = line.indexOf(TITLE_DIV);
			if (i > -1) {
				int endIndex = line.indexOf("</title>");
				/// lyrics.append(line.substring(i+TITLE_DIV.length(), endIndex)+"\n");
				lyrics.append("SONG:" + line.substring(i + TITLE_DIV.length(), endIndex) + "  (2021-07-05)\n");
				/// find and add authors here
				i = line.indexOf(AUTHOR_DIV);
				if (i > -1) {
					endIndex = line.indexOf("]", i);
					String authors;
					if (endIndex < 0)
						authors = line.substring(i + 21); // start to end
					else
						authors = line.substring(i + 22, endIndex); // start + (AUTHOR_DIV+1) to end
					lyrics.append("-" + authors);
				}
			}
		}
		/// add extra #
		lyrics.append("\n");

		return lyrics.toString();
	}

	// take one chord as a string (A, G#m7 or Dsus)
	// convert it into HTML transposable object
	static final String[] chords = { "A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#" };
////////////////////////////////////////////////////
	public static String convertToTransposable(String ch) {
		String baseCh = ch.substring(0, 1);// get first char (base chord)
		String mod = ch.substring(1); // modifier portion if exists
		if (ch.length() > 1) {
			if (ch.charAt(1) == '#') { // include sharp symbol in base chord
				baseCh = ch.substring(0, 2);
				mod = ch.substring(2);
			}
		}
		// HTML span needs this base chord as n0, n1, n2,...etc so find index in chords
		// above
		int index = 0;
		while (index < 12 && !chords[index].equals(baseCh)) {
			index++;
		}
		String htmlSpan = "<span class=\"c" + index + "\">" + baseCh + "</span>";
		return htmlSpan + mod;
	}

	static boolean checkCCLI(String fullFileName) {
		boolean found = false;
		try {
			@SuppressWarnings("resource")
			BufferedReader bufferedReader = new BufferedReader(new FileReader(fullFileName));
			Scanner sc = new Scanner(new FileReader(fullFileName));
			while (sc.hasNext() && !found) {
				String aline = sc.next();
				found = aline.contains("ccli:");
			}
		} catch (FileNotFoundException e) {
			found = false;
		}
		return found;
	}

}