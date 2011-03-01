package org.modencode.tools.liftover.updater;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class AbstractUpdater {
	protected List<MappingData> mappingData;
	private boolean verbose = false;
	private long lastProgress;
	
	public AbstractUpdater(List<MappingData> mappingData) {
		this.mappingData = mappingData;
	}
	
	protected void updateProgress(double fractionProgress) {
		long progress = Math.round(fractionProgress*100);
		if (progress % 10 == 0 && lastProgress != progress) {
			lastProgress = progress; 
			System.out.println("Completed " + progress + "%.");
		}
	}
	
	protected GZIPInputStream getGZIPInputStream(File gffFile) {
		GZIPInputStream gzStream = null;
		try {
			gzStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(gffFile), 16384), 8192);
			gzStream.read();
			gzStream.close();
			return new GZIPInputStream(new BufferedInputStream(new FileInputStream(gffFile), 16384), 8192);
		} catch (IOException e) {
			// Couldn't open as GZ
			try {
				if (gzStream != null) { gzStream.close(); }
			} catch (IOException ignore) { }
			return null;
		}
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
				// Mismatch values are in the coordinate system starting at position 0, and half-open.
				// Convert them to the GFF coordinates system starting at position 1, but leave half-open
				// (real GFF coordinates are fully-closed: the span includes both start and end.)
				mm.previousMismatch.start++;
				mm.previousMismatch.end++;
				if (!mm.flipped) {
					// Is there a change within the boundaries of our feature?
					if (f.getStart() < mm.previousMismatch.end && f.getEnd() >= mm.previousMismatch.start) {
						f.setChanged(true);
						// Is the mismatch entirely contained within the feature?
						if(f.getStart() <= mm.previousMismatch.start && f.getEnd() >= (mm.previousMismatch.end - 1)){
							// The end will be moved as appropriate lower down -- do nothing here
							// f.setEnd(f.getEnd() + mm.thisMismatch.length - mm.previousMismatch.length);
						} else {
							// If the mismatch overlaps one or both ends of the feature, its length after lifting is
							// indeterminate unless one or both of the following are true :
							// 		* it is a point mismatch; or,
							//		* the length doesn't change from prevMismatch to thisMismatch.
							// If neither of these are true, the feature needs to be dropped.  (This is because in a 
							// non-point mismatch adding or removing bases, there's not enough info to say how many of the
							// bases were added or removed from inside the feature, and how many outside.)
							
							// If the mismatch results in a change of length, invalidate it unless it is a point mismatch
							if (mm.thisMismatch.length != mm.previousMismatch.length) {
								if((mm.previousMismatch.length != 0) && (mm.thisMismatch.length != 0)){
									f.setIndeterminate(true);
									return f ;
								} else {
									// PrevMismatch overlaps the inside of the feature, but it's not the case that it's entirely
									// within the feature. Thus it must contain bases both inside and outside feature ; but either it
									// or thisMismatch has zero length, and it doesn't, so thisMismatch must & this is a point mismatch.
									assert mm.thisMismatch.length == 0 : "Lift target has unexpected non-zero length";
									
									int orig_length = f.getEnd() - f.getStart() + 1 ;
									// The section of the feature overlapped by the mismatch (in the feature's coords)
									int overlap_start = Math.max(f.getStart(), mm.previousMismatch.start) ;
									int overlap_end = Math.min(f.getEnd(), mm.previousMismatch.end - 1) ; 

									int overlap_length = overlap_end - overlap_start + 1 ;
									f.setStart(Math.min(f.getStart(), mm.previousMismatch.start));
									// The coordinates are fully-closed, so end - start != length
									f.setEnd(f.getStart() + orig_length - overlap_length - 1) ;
									// At this point, the feature is in its new location, and we should not do any
									// further offsetting.
									return f;
								}
							}
						}
					}
					// Then, offset feature by the difference in mismatch length if necessary.
					
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
						// This also applies to mismatches that are entirely contained within the feature
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
