package org.modencode.tools.liftover.updater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class WIGUpdater extends AbstractUpdater {

	private enum WIGType {
		FIXED_STEP, VARIABLE_STEP, BED;
		public String toString() {
			String words[] = super.toString().split("_");
			String res = words[0].toLowerCase();
			for (int i = 1; i < words.length; i++) {
				res += words[i].substring(0, 1) + words[i].substring(1).toLowerCase();
			}
			return res;
		}
	}
	public WIGUpdater(List<MappingData> mappingData) {
		super(mappingData);
	}

	public void processFile(File wigFile, File outFile) throws MappingException {
		BufferedReader reader;
		try {
			FileReader fileReader = new FileReader(wigFile);
			reader = new BufferedReader(fileReader);
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open " + wigFile + " for reading.");
			throw new MappingException("Couldn't open " + wigFile, e);
		}

		BufferedWriter writer;
		try {
			FileWriter fileWriter = new FileWriter(outFile);
			writer = new BufferedWriter(fileWriter);
		} catch (IOException e) {
			System.err.println("Couldn't open " + outFile + " for writing.");
			throw new MappingException("Couldn't open " + outFile + " for writing", e);
		}
		
		try {
			String line;
			WiggleLineParser lineParser = null;
			while ((line = reader.readLine()) != null) {
				if (line.matches("^\\s*$") || line.matches("^\\s*#.*")) {
					writer.write(line); //Comment
				} else {
					if (line.matches("^\\s*track .*")) {
						writer.write(line);
					} else {
						String fields[] = line.split("\\s+");

						// Track header; parse then write original
						String chr = getVarValue(fields, "chrom=");
						if (line.startsWith(WIGType.VARIABLE_STEP.toString())) {
							String span = getVarValue(fields, "span=");
							if (span == null) span = "1";
							lineParser = new VariableStepParser(chr, Integer.parseInt(span));
							writer.write(line); // Keep the definition line intact
						} else if (line.startsWith(WIGType.FIXED_STEP.toString())) {
							String start = getVarValue(fields, "start=");
							String step = getVarValue(fields, "step=");
							String span = getVarValue(fields, "span=");
							if (span == null) span = "1";
							lineParser = new FixedStepParser(chr, Integer.parseInt(start), Integer.parseInt(step), Integer.parseInt(span));
							// Changing anything in a fixedStep WIG is basically impossible, so convert to variablestep
							line = line.replace(WIGType.FIXED_STEP.toString(), WIGType.VARIABLE_STEP.toString());
							line = line.replaceAll("(start=\\d+\\s*)|(step=\\d+\\s*)", "");
							writer.write(line);
						} else {
							if (lineParser == null) {
								System.err.println("Wiggle parser unsure of type, trying to detect from " + line);
								if (line.matches("\\S+\\t\\d+\\t\\d+\\t\\d")) {
									System.err.println("  Detected BED format.");
									lineParser = new BEDParser();
								}
								if (lineParser == null) {
									throw new MappingException("Wiggle parser unsure of type reading line " + line);
								}
							}
							writer.write(lineParser.processLine(line).toString());
						}
					}
				}
				writer.newLine();
			}
		} catch (IOException e) {
			try {
				reader.close();
				writer.close();
			} catch (IOException e2) {}
			System.err.println("Couldn't read line from " + wigFile);
			throw new MappingException("Couldn't read line from " + wigFile, e);
		}
		try {
			reader.close();
			writer.close();
		} catch (IOException e) {
			throw new MappingException("Couldn't close file handles", e);
		}		
	}
	private String getVarValue(String[] fields, String startsWith) {
		for (String field : fields) {
			if (field.startsWith(startsWith)) {
				return field.substring(startsWith.length());
			}
		}
		return null;
	}
	
	private abstract class WiggleLineParser {
		protected String chr;
		public abstract WIGFeature processLine(String line) throws MappingException;
		public WiggleLineParser(String chr) { this.chr = chr; }
		public WiggleLineParser() {};
	}
	private class VariableStepParser extends WiggleLineParser {
		private int span;
		public VariableStepParser(String chr, int span) {
			super(chr);
			this.span = span;
		}
		public WIGFeature processLine(String line) throws MappingException {
			String[] fields = line.split("\\s+");
			Integer start = new Integer(Integer.parseInt(fields[0]));
			Integer end = start + span;
			WIGFeature f = new WIGFeature(WIGType.VARIABLE_STEP, chr, start, end, fields[1]);
			
			return (WIGFeature)updateFeature(f);
		}
	}
	private class FixedStepParser extends WiggleLineParser {
		private int step;
		private int currentStart;
		private int span = 0;
		public FixedStepParser(String chr, int start, int step, int span) {
			super(chr);
			this.currentStart = start;
			this.step = step;
			this.span = span;
		}
		public WIGFeature processLine(String line) throws MappingException {
			
			Integer start = currentStart;
			Integer end = start + span;
			currentStart += step;
			WIGFeature f = new WIGFeature(WIGType.FIXED_STEP, chr, start, end, line);
			
			return (WIGFeature)updateFeature(f);
		}
	}
	private class BEDParser extends WiggleLineParser {
		public BEDParser() {}
		public WIGFeature processLine(String line) throws MappingException {
			String[] fields = line.split("\\s+");
			Integer start = new Integer(Integer.parseInt(fields[1]));
			Integer end = new Integer(Integer.parseInt(fields[2]));
			WIGFeature f = new WIGFeature(WIGType.BED, fields[0], start, end, fields[3]);
			
			return (WIGFeature)updateFeature(f);
		}
	}
	public class WIGFeature extends AbstractFeature {
		private String score;
		private WIGType type;
		private WIGFeature originalFeature;
		public WIGFeature() {}
		public WIGFeature(WIGType type, String chr, Integer start, Integer end, String score) {
			this.originalFeature = new WIGFeature();
			this.originalFeature.type = this.type = type;
			this.originalFeature.chr = this.chr = chr;
			this.originalFeature.start = this.start = start;
			this.originalFeature.end = this.end = end;
			this.originalFeature.score = this.score = score;
			this.originalFeature.strand = this.strand = "+";
		}
		
		public String toString() {
			String res = "";
			if (changed && isVerbose()) {
				if (dropped) {
					res += "# Following position dropped due to inversion: " + "\n";
					res += "#" + this.originalFeature.toString();
					return res;
				} else if (flipped) {
					res += "# The following position was inverted\n";
					res += "#" + this.originalFeature.toString() + "\n";
				} else {
					res += "# The following span contains an internal structure change that may not be visible:\n";
					res += "#" + this.originalFeature.toString() + "\n";
				}
			}
			if (this.type == WIGType.VARIABLE_STEP || this.type == WIGType.FIXED_STEP) {
				// We write out fixedStep as variableStep so it can actually reflect
				// changes
				res += this.start + "\t" + this.score;
			} else if (this.type == WIGType.BED) {
				res += this.chr + "\t" + this.start + "\t" + this.end + "\t" + this.score;
			}
			return res;
		}
	}
}
