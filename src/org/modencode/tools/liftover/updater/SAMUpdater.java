package org.modencode.tools.liftover.updater;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMSequenceDictionary;

import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class SAMUpdater extends AbstractUpdater {

	public SAMUpdater(List<MappingData> mappingData) {
		super(mappingData);
	}
	
	public SAMFileHeader updateHeader(SAMFileHeader header) throws MappingException {
		/**This function loops through all SQ headers and updates the length 
		 * coordinate by adding the net change in length calculated from the
		 * CHROMOSOME_DIFFERENCES items.  the result will be wrong if the starting
		 * values are also wrong.  this also changes the WS build number
		 * according to what is specified in the liftover parameters
		 */
		SAMSequenceDictionary sd = header.getSequenceDictionary();
		for (int z=0; z<sd.getSequences().size(); z++) {
			int sizeDiff = 0;
			for (int i=0; i<mappingData.size(); i++) {
				String ref = sd.getSequence(z).getSequenceName();
				for (int j=0; j<mappingData.get(i).getMismatchPairs(ref).size(); j++) {
					MappingData.MismatchPair mmPair = mappingData.get(i).getMismatchPairs(ref).get(j);
					sizeDiff += mmPair.thisMismatch.length - mmPair.previousMismatch.length;
				}
			}
			sd.getSequence(z).setSequenceLength(sd.getSequence(z).getSequenceLength()+sizeDiff);
			sd.getSequence(z).setAttribute("AS", "WormBase WS"+mappingData.get(mappingData.size()-1).getRelease());
		}
		header.setSequenceDictionary(sd);
		return header;
	}
	
	public void processFile(File samFile, File outFile) throws MappingException {
		SAMFileReader reader = new SAMFileReader(samFile);
		
		SAMFileHeader header = reader.getFileHeader();
		header = this.updateHeader(header);
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(header, true, outFile);
		
		for (SAMRecord r : reader) {
			SAMFeature f = new SAMFeature(r);
			f = (SAMFeature)this.updateFeature(f);
			writer.addAlignment(r);
		}	
		writer.close();
	}
	@Override
	public AbstractFeature updateFeature(AbstractFeature af) throws MappingException {
		SAMFeature f = (SAMFeature)af;
		if (f.getStart() == null || f.getEnd() == null || f.getReadUnmapped()) {
			return f; // No need to continue for unlocated features
		}
		for (MappingData md : mappingData) {
			/*
			 * The mismatch_start value is the start of the mismatch, it is the first position which doesn't match.
			 * The mismatch_end value is the base past the end of the mismatch region, the first base which matches again 
			 * ($mismatch_start1, $mismatch_end1, $len1, $mismatch_start2, $mismatch_end2, $len2, $flipped)
			 */
			for (MappingData.MismatchPair mm_orig : md.getMismatchPairs(f.getChromosome())) {
				MappingData.MismatchPair mm = mm_orig.clone();
				if (!mm.flipped) {
					/**
					 * Simple insertions (insert in between reads)
					 */
					// The read
					int orig_start = f.getStart();
					int orig_end = f.getEnd();
					int orig_mate_start = f.getMateStart();
					int orig_mate_end = 0;
					String orig_ref_seq = f.getChromosome();
					if (f.getStart() < f.getMateStart()) {
						orig_mate_end = f.getMateEnd();
					}
					if (f.getStart() >= mm.previousMismatch.end) {
						// The start is past the end of the mismatch, so just shift the start right
						// by the difference in length of the old and new regions
						f.setStart(f.getStart() + (mm.thisMismatch.length - mm.previousMismatch.length));
					} else if (f.getStart() >= mm.previousMismatch.start && f.getEnd() <= mm.previousMismatch.end && mm.thisMismatch.length < mm.previousMismatch.length) {
						// Deleted the whole read
						f.setInferredInsertSize(0);
						f.setChromosome("*");
						f.setStart(0);
						f.setCigar(new Cigar());
						f.setReadUnmapped(true);
						f.setFirstRead(false);
						f.setSecondRead(false);
					} else if (f.getStart() >= mm.previousMismatch.start && (f.getStart() - mm.previousMismatch.start) > mm.thisMismatch.length) {
						// The start was somewhere inside the changed region and is now outside because the new region is smaller,
						// so lock the start to the end of the new region (it's really somewhere between the last base of the new region
						// and the first base of the following unchanged region.)
						f.setStart(mm.previousMismatch.start + mm.thisMismatch.length);
					}

					// What about the mate?
					if (f.isPaired() && !f.getMateUnmapped()) {
						if (f.getMateChromosome() != orig_ref_seq) {
							// TODO: Deal with remapping mates on different chromosomes
							throw new RuntimeException("Can't yet deal with remapping mate on another chromosome");
						} else {
							if (f.getMateStart() >= mm.previousMismatch.end) {
								// Mate start after any changes
								int shift = mm.thisMismatch.length - mm.previousMismatch.length;
								f.setMateStart(f.getMateStart() + shift);
							} else if (f.getMateStart() >= mm.previousMismatch.start && (f.getMateStart() - mm.previousMismatch.start) > mm.thisMismatch.length) {
								if (orig_start > orig_mate_start) {
									// Start of mate was inside changed region, which means we're in the
									// ambiguous situation where we may or may not have deleted the whole mate, 
									// since we can't see the end of the mate
									f.setMateStart(0);
									f.setInferredInsertSize(0);
									f.setMateUnmapped(true);
									f.setFirstRead(false);
									f.setSecondRead(false);
								} else {
									// We know both ends of the mate, and so it's not ambiguous.
									if (mm.previousMismatch.start <= orig_mate_start && mm.previousMismatch.end >= orig_mate_end) {
										// We deleted the whole mate!
										f.setMateStart(0);
										f.setInferredInsertSize(0);
										f.setMateUnmapped(true);
										f.setFirstRead(false);
										f.setSecondRead(false);
									} else {
										// We deleted across the start of the mate, so we want to lock the start
										// to the edge of the deleted region
										f.setMateStart(mm.previousMismatch.start + mm.thisMismatch.length);
									}
								}
							}
							if (mm.previousMismatch.length != mm.thisMismatch.length && !f.getMateUnmapped()) { 
								if (
										(orig_start < orig_mate_start && mm.previousMismatch.start > orig_start && mm.previousMismatch.end < f.getMateEnd()) ||
										(orig_start > orig_mate_start && mm.previousMismatch.start > orig_mate_start && mm.previousMismatch.end < orig_end)
								) {
									// Change in ISIZE; something changed between beginning of read and end of mate
									int shift = mm.thisMismatch.length - mm.previousMismatch.length;
									/*
									if (offset < 0) {
										shift -= offset;
									}*/
									if (orig_mate_start > orig_start) {
										// Mate after read; add shift to isize
										f.setInferredInsertSize(f.getInferredInsertSize() + shift);
									} else {
										// Mate before read; add -shift to isize
										f.setInferredInsertSize(f.getInferredInsertSize() + (-shift));
									}
								} else if (orig_start < orig_mate_start && mm.previousMismatch.start < orig_start && mm.previousMismatch.end > orig_start && f.getChromosome() != "*") {
									int offset = mm.previousMismatch.start - orig_start;
									int shift = mm.thisMismatch.length - mm.previousMismatch.length;
									if (offset < 0) {
										shift -= offset;
									}
									if (orig_mate_start > orig_start) {
										// Mate after read; add shift to isize
										f.setInferredInsertSize(f.getInferredInsertSize() + shift);
									} else {
										// Mate before read; add -shift to isize
										f.setInferredInsertSize(f.getInferredInsertSize() + (-shift));
									}
								} else if (orig_start > orig_mate_start && mm.previousMismatch.start < orig_mate_start && mm.previousMismatch.end > orig_mate_start) {
									int offset = mm.previousMismatch.start - orig_mate_start;
									int shift = mm.thisMismatch.length - mm.previousMismatch.length;
									if (offset < 0) {
										shift -= offset;
									}
									if (orig_mate_start > orig_start) {
										// Mate after read; add shift to isize
										f.setInferredInsertSize(f.getInferredInsertSize() + shift);
									} else {
										// Mate before read; add -shift to isize
										f.setInferredInsertSize(f.getInferredInsertSize() + (-shift));
									}									
								} else if (orig_start > orig_mate_start && (f.isFirstRead() || f.isSecondRead()) && mm.previousMismatch.start <= orig_end && mm.previousMismatch.end > orig_end) {
									// Deleted end of read
									int length = mm.thisMismatch.length - mm.previousMismatch.length;
									length = length - (-((mm.previousMismatch.end-1) - orig_end));
									f.setInferredInsertSize(f.getInferredInsertSize() - length);
								}
							}
						}
					}
										
					/**
					 * A change in length
					 */
					// ... of the read
					if (orig_start < mm.previousMismatch.end && orig_end > mm.previousMismatch.end) {
						if (mm.previousMismatch.length != mm.thisMismatch.length) {
							int offset = mm.previousMismatch.start - orig_start;
							//if (offset < 0) { throw new MappingException("Pure insert seems to be outside the read it's inserting into"); }
							int shift = mm.thisMismatch.length - mm.previousMismatch.length;
							if (offset < 0) {
								if (shift > 0) { shift += offset; } else { shift -= offset; }
								offset = 0;
							}
							Cigar newCigar = f.getCigar();
							if (shift > 0) {
								// Pure insertion
								// add cigar element
								newCigar = updateCigarForInsertedReference(f.getCigar(), offset, shift);
							} else if (shift < 0) {
								// Pure deletion
								// delete cigar element
								newCigar = updateCigarForDeletedReference(f.getCigar(), offset, Math.abs(shift));
							}
							f.setCigar(newCigar);
						}
					} else if (orig_end < mm.previousMismatch.end && orig_end >= mm.previousMismatch.start && orig_start < mm.previousMismatch.start) {
						if (mm.previousMismatch.length != mm.thisMismatch.length) {
							int offset = mm.previousMismatch.start - orig_start;
							int length = mm.previousMismatch.length - mm.thisMismatch.length;
							length = length - ((mm.previousMismatch.end-1) - orig_end);
							Cigar newCigar = f.getCigar();
							newCigar = updateCigarForDeletedReference(f.getCigar(), offset, length);
							f.setCigar(newCigar);
						}
					}
					// ... of the mate
					// Only applies if we're looking at a change to the mate from the perspective of the read
					// (and we haven't deleted the whole mate)
					if (orig_start < orig_mate_start && (f.isFirstRead() || f.isSecondRead())) {
						if (orig_mate_end < mm.previousMismatch.end && orig_mate_end >= mm.previousMismatch.start) {
							int length = mm.previousMismatch.length - mm.thisMismatch.length;
							length = length - ((mm.previousMismatch.end-1) - orig_mate_end);
							// All we can change is the isize
							f.setInferredInsertSize(f.getInferredInsertSize() - length);
						}
					}
				}
			}
		}
		return f;
	}
	public Cigar updateCigarForInsertedReference(Cigar cigar, int position, int length) throws IndexOutOfBoundsException {
		StringBuilder cigarExtender = new StringBuilder();
		Cigar newCigar = new Cigar();
		
		for (CigarElement elem : cigar.getCigarElements()) {
			for (int i = 0; i < elem.getLength(); i++)
				cigarExtender.append(elem.getOperator());
		}
		String extendedCigar = cigarExtender.toString();
		if (position > extendedCigar.length()) {
			throw new IndexOutOfBoundsException("Can't add to cigar string at or past the end of the original cigar (" + position + " > " + extendedCigar.length() + ")");
		}
		int curOpLength = 1;
		int iRelativeToReference = 0;
		
		CigarOperator curOp = CigarOperator.characterToEnum(extendedCigar.charAt(0));
		curOpLength = 0;
		for (int i = 0; i < extendedCigar.length(); i++) {
			if (curOp != CigarOperator.INSERTION) { iRelativeToReference++; }
			if (curOp == CigarOperator.characterToEnum(extendedCigar.charAt(i)))
				curOpLength++;
			else {
				newCigar.add(new CigarElement(curOpLength, curOp));
				curOpLength = 1;
				curOp = CigarOperator.characterToEnum(extendedCigar.charAt(i));
			}
			if (iRelativeToReference == position) {
				// Insert D if amid Ms, insert D if among Ds, insert N if among Ns
				if (curOp == CigarOperator.DELETION) {
					curOpLength += length;
				} else if (curOp == CigarOperator.SKIPPED_REGION) {
					curOpLength += length;
				} else if (curOp == CigarOperator.MATCH_OR_MISMATCH) {
					newCigar.add(new CigarElement(curOpLength, curOp));
					curOp = CigarOperator.DELETION;
					curOpLength = length;
				} else {
					throw new RuntimeException("Shouldn't be able to insert into I section of cigar!");
				}
				iRelativeToReference += length;
			}
		}
		newCigar.add(new CigarElement(curOpLength, curOp));
		return newCigar;
	}
	public Cigar updateCigarForDeletedReference(Cigar cigar, int position, int length) {
		StringBuilder cigarExtender = new StringBuilder();
		StringBuilder newCigarExtender = new StringBuilder();
		for (CigarElement elem : cigar.getCigarElements()) {
			for (int i = 0; i < elem.getLength(); i++)
				cigarExtender.append(elem.getOperator());
		}
		String extendedCigar = cigarExtender.toString();
		if (extendedCigar == "*") { extendedCigar = ""; }
		if (position > extendedCigar.length()) {
			throw new IndexOutOfBoundsException("Can't delete from cigar string at or past the end of the original cigar (" + position + " > " + extendedCigar.length() + ")");
		}
		if (position < 0) {
			throw new IllegalArgumentException("Can't delete from cigar with offset < 0!");
		}
		int curOpLength = 1;
		int iRelativeToReference = 0;
		int cursor = -1;
		for (int i = 0; iRelativeToReference < position; i++) {
			CigarOperator curOp = CigarOperator.characterToEnum(extendedCigar.charAt(i));
			if (curOp != CigarOperator.INSERTION) { iRelativeToReference++; }
			newCigarExtender.append(extendedCigar.charAt(i));
			cursor = i;
		}

		ArrayList<CigarOperator> insertsInDeletedRegion = new ArrayList<CigarOperator>();
		for (int i = cursor+1; iRelativeToReference < position+length; i++) {
			// Inside region to insert
			CigarOperator curOp = CigarOperator.characterToEnum(extendedCigar.charAt(i));
			if (curOp != CigarOperator.INSERTION) { iRelativeToReference++; }
			if (curOp == CigarOperator.MATCH_OR_MISMATCH) {
				// Replace M with I
				newCigarExtender.append(CigarOperator.INSERTION);
			} else if (curOp == CigarOperator.DELETION || curOp == CigarOperator.SKIPPED_REGION) {
				// Drop D or N
			} else if (curOp == CigarOperator.INSERTION) {
				// Keep any I and append to the end
				insertsInDeletedRegion.add(CigarOperator.INSERTION);
			}
			cursor = i;
		}
		for (CigarOperator oper : insertsInDeletedRegion) {
			newCigarExtender.append(oper.toString());
		}
		
		for (int i = cursor+1; i < extendedCigar.length(); i++) {
			newCigarExtender.append(extendedCigar.charAt(i));
		}
		
		// Translate newCigarExtender to Cigar
		String newExtendedCigar = newCigarExtender.toString();
		Cigar newCigar = new Cigar();
		
		CigarOperator curOp = CigarOperator.characterToEnum(newExtendedCigar.charAt(0));
		curOpLength = 0;
		for (int i = 0; i < newExtendedCigar.length(); i++) {
			if (curOp == CigarOperator.characterToEnum(newExtendedCigar.charAt(i)))
				curOpLength++;
			else {
				newCigar.add(new CigarElement(curOpLength, curOp));
				curOpLength = 1;
				curOp = CigarOperator.characterToEnum(newExtendedCigar.charAt(i));
			}
		}
		newCigar.add(new CigarElement(curOpLength, curOp));
		return newCigar;
	}

	public class SAMFeature extends AbstractFeature {
		private SAMRecord originalRecord = null;
		private SAMRecord thisRecord = null;
		private boolean dropped = false;
		private boolean flipped = false;
		private boolean changed = false;
		public SAMFeature(SAMRecord record) throws MappingException {
			this.thisRecord = record;
			try {
				this.originalRecord = (SAMRecord)this.thisRecord.clone();
			} catch (CloneNotSupportedException e) {
				throw new MappingException("Couldn't clone a SAMRecord", e);
			}
		}
		public String getChromosome() {
			return thisRecord.getReferenceName();
		}
		public void setChromosome(String referenceName) {
			thisRecord.setReferenceName(referenceName);
		}
		public Integer getStart() {
			return thisRecord.getAlignmentStart();
		}
		public Integer getEnd() {
			return thisRecord.getAlignmentEnd();
		}
		public Integer getMateStart() {
			return thisRecord.getMateAlignmentStart();
		}
		@Deprecated
		public SAMRecord getRecord() {
			return thisRecord;
		}
		public String getStrand() {
			boolean isForward = thisRecord.getReadNegativeStrandFlag();
			if (isForward)
				return "+";
			else
				return "-";
		}
		public Integer getMateEnd() {
			/*      read                mate
			 *  [5'------3']        [3'------5']
			 *  [---- inferred insert size ----]
			 *  or
			 *      mate                read
			 *  [3'------5']        [5'------3']
			 *             [-i size-]
			 */
			if (!isPaired() || getMateUnmapped()) {
				return null;
			}
			int ins_size = thisRecord.getInferredInsertSize();
			if (getMateStart() > getStart()) {
				int left_of_read = this.getStart();
				if (ins_size != 0) {
					return left_of_read + ins_size - 1;
				} else {
					throw new RuntimeException("No inferred insert size; can't find mate end");
				}
			} else {
				throw new UnsupportedOperationException("Can't get mate end if read comes after mate.");
			}
		}
		public Integer getInferredInsertSize() {
			return thisRecord.getInferredInsertSize();
		}
		@Deprecated @Override
		public void setEnd(Integer end) {
			thisRecord.setAlignmentEnd(end);
		}
		@Deprecated @Override
		public void setStrand(String strand) {
		}
		@Override
		public void setStart(Integer start) {
			thisRecord.setAlignmentStart(start);
		}
		public String getMateChromosome() {
			return thisRecord.getMateReferenceName();
		}
		public void setMateStart(Integer start) {
			thisRecord.setMateAlignmentStart(start);
		}
		public boolean getMateUnmapped() {
			return thisRecord.getMateUnmappedFlag();
		}
		public void setMateUnmapped(boolean isUnmapped) {
			thisRecord.setMateUnmappedFlag(isUnmapped);
		}
		public boolean getReadUnmapped() {
			return thisRecord.getReadUnmappedFlag();
		}
		public void setReadUnmapped(boolean isUnmapped) {
			thisRecord.setReadUmappedFlag(isUnmapped);
		}
		public boolean isPaired() {
			return thisRecord.getReadPairedFlag();
		}
		public boolean isFirstRead() {
			return thisRecord.getFirstOfPairFlag();
		}
		public void setFirstRead(boolean isFirstRead) {
			thisRecord.setFirstOfPairFlag(isFirstRead);
		}
		public void setSecondRead(boolean isSecondRead) {
			thisRecord.setSecondOfPairFlag(isSecondRead);
		}
		public boolean isSecondRead() {
			return thisRecord.getSecondOfPairFlag();
		}
		public void setInferredInsertSize(int inferredInsertSize) {
			thisRecord.setInferredInsertSize(inferredInsertSize);
		}
		public void setCigar(Cigar cigar) {
			thisRecord.setCigar(cigar);
		}
		public Cigar getCigar() {
			return thisRecord.getCigar();
		}

	}
}
