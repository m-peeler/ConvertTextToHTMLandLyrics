package toHTMLandTXT;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class LyricCreator {
	private String _html_output_directory;
	private Boolean _newSlide;
	private StringBuilder _lyrics;
	private String _lyric_Div;
	private String _title_Div;
	private String _author_Div;

	
	
	public LyricCreator(String html) {
		_html_output_directory = html;
		_newSlide = false;
		_lyrics = new StringBuilder();
		_lyric_Div = "<div class=\"song_lyrics\">";
		_title_Div = "<title>";
		_author_Div = "<div class=\"authors\">";
	}


	public void createLyrics(StringBuilder html, String outFileName) {
		// Output _lyrics to text file
		String _lyrics = pull_lyricsFrom(html.toString());
		try {
			saveToTextFile(_html_output_directory, outFileName + ".txt", _lyrics);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}

	// pulls _lyrics from the html string, previously generated
	// this is slightly more automatic than pulling _lyrics from text file XML
	// hash symbols are added to make this Apple Keynote compatible for auto
	// insertion of slides
	public String pull_lyricsFrom(String fullhtml) {
		String[] parts = fullhtml.split("</div>"); // new lines with divs
		loopThroughHTMLString(parts);
		// add extra #
		_lyrics.append("\n");

		return _lyrics.toString();
	}
	
	private void loopThroughHTMLString(String[] parts) {
		for (String line : parts) {
			lyricDiv(line, _newSlide);
			blankLineBetweenStanzas(line);
			addTitleLine(line);
		}
	}
	
	private void addTitleLine(String line) {
		int index = line.indexOf(_title_Div);
		// add Title line
		if (index > -1) {
			int endIndex = line.indexOf("</title>");
			/// _lyrics.append(line.substring(i+_title_Div.length(), endIndex)+"\n");
			_lyrics.append("SONG:" + line.substring(index + _title_Div.length(), endIndex) + "  (2021-07-05)\n");
			/// find and add authors here
			index = line.indexOf(_author_Div);
			if (index > -1) {
				endIndex = line.indexOf("]", index);
				String authors;
				if (endIndex < 0)
					authors = line.substring(index + 21); // start to end
				else
					authors = line.substring(index + 22, endIndex); // start + (_author_Div+1) to end
				_lyrics.append("-" + authors);
			}
		}
	}
	
	private void blankLineBetweenStanzas(String line) {
		int index = 0;
		// add blank line between stanzas
		index = line.indexOf("class='stanza'>");
		if (index > -1) {
			_newSlide = true;
		}
	}
	
	private void lyricDiv(String line, Boolean _newSlide) {
		int index = line.indexOf(_lyric_Div);
		if (index > -1) {
			String songSub = line.substring(index + _lyric_Div.length());
			// check that new slide marker only appears with real lyric stanzas
			if (_newSlide && songSub.length() > 0 && !songSub.equals("&nbsp;")) {
				_lyrics.append("\n");
				_newSlide = false;
			}
			if (songSub.length() > 0 && !songSub.equals("&nbsp;"))
				_lyrics.append(songSub + "\n");
		}
	}
	
	// Parameter contents is a string with the HTML document contents (see earlier
		// procedure)
		// Write an output string to a file fname, in directory dir
		private void saveToTextFile(String dir, String fname, String contents) throws IOException {
			Writer out = null;
			try {
				out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dir + fname), "UTF-8"));
				out.write(contents);
			} catch (Exception e) {
				e.printStackTrace();
			}	finally {
				if (out != null)
					out.close();
			}
		}




}
