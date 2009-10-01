package org.modencode.tools.liftover;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class MappingData {

	private int release;
	private HashMap<String,ArrayList<MismatchPair>> mismatches;

	public MappingData(int release) {
		this.release = release;
		mismatches = new HashMap<String, ArrayList<MismatchPair>>(); 
	}

	public Set<String> getChromosomes() {
		return mismatches.keySet();
	}
	public void setRelease(int release) {
		this.release = release;
	}

	public int getRelease() {
		return release;
	}
	public int getPreviousRelease() {
		return release-1;
	}
	
	public void addMismatchPair(String chromosome, int previousStart, int previousEnd, int previousLength, int thisStart, int thisEnd, int thisLength, boolean flipped) {
		MismatchPair newPair = new MismatchPair();
		newPair.previousMismatch.start = previousStart;
		newPair.previousMismatch.end = previousEnd;
		newPair.previousMismatch.length = previousLength;
		newPair.thisMismatch.start = thisStart;
		newPair.thisMismatch.end = thisEnd;
		newPair.thisMismatch.length = thisLength;
		newPair.flipped = flipped;
		if (mismatches.get(chromosome) == null)	mismatches.put(chromosome, new ArrayList<MismatchPair>());
		mismatches.get(chromosome).add(newPair);
	}
	
	public List<MismatchPair> getMismatchPairs(String chromosome) {
		if (mismatches.get(chromosome) != null) {
			return mismatches.get(chromosome);
		} else {
			return new ArrayList<MismatchPair>();
		}
	}
	
	public class MismatchCoords {
		public int start = 0;
		public int end = 0;
		public int length = 0;
		public MismatchCoords clone() {
			MismatchCoords mc = new MismatchCoords();
			mc.start = start;
			mc.end = end;
			mc.length = length;
			return mc;
		}
	}
	public class MismatchPair {
		public MismatchCoords thisMismatch;
		public MismatchCoords previousMismatch;
		public boolean flipped = false;
		public MismatchPair() {
			this.previousMismatch = new MismatchCoords();
			this.thisMismatch = new MismatchCoords();
		}
		
		public MismatchPair clone() {
			MismatchPair mp = new MismatchPair();
			mp.previousMismatch = this.previousMismatch.clone();
			mp.thisMismatch = this.thisMismatch.clone();
			return mp;
		}
	}

	public String toString() {
		String res = "";
		res += "# RELEASE " + this.release + "\n";
		for (String chr : mismatches.keySet()) {
			res += "Chromosome: " + chr + "\n";
			for (MismatchPair mm : mismatches.get(chr)) {
				if (mm != null && mm.previousMismatch != null) {
					res += mm.previousMismatch.start + "\t";
					res += mm.previousMismatch.end + "\t";
					res += mm.previousMismatch.length + "\t";
				} else {
					res += "\t\t\t";
				}
				if (mm != null && mm.thisMismatch != null) {
					res += mm.thisMismatch.start + "\t";
					res += mm.thisMismatch.end + "\t";
					res += mm.thisMismatch.length + "\t";
				} else {
					res += "\t\t\t";
				}
				if (mm.flipped) {
					res += "1";
				} else {
					res += "0";
				}
				res += "\n";
			}
		}
		return res;
	}
}
