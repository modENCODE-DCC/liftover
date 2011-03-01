package org.modencode.tools.liftover;

public abstract class AbstractFeature {
	protected String chr, strand;
	protected Integer start, end;
	protected boolean changed = false, flipped = false, dropped = false, indeterminate = false;

	public String getChromosome() {
		return chr;
	}
	public Integer getStart() {
		return start;
	}
	public Integer getEnd() {
		return end;
	}
	public String getStrand() {
		return strand;
	}
	public void setChanged(boolean changed) {
		this.changed = changed;
	}
	public void setFlipped(boolean flipped) {
		this.flipped = flipped;
	}
	public void setDropped(boolean dropped) {
		this.dropped = dropped;
	}
	public void setIndeterminate(boolean indeterminate){
		this.indeterminate = indeterminate;
	}
	public void setStart(Integer start) {
		this.start = start;
	}
	public void setEnd(Integer end) {
		this.end = end;
	}
	public void setStrand(String strand) {
		this.strand = strand;
	}
	// Return the line prefix that comments regarding invalidated features should use
	public String getCommentPrefix(){
		return "#liftover: " ;
	}
	public boolean hasLocation() {
		return (this.start != null && this.end != null);
	}
}
