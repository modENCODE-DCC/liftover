package org.modencode.tools.liftover.updater;

import java.util.List;

import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class AbstractUpdater {
	protected List<MappingData> mappingData;
	private boolean verbose = false;

	
	public AbstractUpdater(List<MappingData> mappingData) {
		this.mappingData = mappingData;
	}

	public AbstractFeature updateFeature(AbstractFeature f) throws MappingException {
		if (f.getStart() == null || f.getEnd() == null) {
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
				// Mismatch values are in the coordinate system starting at position 0.
				// Convert them to the GFF coordinates system starting at position 1.
				mm.previousMismatch.start++;
				mm.previousMismatch.end++;
				if (!mm.flipped) {
					// Is there a change within the boundaries of our feature?
					if (f.getStart() < mm.previousMismatch.end && f.getEnd() >= mm.previousMismatch.start) {
						f.setChanged(true);
					}
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
					if (f.getEnd() >= mm.previousMismatch.end){
						// The end is past the end of the mismatch, so just shift the end right
						// by the difference in length of the old and new regions
						f.setEnd(f.getEnd() + mm.thisMismatch.length - mm.previousMismatch.length);
					} else if (f.getStart() >= mm.previousMismatch.start && (f.getStart() - mm.previousMismatch.start) > mm.thisMismatch.length) {
						// The end was somewhere inside the changed region and is now outside because the new region is smaller,
						// so lock the end of the feature to the end of the new region (it's really somewhere between the last base 
						// of the new region and the first base of the following unchanged region.)
						f.setEnd(mm.previousMismatch.start + mm.thisMismatch.length);
					}
				} else {
					// Flipped (inversion)
					if (f.getStart() >= mm.previousMismatch.start && f.getEnd() < mm.previousMismatch.end) {
						// The feature falls entirely within the flipped region
						if (f.getStrand() == "-") { f.setStrand("+"); } else { f.setStrand("-"); } // Change strand
						f.setStart((mm.previousMismatch.start + mm.previousMismatch.end) - f.getStart()); // Set start to <start>bp from the end of the new region
						f.setEnd((mm.previousMismatch.start + mm.previousMismatch.end)- f.getEnd()); // Set end to <end>bp from the end of the new region
						if (f.getStart() > f.getEnd()){
							// Make sure start > end as required by GFF
							Integer tmp = f.getStart();
							f.setStart(f.getEnd());
							f.setEnd(tmp);
						}
						f.setChanged(true);
						f.setFlipped(true);
					} else if (
							(f.getStart() >= mm.previousMismatch.start && f.getStart() < mm.previousMismatch.end) ||
							(f.getEnd() >= mm.previousMismatch.start && f.getEnd() < mm.previousMismatch.end)) {
						// The feature overlaps the change boundary; since it's an inversion, we can't 
						// really restructure it in a useful way.
						f.setChanged(true);
						f.setDropped(true);
					}
				}
			}
		}
		return f;
	}
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	public boolean isVerbose() {
		return verbose;
	}
}
