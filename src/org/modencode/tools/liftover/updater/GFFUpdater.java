package org.modencode.tools.liftover.updater;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.lang.StringBuilder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
		return join(" ", Arrays.asList(header));
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
	// Parses the GFF line and returns the original feature.
	private GFFFeature processLine(String line) throws MappingException {
		String[] fields = line.split("\t");
		Integer start = null, end = null;
		try { start = new Integer(Integer.parseInt(fields[3])); } catch (NumberFormatException e) { }
		try { end = new Integer(Integer.parseInt(fields[4])); } catch (NumberFormatException e) { }
		String attrs;
		if (fields.length == 8) {
			attrs = "";
		} else {
			attrs = fields[8];
		}
		GFFFeature f = new GFFFeature(fields[0], fields[1], fields[2], start, end,
				fields[5], fields[6], fields[7], attrs);
		
		return f;
	}
	
	private Boolean fileHasFasta(File gffFile) throws MappingException {
		
		long currPos ;
		char currChar;
		String lineToCheck;
		StringBuilder currLine = new StringBuilder();
		// Try to open as GZip data
		GZIPInputStream gzStream = getGZIPInputStream(gffFile);
		if (gzStream != null) {
			BufferedReader r = new BufferedReader(new InputStreamReader(gzStream));
			try {
				while ((lineToCheck = r.readLine()) != null) {
					if (lineToCheck.length() > 0 && lineToCheck.charAt(0) == '>') {
						r.close();
						return true;
					}
				}
				r.close();
				return false;
			} catch (IOException e) {
				throw new MappingException("Couldn't read " + gffFile + " as GZip", e);
			}
		} else {
			// Open as regular GFF
			RandomAccessFile gff;
			try {
				gff = new RandomAccessFile(gffFile, "r");	
			} catch (FileNotFoundException e){
				System.err.println("Couldn't open " + gffFile + " for reading.");
				throw new MappingException("Couldn't open " + gffFile, e);
			}

			try {
				// We can get a little more clever here: iterate backwards through the file and look for the FASTA there
				for(currPos = gffFile.length() - 1; currPos >= 0; currPos--){
					gff.seek(currPos);
					currChar = (char)gff.readByte();
					// Process line if it's an end-of-line char
					if(currChar == 13 || currChar == 10){
						lineToCheck = currLine.toString();
						currLine.setLength(0);
						// Ignore whitespace or comment (#) lines
						if (lineToCheck.matches(".*\t.*")) {
							// If it has a tab, it's regular GFF and we shouldn't keep looking for FASTA
							gff.close();
							return false;
						}
						if(lineToCheck.matches("^>.*")) {
							// If it starts with a ">", then there's at least some FASTA
							gff.close();
							return true;
						}
					} else {
						// Otherwise, continue building the line
						currLine.insert(0, currChar);
					}
				}
			} catch(Exception e) {
				throw new MappingException("Error when checking for FASTA data: " + e.toString());
			}
			try {
				gff.close();
			} catch (IOException e) {}
		}

		// We got to the beginning of the file!
		System.err.println("Gff seems to be empty of content other than comments!");
		return false;
	}

	public void processFile(File gffFile, File outFile) throws MappingException {
		// First, read from the end of the file to see if it has any FASTA in it
		System.out.println("Checking for FASTA data...");
		Boolean hasFasta = this.fileHasFasta(gffFile);
		if (hasFasta) {
			System.out.println("Found FASTA data.");
		} else {
			System.out.println("No FASTA data found.");
		}
		
		BufferedReader reader;
		boolean isGZIP = false;
		try {
			GZIPInputStream gzStream = getGZIPInputStream(gffFile);
			if (gzStream != null) {
				InputStreamReader streamReader = new InputStreamReader(gzStream);
				reader = new BufferedReader(streamReader);
				isGZIP = true;
				System.out.println("File is compressesed (GZIP), and percentage completion will be incorrect.");
			} else {
				FileReader fileReader = new FileReader(gffFile);
				reader = new BufferedReader(fileReader);
			}
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open " + gffFile + " for reading.");
			throw new MappingException("Couldn't open " + gffFile, e);
		}

		BufferedWriter writer;
		try {
			if (isGZIP) {
				GZIPOutputStream gzStream = new GZIPOutputStream(new FileOutputStream(outFile));
				OutputStreamWriter osw = new OutputStreamWriter(gzStream);
				writer = new BufferedWriter(osw);
			} else {
				FileWriter fileWriter = new FileWriter(outFile);
				writer = new BufferedWriter(fileWriter);
			}
		} catch (IOException e) {
			System.err.println("Couldn't open " + outFile + " for writing.");
			throw new MappingException("Couldn't open " + outFile + " for writing", e);
		}
		
		// Set up a FASTAUpdater to collect IDs if the file has FASTA data
		// Also set up a FASTAFeature to accumulate the FASTA lines.
		FASTAUpdater fasta_updater = null;
		FASTAFeature fasta_feature = null;
		
		if(hasFasta){
			fasta_updater = new FASTAUpdater();
		}
		
		long fileSize =  gffFile.length();
		long bytesProcessed = 0;
		boolean fastaMode = false ; // Are we processing the FASTA section (if it exists) ?
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
						if (line.matches("^>.*")) {
							fastaMode = true;
						}
						if( fastaMode){
							if (line.matches("^>.*")) {
								// Lift an existing feature and write it to the file
								if(fasta_feature != null){
									String toWrite = fasta_updater.liftFeature(fasta_feature).toString();
									if (toWrite != null) {
										writer.write(toWrite);
										writer.newLine();
									}
									// Then, remove that feature's ID from the FastaUpdater to catch duplicates
									fasta_updater.clearID(fasta_feature.getID()) ;
								}
								// Start up a new fasta feature
								// ID is everything between the first > and the next whitespace (or end of line) after that.
								Pattern fastaID = Pattern.compile(">(.*?)([\\s]|$)");
								Matcher fastaIDMatcher = fastaID.matcher(line);
								String foundID = "";
								while (fastaIDMatcher.find()){
									foundID = fastaIDMatcher.group(1);
								}
								//String fastaID = line.substring(line.indexOf(">")+1);
								int[] fastaFeatureInfo = fasta_updater.getFeatureInfo(foundID);
								fasta_feature = new FASTAFeature(foundID, fastaFeatureInfo, line);
							} else {
								fasta_feature.addContent(line);	// Data line - add to current FASTA feature
							}
						} else {
							GFFFeature currFeature = this.processLine(line);
							// pull out start and end BEFORE updating the feature
							// because it modifies itself
							if (!currFeature.hasLocation()) {
								writer.write(currFeature.toString());
							} else {
								int orig_start = currFeature.getStart() ;
								int orig_end = currFeature.getEnd() ;
								writer.write(updateFeature(currFeature).toString());
								String featureID = currFeature.getID();
								if(hasFasta && featureID != ""){
									fasta_updater.addID(
											featureID, 
											orig_start, 
											orig_end,
											currFeature.getChromosome(),
											currFeature.getStart(),
											currFeature.getEnd(),
											currFeature.getDroppedOrIndeterminate()
									);	
								}
							}
						}
					}
				}
				// if we're in FASTA mode, the newlines are handled above since not every input line
				// necessarily produces an output line.
				if(! fastaMode){
					writer.newLine();
				}
			}
			// Then, process the last FASTAfeature (since they're only processed above when a new one comes in)
			if(fasta_feature != null){
				String toWrite = fasta_updater.liftFeature(fasta_feature).toString();
				if (toWrite != null) {
					writer.write(toWrite);
					writer.newLine();
				}
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

	private class FASTAUpdater {
		// A list of IDs found in the GFF file that may refer to FASTA sequences.
		// Format:
		// <ID_name, fasta_info_array]>
		// fasta_info_array has assorted information about the fasta sequence
		// in order:
		//		feature_start, 
		//		feature end,
		//		chrom_index,
		//		target_start (where it is expected to start once fully lifted, based on GFF),
		//		target_end,
		//		dropped_feature (0 if it has not been dropped or made indeterminate ; 1 if it has.)
		// The feature's chromosome is chromosomes[chrom_index]
		private HashMap <String, int[]> id_references;
		
		// Worm chromosomes to provide an index to locate each feature on.
		private String[] chromosomes = {"I", "II", "III","IV","V","X"};
		
		public FASTAUpdater() {
			id_references = new HashMap<String, int[]>() ;
		}
		
		// remove the info associated with id from the fasta updater
		// once the fasta line has been written
		public void clearID(String id) {
			id_references.put(id, null);		
		}

		// you cannot add an ID that is already in the HashMap.
		// IDs are guaranteed to be unique within a gff.
		public boolean addID
							(
								String IDname, 
								int start, 
								int end, 
								String chrom, 
								int liftedStart, 
								int liftedEnd,
								boolean isDropped
							) throws MappingException {			
			if( id_references.containsKey(IDname)) {
				throw new MappingException("Can't process file that contains duplicate feature ID " + IDname);
			}
			int chromIndex = -1 ;
			// Pull out the chromosome from chromosomes
			for (int i = 0; i < this.chromosomes.length ; ++i){
				if ( chrom.equalsIgnoreCase(this.chromosomes[i])) {
					chromIndex = i;
				}
			}
			if (chromIndex == -1){
				throw new MappingException("Couldn't find chromosome " + chrom + "in lifting data!");
			}
			int[] contentArray = new int[6];
			contentArray[0] = start ;
			contentArray[1] = end;
			contentArray[2] = chromIndex;
			contentArray[3] = liftedStart ;
			contentArray[4] = liftedEnd ;
			contentArray[5] = (isDropped)? 1 : 0 ;
			// Adds the start and end to the hashMap under that ID
			id_references.put(IDname, contentArray);
			return true;
		}
		
		public int[] getFeatureInfo(String IDname){
			return id_references.get(IDname);
		}
		public String getChrom(FASTAFeature feature)
		{
			return this.chromosomes[id_references.get(feature.getID())[2]];
		}
		
		// This method actually does the heavy lifting.
		public FASTAFeature liftFeature(FASTAFeature source_feature) throws MappingException{
			// If the feature's dropped due to indeterminate length, don't even bother lifting
			if(source_feature.isDroppedOrIndeterminate()) {
				return source_feature ;
			}
			for(MappingData md : mappingData) {
				// md is each set of mappingData in the mappingData list
				// note that the MismatchPairs are encountered in reverse order so that 
				// earlier lifts won't offset the feature past later ones.
				for (MappingData.MismatchPair mm_orig : md.getMismatchPairs(getChrom(source_feature))){
					// Increment the 0-based Mismatch coordinates to be consistent with the 1-based gff
					// The Mismatch coords were originally 0-based half-open : IE, 
					// coords of 30	30 had length 0 and indicated an insertion in the 30th pos 
					// (so the base previously at 30 would become at 31)
					// coords of 25 26 indicated something that affected the base at 25.
					// Now they're 1-based (consistent with GFF) and fully-closed. 
					// a 0-length mismatch would be 31 30.
					MappingData.MismatchPair mm = mm_orig.clone();
					mm.previousMismatch.start ++ ;
					// Don't increment previousMismatch.end
					// Then lift the feature for that MismatchPair
					source_feature.doIncrementalLift(mm) ;
				}
			}
			// Check whether the lifted coordinates agree with the expected targetCoords
			if(source_feature.getStart() != source_feature.getTargetStart()){
				System.err.println("Feature " + source_feature.id + " : " +
						"Lifted start " + source_feature.getStart() + "doesn't match target start " +
						source_feature.getTargetStart());
			}
			if(source_feature.getEnd() != source_feature.getTargetEnd()){
				System.err.println("Feature " + source_feature.id + " : " +
						"Lifted end " + source_feature.getEnd() + "doesn't match target end " +
						source_feature.getTargetEnd());
			}

			return source_feature ;
		}
	}

	private class FASTAFeature {
		private String FASTA_UNKNOWN_BASE = "X" ;
		private String id ;
		private String fullHeaderLine ; // original header of the feature
		private StringBuilder fasta_content;
		private StringBuilder original_fasta_content ;
		// 1-based closed coordinates for the feature, taken from the gff line.
		private int start ;
		private int end ;
		// The goal start and end for after the feature is lifted---from the lifted gff feature
		private int target_start ;
		private int target_end ;
		private boolean is_dropped ;
		
		public FASTAFeature(String new_id, int[] info, String headerLine) throws MappingException{
			// Info format :
			// start, end, chrom, target_start, target_end, is_dropped
			this.id = new_id ;
			if (info == null){
				throw new MappingException("Couldn't find gff info for FASTA feature with ID " + new_id);
			}
			if (info.length != 6) {
				throw new MappingException("Couldn't create FASTA feature with ID " + new_id + " : insufficient information given!");
			}
			this.start = info[0] ;
			this.end = info[1] ;
			this.target_start = info[3];
			this.target_end = info[4];
			this.is_dropped = (info[5] != 0) ;
			
			this.fasta_content = new StringBuilder();
			this.original_fasta_content = new StringBuilder();
			this.fullHeaderLine = headerLine ;
		}
		
		// Checks that the reported start & end are consistent with the length of the fasta content
		public boolean hasInconsistentLength(boolean verbose){
			int lengthFromCoords = this.end - this.start + 1 ;
			boolean inconsistent = (lengthFromCoords != this.fasta_content.length());
			if (verbose && inconsistent){
				System.err.println( 
									"Feature " +
									this.id +
									" is inconsistent! The content length is " +
									this.fasta_content.length() +
									" but the length reported from the gff line is " +
									lengthFromCoords 
									);
			}
			return inconsistent ;
		}
		public boolean isDroppedOrIndeterminate(){
			return this.is_dropped ;
		}
		public boolean isChanged(){
			return ! this.fasta_content.toString().equals(this.original_fasta_content.toString()) ;
		}
		
		public String getID() {
			return id;
		}
		public int getStart(){
			return this.start;
		}
		public int getEnd(){
			return this.end;
		}
		public int getTargetStart(){
			return target_start ;
		}
		public int getTargetEnd(){
			return target_end ;
		}

		// Takes the feature (in build liftPair.previousMismatch) and 
		// updates start, end, and the data contents to match thisMismatch.
		// It does not apply offsets from previous MismatchPairs. 
		// liftPair must be given in 1-based half-open coordinates.
		// A mismatch with end = start - 1 indicates a 0-length site.
		public FASTAFeature doIncrementalLift(MappingData.MismatchPair liftPair) throws MappingException{
			assert !this.hasInconsistentLength(true);
			
			int mm_start = liftPair.previousMismatch.start;
			int mm_end = liftPair.previousMismatch.end;
			int mm_new_length = liftPair.thisMismatch.length;
			int mm_orig_length = liftPair.previousMismatch.length;
			int length_diff = mm_new_length - mm_orig_length ;
			// Is the mismatch entirely downstream of the feature (not affecting it at all) ?
			if (mm_start > this.end) {
				return this ;
			}
			// Is the mismatch entirely upstream of the feature (shifting it by length_diff, but not affecting the FASTA) ?
			if (mm_end < this.start){
				this.start += length_diff ;
				this.end += length_diff ;
				return this ;
			}
			
			// There is some overlap with the mismatch and the feature--trim mismatch to the boundaries of feature
			int trimmed_start = Math.max(mm_start, this.start) ;
			int trimmed_end = Math.min(mm_end, this.end); 
			

			// If the bases need be changed, remove or insert based on length_diff
			// if removing bases, change all bases in overlap to X and set the number to new_length.
			// (if length_diff = 0, the base(s) are being changed; set to X)
			// overlap indicies start and end are inclusive -- they are the index in fasta_content of affected bases.
			int overlap_index_start = trimmed_start - this.start;
			int overlap_index_end = trimmed_end - this.start ; 
			// The number of affected bases *inside* the feature after lifting takes place.
			// Add 1 at the end since end & start are inclusive.
			int overlap_new_length = ( trimmed_end - trimmed_start) + length_diff + 1 ;

			// If more bases are removed than the overlap had in the first place,
			// they must come from before or after the feature.
			// Also remove all bases in the overlap (set new length to 0 )
			boolean removed_extra_bases = false ;
			if (overlap_new_length < 0){
				// If the mismatch includes bases from before the feature, 
				// move start to just after the end of the new mismatch length
				// and remove all bases from overlap
				if ( mm_start < this.start){
					this.start = mm_start + mm_new_length ; 
					// end gets moved to the new start + the non-overlapped (remaining) length
					// -1 because start & end are inclusive
					this.end = this.start + (this.end - trimmed_end) - 1; // may cause 0-length feature
					overlap_new_length = 0 ; // so the filler-string-builder doesn't get passed a negative.
					removed_extra_bases = true;
				} else { 
					// mismatch start >= feature start, so the extra bases can only have been removed past the feature
					// this.start doesn't change
					// but move the end to the beginning of the (now completely removed) overlap.
					this.end = trimmed_start - 1 ; // may cause 0-length feature.
					overlap_new_length = 0 ;
					removed_extra_bases = true ;
				}	
			} 
			
			// Then, add or remove bases as necessary from the content
			// Removing bases: we don't know which bases are gone so replace all remaining bases with "N" for unknown
			// ALSO UPDATE START AND END
			if (length_diff <= 0) {
				String newBases = buildFillerString(overlap_new_length, FASTA_UNKNOWN_BASE);
				// delete(s, e) deletes contents at index s through e - 1 .
				// if s = e deletes nothing.
				// if s > e crashes.
				// Remove the existing overlap content (ois through oie inclusive)
				this.fasta_content.delete(overlap_index_start, overlap_index_end + 1);
				// Insert the new bases 
				this.fasta_content.insert(overlap_index_start, newBases);
			} else {
				// add new bases as "X" to the end of the overlap zone
				String addedBases = buildFillerString(length_diff, FASTA_UNKNOWN_BASE);
				this.fasta_content.insert(overlap_index_end + 1 , addedBases);
			}
			// Then, update end to account for the added or removed bases
			// If overlap was < 0 this has already happened
			if( ! removed_extra_bases){
					this.end += length_diff ;		
			}
			assert !this.hasInconsistentLength(true) ;
			return this ;
		}
		// returns the pass stringBuilder formatted to 70 characters, prefixed by prefix if given.
		// The length will be (70 + prefix.length) characters.
		public StringBuilder formatContent(String prefix, StringBuilder content){
			prefix = (prefix == null ? "" : prefix);
			StringBuilder formatted_content = new StringBuilder( prefix ) ;
			String separator = "\n" + prefix ;
			// insert a newline every 70th char
			for (int start = 0 ; start < content.length(); start += 70) {
				int end = start + 70 ;
				// If we've reached the last line, don't add a newline at the end
				if(end >= content.length()) {
					end = content.length() ;
					separator = "";
				}
				formatted_content.append(content.substring(start, end) + separator);
			}
			return formatted_content ;
		}
		// Returns the FASTAFeature formatted for writing to file
		public String toString(){
			StringBuilder res = new StringBuilder("") ;
			String pfx = (new GFFFeature()).getCommentPrefix() ;
			
			if(isVerbose()){
				// If the feature is dropped, comment it out completely
				if(this.isDroppedOrIndeterminate()){
					res.append( pfx +  "Following FASTA feature dropped due to indeterminate content:\n" );
					res.append(pfx + this.fullHeaderLine + "\n");
					res.append(formatContent(pfx, this.original_fasta_content));
					return res.toString() ;
					// if the feature is changed, attach the original internal structure
				} else if (this.isChanged()) {
					res.append(pfx + "The following feature's internal structure changed:\n");
					res.append(pfx+ this.fullHeaderLine + "\n");
					res.append(formatContent(pfx, this.original_fasta_content)) ;
					res.append("\n");
				}
			} else {
				// Without verbose, drop or change the feature silently
				if(this.isDroppedOrIndeterminate()){
					return null ;
				}
			}
			// Add the full header line and the actual content
			res.append(this.fullHeaderLine + "\n");
			res.append(formatContent("", this.fasta_content));
			return res.toString() ;
		}
		
		// Adds content to FASTA, stripping newlines
		public boolean addContent(String new_content) throws MappingException
		{
			// Strip newlines
			new_content = new_content.replaceAll("(\n|\r)", "") ;
			// Make sure this is a DNA / RNA string.
			// Allowed: ACGT, U, and N for any/unknown.
			// No multi-base chars or '-' (indeterminate length) allowed,
			// so that we don't have to handle them later.
			String allowed_chars = "[acgntuACGNTU]*";
			if (! new_content.matches(allowed_chars)){
				throw new MappingException(
					"Feature " + this.id + ":  Illegal character found in FASTA content\n\"" + new_content + "\"\nAllowed characters are: acgntuACGNTU");
			}
			fasta_content.append(new_content);
			original_fasta_content.append(new_content);
			return true;
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
		public boolean getDroppedOrIndeterminate(){
			return (this.dropped || this.indeterminate);
		}

		public String getID(){
			if (this.attributes == null) {
				return "";
			}
			String id = "" ;
			// We're looking for a pattern of ID=target;
			// Attribute names are case sensitive
			Pattern idAttribute = Pattern.compile(".*ID=(.*?)(;.*|$)");
			Matcher matchId = idAttribute.matcher(this.attributes);
			while (matchId.find()) {
			    id = matchId.group(1);
			}
			
			return id;
		}
		
		public String toString() {
			String res = "";
			String pfx = getCommentPrefix();
			if (changed && isVerbose()) {
				if (indeterminate){
					res += pfx + "Following feature dropped due to indeterminate length after lifting" + "\n";
					res += pfx + this.originalFeature.toString();
					return res;
				}
				if (dropped) {
					res += pfx + "Following feature dropped due to inversion: " + "\n";
					res += pfx + this.originalFeature.toString();
					return res;
				} else if (flipped) {
					res += pfx + "The following feature was inverted\n";
					res += pfx + this.originalFeature.toString() + "\n";
				} else {
					res += pfx + "The following feature's internal structure changed:\n";
					res += pfx + this.originalFeature.toString() + "\n";
				}
			}  else if (indeterminate || dropped ) {
				// if not verbose, just silently drop indeterminate and dropped features.
				return "" ;
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
	// Returns a String consisting of length instances of FillerChar 
	private static String buildFillerString(int length, String FillerChar) {
		String[] tempArray = new String[length];
		Arrays.fill(tempArray, FillerChar) ;
		return join("", Arrays.asList(tempArray));
	}
	private static String join(Object delimiter, Iterable<?> elements) {
		StringBuilder b = new StringBuilder();
		Iterator<?> i = elements.iterator();
		while (i.hasNext()) {
			b.append(i.next());
			if (i.hasNext()) { b.append(delimiter); }
		}
		return b.toString();
	}
}
