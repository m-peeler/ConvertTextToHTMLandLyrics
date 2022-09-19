//package toHTMLandTXT;
//
//import java.io.BufferedReader;
//import java.io.BufferedWriter;
//import java.io.FileNotFoundException;
//import java.io.FileOutputStream;
//import java.io.FileReader;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.io.UnsupportedEncodingException;
//import java.io.Writer;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//
///**
// * Creates an HTML file with lyrics 
// * 
// * @author taterosen & lukekvamme
// * @date 09/07/2022
// *
// */
//
//
//public class TatesHTML {
//
//
//		private static String html_outfile_directory;
//
//		public TatesHTML(String outputPath) {
//			html_outfile_directory = outputPath;
//		}
//
//		// Converts a file from the raw text file
//		// into an HTML formatted file for scroll display
//		// OUTPUT is an HTML file with the: title-of-the-song.html AND a lyrics file:
//		// title.txt
//		public String convertToHTML(String fullFileName) throws IOException {
//			StringBuilder infoString = new StringBuilder();
//			StringBuilder html = new StringBuilder();
//			JsonObject song;
//			BufferedReader bufferedReader = null;
//
//			try {
//				bufferedReader = new BufferedReader(new FileReader(fullFileName));
//				String jline = bufferedReader.readLine();
//				JsonObject obj = (JsonObject) JsonParser.parseString(jline); 
//				song = obj.getAsJsonObject("song");
//
//				String header = build_html_header(song);
//				html.append (header);
//
//				String body = build_html_stanzas(song);
//				html.append(body);
//
//				String outro = build_html_outro(song);
//				html.append(outro);
//
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//				infoString.append("File Not Found Exception ERROR:");
//				return infoString.toString();
//			} catch (IOException e) {
//				e.printStackTrace();
//				infoString.append("IO Exception ERROR:");
//				return infoString.toString();
//			} catch (Exception e) {
//				// e.printStackTrace(); //probably text has quotes in it
//				infoString.append("UNKNOWN ERROR:Invalid file format (Perhaps double-quotes?)");
//				return infoString.toString();
//			} finally {
//				if (bufferedReader != null)
//					bufferedReader.close();
//			}
//
//			// output this to a text (HTML) file!
//			JsonElement temp = song.get("title");
//			String title = temp!=null?temp.getAsString():"Unknown";
//			String outfilename = title;
//			//				if (!variantChoice.equals("master"))
//			//					outfilename = outfilename + "-" + variantChoice;
//
//			saveToTextFile(html_outfile_directory, outfilename + ".html", html.toString());
//
//			// Output lyrics to text file
//			String lyrics = LyricCreator.pullLyricsFrom(html.toString());
//			saveToTextFile(html_outfile_directory, outfilename + ".txt", lyrics);
//
//			return infoString.toString();
//		}
//
//		private static String build_html_header(JsonObject song) {
//			JsonElement temp = song.get("title");
//			String title = temp!=null?temp.getAsString():"Unknown";
//
//			temp = song.get("authors");
//			String authors = temp!=null?temp.getAsString():"Unknown";
//
//			temp = song.get("tune");
//			String tune = temp!=null?temp.getAsString():"normal";
//
//			temp = song.get("chordedIn");
//			String chordedIn = temp!=null?temp.getAsString():"Unknown";
//
//			temp = song.get("media");
//			String media = temp!=null?temp.getAsString():"Unknown";
//
//			temp = song.get("capo");
//			String capo = temp!=null?temp.getAsString():"0";
//
//			temp = song.get("tempo");
//			String tempo = temp!=null?temp.getAsString():"111";
//
//			int t = Integer.parseInt(tempo);
//			//non-linear approximation of scroll delay time needed
//			//   tempo      60, 70, 80, 90,100,110,120,130
//			int[] times = {300,250,210,160,110, 90, 70, 50};
//			int x = (t-60)/10;   //130=7  60=0
//			if (x < 0) x =0;
//			if (x > times.length-1) x=times.length-1;
//			int delayTime = times[x];
//
//			temp = song.get("timesig");
//			String timesig = temp!=null?temp.getAsString():"Unknown";
//
//			temp = song.get("keyword");
//			String keyword = temp!=null?temp.getAsString():"Unknown";
//
//			temp = song.get("event");
//			String event = temp!=null?temp.getAsString():"Unknown";
//
//			String variant = "master";   
//
//			assert (title != null);
//			String s = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>" + title
//					+ "</title> <link rel=\"stylesheet\" href=\"..\\SongSupport\\view.css\">"
//					+ "<script src=\"..\\SongSupport\\scroll.js\"></script>"
//					+ "<meta name=\"keywords\" content=\""+keyword+"\">"
//					+ "<meta name=\"events\" content=\""+event+"\">"
//					+"</head>\n";
//
//			// add content to include a "transposition" pull-down menu
//			s += "<body>"
//					+ "<select id=\"xpose\" onchange=\"transpose()\"><option value=\"-5\">-5</option><option value=\"-4\">-4</option>"
//					+ "<option value=\"-3\">-3</option><option value=\"-2\">-2</option><option value=\"-1\">-1</option>"
//					+ "<option value=\"0\" selected>0</option><option value=\"1\">1</option><option value=\"2\">2</option>"
//					+ "<option value=\"3\">3</option><option value=\"4\">4</option><option value=\"5\">5</option>"
//					+ "<option value=\"6\">6</option></select> ";
//
//			s += "<script>SCROLL_INTERVAL="+delayTime+";</script>"; /// does it use this instead?
//
//			// Title line and metadata
//			s += "\n<div class=\"title\">" + title + "<span></span><div class=\"authors\">"
//					+ authors + "</div></div>" + "<div class=\"info\">" + "Key: " + chordedIn
//					+ "&nbsp; &nbsp; Capo: " + capo + "&nbsp; &nbsp; Tempo:" + tempo
//					+ "&nbsp;&nbsp;" + timesig + "&nbsp;&nbsp;&nbsp;<i>(Variant: " + variant;
//			if (tune != null && !tune.equals("normal")) {
//				s += "nbsp;" + tune;
//			}
//
//			s += ")</i>"; // finish metadata line
//
//			if (media != null && media.length() > 2) {
//				s = s + "<br><a href=\"" + media + "\" target=\"_blank\">media link</a>";
//			}
//
//			s = s + "</div>";
//			return s;
//		}
//
//		private static String build_html_stanzas (JsonObject song) {
//			JsonObject stanzas = song.getAsJsonObject("stanzas");
//			JsonElement temp = song.get("order");
//			StringBuilder results = build_in_order(stanzas, temp);
//
//			results.append("</div>"); //end of all stanzas
//			return results.toString();
//		}
//
//
//		private static StringBuilder build_in_order(JsonObject stanzas, JsonElement temp) {
//			String orderstr = temp!=null?temp.getAsString():"Unknown";
//			String[] order = orderstr.split(",");
//
//			StringBuilder results = new StringBuilder("\n<div class='stanzas'>");
//
//			for(int i=0; i<order.length; i++) {
//				String stanzaName = order[i];  //find stanza name in order list
//				JsonArray currentStanza = stanzas.getAsJsonArray(stanzaName); 
//
//				results = build_current_stanza(currentStanza, results, stanzaName);
//				results.append("</div>\n\n"); //end of one stanza
//			}
//
//			return results;
//		}
//
//
//		private static StringBuilder build_current_stanza(JsonArray currentStanza, StringBuilder results,
//				String stanzaName) {
//			if (currentStanza != null) {
//				results.append ("\n<div class='stanza'>"
//						+"<div class='stanzaName'>"  + stanzaName + "</div>\n");
//				add_stanza_lines(currentStanza, results);
//			}
//			else {
//				results.append ("\n<div class='stanza'>"
//						+"<h1>?? Missing "  + stanzaName + "</h1>\n");
//			}
//
//			return results;
//		}
//
//
//		private static StringBuilder add_stanza_lines(JsonArray currentStanza, StringBuilder results) {
//			for (int line=0; line < currentStanza.size(); line++) {
//				String aline = currentStanza.get(line).getAsString();
//				String oneLine = process_aline(aline);
//				results.append(oneLine);
//			}
//
//			return results;
//		}
//
//
//		// process end of HTML string - put CCLI addendum
//		private static String build_html_outro(JsonObject song) {
//			String s = "";
//			JsonElement temp = song.get("title");
//			String title = temp!=null?temp.getAsString():"Unknown";
//			temp = song.get("ccli");
//			String ccli = temp!=null?temp.getAsString():"?";
//			if (ccli.length()>2) {
//				s = s + "<br><br>ccli:" + ccli + " " + title;
//			} else {
//				// ask for ccli
//				System.out.println ("WARNING: No CCLI value for song:"+title);
//			}
//			return s + "</body></html>";
//		}
//
//
//		// Parameter contents is a string with the HTML document contents (see earlier
//		// procedure)
//		// Write an output string to a file fname, in directory dir
//		private static void saveToTextFile(String dir, String fname, String contents) throws IOException {
//			Writer out = null;
//			try {
//				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir + fname), "UTF-8"));
//				out.write(contents);
//			} catch (UnsupportedEncodingException e) {
//				e.printStackTrace();
//			} catch (FileNotFoundException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} finally {
//				if (out != null)
//					out.close();
//			}
//		}
//		
//
//
//
//
//
//
//	}
//
