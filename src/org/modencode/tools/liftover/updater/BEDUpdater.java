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

public class BEDUpdater extends AbstractUpdater {

	public BEDUpdater(List<MappingData> mappingData) {
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
			while ((line = reader.readLine()) != null) {
				if (line.matches("^\\s*$") || line.matches("^\\s*#.*")) {
					writer.write(line); //Comment
				} else if (line.startsWith("track") || line.startsWith("fixedStep") || line.startsWith("variableStep")) {
					writer.write(line); // Ignore anything that looks like a WIG header
				} else {
					String fields[] = line.split("\\s+");
					Integer start = new Integer(Integer.parseInt(fields[1]));
					Integer end = new Integer(Integer.parseInt(fields[2]));
					BEDFeature f = new BEDFeature(fields[0], start, end, fields[3]);
					writer.write(updateFeature(f).toString());
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
	public class BEDFeature extends AbstractFeature {
		private String score;
		private BEDFeature originalFeature;
		public BEDFeature() {}
		public BEDFeature(String chr, Integer start, Integer end, String score) {
			this.originalFeature = new BEDFeature();
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
					res += "# Following element dropped due to inversion: " + "\n";
					res += "#" + this.originalFeature.toString();
					return res;
				} else if (flipped) {
					res += "# The following element was inverted\n";
					res += "#" + this.originalFeature.toString() + "\n";
				} else {
					res += "# The following element had a structure change\n";
					res += "#" + this.originalFeature.toString() + "\n";
				}
			}
			res += this.chr + " " + this.start + " " + this.end + " " + this.score;
			return res;
		}
	}
}
