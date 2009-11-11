package org.modencode.tools.liftover.updater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class GFFUpdater extends AbstractUpdater {
	public GFFUpdater(List<MappingData> mappingData) {
		super(mappingData);
	}
	
	private String processHeader(String line) throws MappingException {
		/**This delegates updates for the directives "sequence-region"
		 * and "genome-build", both of which are specific to the contents 
		 * of the file
		 */
		String[] header = line.split("\\s+");
		if (header[0].contains("genome-build")) {
			header = updateGenomeBuild(header);
		}
		if (header[0].contains("sequence-region")) {
			header = updateSequenceRegion(header);
		}
		return StringUtils.join(header, " ");
	}
	private String[] updateGenomeBuild(String[] genomeBuild) throws MappingException {
		/** This changes the WS build number according to what is specified 
		 * in the liftover parameters
		 */
		
		// ##genome-build source buildnum
		String buildnum = genomeBuild[2];
		buildnum = "WS" + mappingData.get(mappingData.size()-1).getRelease();		
		genomeBuild[2] = buildnum;
		return genomeBuild;
	}
	
	private String[] updateSequenceRegion(String[] sequenceRegion) throws MappingException {
		/**This function updates the start and end coordinates for the current 
		 * sequence-region directive by adding the net change in length calculated 
		 * from the CHROMOSOME_DIFFERENCES items.  the result will be wrong if 
		 * the starting values are also wrong.  although most of the time the
		 * sequence-regions will start at a value of 1, this will allow for other 
		 * starting values 
		 */ 
		 //##sequence-region reference start end		 
		String ref = sequenceRegion[1];
		int newStart = Integer.parseInt(sequenceRegion[2]);
		int newEnd = Integer.parseInt(sequenceRegion[3]);
		for (int i=0; i<mappingData.size(); i++) {
			for (int j=0; j<mappingData.get(i).getMismatchPairs(ref).size(); j++) {
				MappingData.MismatchPair mmPair = mappingData.get(i).getMismatchPairs(ref).get(j);
				int sizeDiff = mmPair.thisMismatch.length - mmPair.previousMismatch.length;
				if (mmPair.previousMismatch.start > Integer.parseInt(sequenceRegion[2])) {						
					newEnd+=sizeDiff;
				} else {
					newStart+=sizeDiff;
				}
			}
		}
		sequenceRegion[2] = Integer.toString(newStart);
		sequenceRegion[3] = Integer.toString(newEnd);
		return sequenceRegion;
		
	}
	
	private GFFFeature processLine(String line) throws MappingException {
		String[] fields = line.split("\t");
		Integer start = null, end = null;
		try { start = new Integer(Integer.parseInt(fields[3])); } catch (NumberFormatException e) { }
		try { end = new Integer(Integer.parseInt(fields[4])); } catch (NumberFormatException e) { }
		GFFFeature f = new GFFFeature(fields[0], fields[1], fields[2], start, end,
				fields[5], fields[6], fields[7], fields[8]);
		
		return (GFFFeature)updateFeature(f);
	}


	public void processFile(File gffFile, File outFile) throws MappingException {
		BufferedReader reader;
		try {
			FileReader fileReader = new FileReader(gffFile);
			reader = new BufferedReader(fileReader);
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open " + gffFile + " for reading.");
			throw new MappingException("Couldn't open " + gffFile, e);
		}

		BufferedWriter writer;
		try {
			FileWriter fileWriter = new FileWriter(outFile);
			writer = new BufferedWriter(fileWriter);
		} catch (IOException e) {
			System.err.println("Couldn't open " + outFile + " for writing.");
			throw new MappingException("Couldn't open " + outFile + " for writing", e);
		}
		
		long fileSize =  gffFile.length();
		long bytesProcessed = 0;
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				bytesProcessed += line.length()+1;
				this.updateProgress(bytesProcessed/(double)fileSize);
				if (line.matches("^\\s*##.*")) {
					writer.write(this.processHeader(line)); //directives
				} else {
					if (line.matches("^\\s*$") || line.matches("^\\s*#.*")) {
						writer.write(line); // blank line or comments
					} else {
						writer.write(this.processLine(line).toString());
					}
				}
				writer.newLine();
			}
		} catch (IOException e) {
			System.err.println("Couldn't read line from " + gffFile);
			throw new MappingException("Couldn't read line from " + gffFile, e);
		}
		try {
			reader.close();
			writer.close();
		} catch (IOException e) {
			throw new MappingException("Couldn't close file handles", e);
		}
	}

	

	public class GFFFeature extends AbstractFeature {
		private String source, type, score, phase;
		private String attributes;
		private GFFFeature originalFeature;
		
		private GFFFeature() { }
		public GFFFeature(String chr, String source, String type, Integer start, Integer end, String score, String strand,
				String phase, String attributes) {
			this.originalFeature = new GFFFeature();
			this.originalFeature.chr = this.chr = chr;
			this.originalFeature.source = this.source = source;
			this.originalFeature.type = this.type = type;
			this.originalFeature.start = this.start = start;
			this.originalFeature.end = this.end = end;
			this.originalFeature.score = this.score = score;
			this.originalFeature.strand = this.strand = strand;
			this.originalFeature.phase = this.phase = phase;
			this.originalFeature.attributes = this.attributes = attributes;
		}
		public String toString() {
			String res = "";
			if (changed && isVerbose()) {
				if (dropped) {
					res += "# Following feature dropped due to inversion: " + "\n";
					res += "#" + this.originalFeature.toString();
					return res;
				} else if (flipped) {
					res += "# The following feature was inverted\n";
					res += "#" + this.originalFeature.toString() + "\n";
				} else {
					res += "# The following feature's internal structure changed:\n";
					res += "#" + this.originalFeature.toString() + "\n";
				}
			}
			if (chr != null) res += this.chr; else res += ".";
			res += "\t";
			if (source != null) res += this.source; else res += ".";
			res += "\t";
			if (type != null) res += this.type; else res += ".";
			res += "\t";
			if (start != null && start > 0) res += this.start; else res += ".";
			res += "\t";
			if (end != null && end > 0) res += this.end; else res += ".";
			res += "\t";
			if (score != null) res += this.score; else res += ".";
			res += "\t";
			if (strand != null) res += this.strand; else res += ".";
			res += "\t";
			if (phase != null) res += this.phase; else res += ".";
			res += "\t";
			if (attributes != null) res += this.attributes;
			
			return res;
		}
	}
}
