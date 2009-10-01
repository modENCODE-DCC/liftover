package org.modencode.tools.liftover.updater;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class SAMUpdater extends AbstractUpdater {

	public SAMUpdater(List<MappingData> mappingData) {
		super(mappingData);
	}
	
	public void processFile(File samFile, File outFile) throws MappingException {
		SAMFileReader reader = new SAMFileReader(samFile);
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader.getFileHeader(), true, outFile);
		
		for (SAMRecord r : reader) {
			SAMFeature f = new SAMFeature(r);
			f = (SAMFeature)this.updateFeature(f);
			writer.addAlignment(r);
		}
		
		// TODO: Implement SAM reader
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
					 * A shift to the right
					 */
					// The read
					if (f.getStart() >= mm.previousMismatch.end) {
						// The start is past the end of the mismatch, so just shift the start right
						// by the difference in length of the old and new regions
						f.setStart(f.getStart() + mm.thisMismatch.length - mm.previousMismatch.length);
					} else if (f.getStart() >= mm.previousMismatch.start && (f.getStart() - mm.previousMismatch.start) > mm.thisMismatch.length) {
						// The start was somewhere inside the changed region and is now outside because the new region is smaller,
						// so lock the start to the end of the new region (it's really somewhere between the last base of the new region
						// and the first base of the following unchanged region.)
						f.setStart(mm.previousMismatch.start + mm.thisMismatch.length);
					}

					// What about the mate?
					if (f.isPaired() && !f.getMateUnmapped()) {
						if (f.getMateChromosome() != f.getChromosome()) {
							// TODO: Deal with remapping mates on different chromosomes
							throw new RuntimeException("Can't yet deal with remapping mate on another chromosome");
						}
						if (f.getMateStart() >= mm.previousMismatch.end) {
							f.setMateStart(f.getMateStart() + mm.thisMismatch.length + mm.previousMismatch.length);
						} else if (f.getMateStart() >= mm.previousMismatch.start && (f.getMateStart() - mm.previousMismatch.start) > mm.thisMismatch.length) {
							f.setMateStart(mm.previousMismatch.start + mm.thisMismatch.length);
						}
						// Update the inferred insert size
						f.setInferredInsertSize(f.getMateEnd() - f.getStart());
					}
					/**
					 * A change in length
					 */
					if (f.getStart() < mm.previousMismatch.end && f.getEnd() > mm.previousMismatch.start) {
						// Pure insertion
						if (mm.previousMismatch.length == 0 && mm.thisMismatch.length > 0) {
							int offset = mm.previousMismatch.start - f.getStart();
							if (offset < 0) { throw new MappingException("Pure insert seems to be outside the read it's inserting into"); }
							f.setCigar(addCigarElement(f.getCigar(), offset, new CigarElement(mm.thisMismatch.length, CigarOperator.INSERTION)));
						}
						// Pure deletion
						if (mm.previousMismatch.length > 0 && mm.thisMismatch.length == 0) {
							
						}
					}
				}
			}
		}
		return f;
	}
	public Cigar addCigarElement(Cigar cigar, int position, CigarElement e) {
		Cigar newCigar = new Cigar();
		StringBuilder cigarExtender = new StringBuilder();
		for (CigarElement elem : cigar.getCigarElements()) {
			for (int i = 0; i < elem.getLength(); i++)
				cigarExtender.append(elem.getOperator());
		}
		String extendedCigar = cigarExtender.toString();
		CigarOperator curOp = CigarOperator.characterToEnum(extendedCigar.charAt(0));
		int curOpLength = 0;
		for (int i = 0; i <= extendedCigar.length(); i++) {
			if (i < position) {
				// Before the insert
				if (curOp == CigarOperator.characterToEnum(extendedCigar.charAt(i)))
					curOpLength++;
				else {
					newCigar.add(new CigarElement(curOpLength, curOp));
					curOpLength = 0;
					curOp = CigarOperator.characterToEnum(extendedCigar.charAt(i));
				}
			} else if (i >= position && i < e.getLength() + position) {
				if (e.getOperator() == CigarOperator.DELETION) {
					int consumed = 0;
					while (consumed < e.getLength() && i <= extendedCigar.length()) {
						// TODO: Finish consuming cigar
					}
				} else {
					
				}
			}
		}
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
			int left_of_read = this.getStart();
			int ins_size = thisRecord.getInferredInsertSize();
			if (ins_size != 0) {
				return left_of_read + ins_size;
			} else {
				throw new RuntimeException("No inferred insert size; can't find mate end");
			}
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
		public boolean getReadUnmapped() {
			return thisRecord.getReadUnmappedFlag();
		}
		public boolean isPaired() {
			return thisRecord.getReadPairedFlag();
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
