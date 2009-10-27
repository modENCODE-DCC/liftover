package org.modencode.tools.liftover.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;

import org.junit.Test;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;
import org.modencode.tools.liftover.updater.SAMUpdater;
import org.modencode.tools.liftover.updater.SAMUpdater.SAMFeature;

/*
 * All tests run on this wiggle file:
@HD	VN:1.0
@SQ	SN:I	LN:9999999
read_pair_001	67	I	9949430	255	50M	=	9949500	120	AACTGAATCAGTGCATGAGGATATGCCATTAATTCGTCTGAAAGGAGTTG	*
read_pair_001	131	I	9949500	255	50M	=	9949430	-120	AAGAAAAGTTTCGCAATGGTAAAATGGTTGGCAGAGAATATGGCTGATGA	*
 */
public class TestSAMUpdater {
	@Test
	public void testSimpleInsertBeforeRead() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949400, 9949400, 0, 9949400, 9949402, 2, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			assertEquals("Didn't move start two to the right", oldF.getStart()+2, f.getStart().intValue());
			assertEquals("Didn't move end two to the right", oldF.getEnd()+2, f.getEnd().intValue());
			assertEquals("Didn't move mate start two to the right", oldF.getMateStart()+2, f.getMateStart().intValue());
			if (f.isFirstRead()) {
				assertEquals("Didn't move mate end two to the right", oldF.getMateEnd()+2, f.getMateEnd().intValue());
			}
			assertEquals("ISIZE changed even though we just shifted both mate and read right", oldF.getInferredInsertSize(), f.getInferredInsertSize());
			assertEquals("Cigar string changed even though we just shifted right", oldF.getCigar(), f.getCigar());
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleDeleteBeforeRead() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949400, 9949402, 2, 9949400, 9949400, 0, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			assertEquals("Didn't move start two to the left", oldF.getStart()-2, f.getStart().intValue());
			assertEquals("Didn't move end two to the left", oldF.getEnd()-2, f.getEnd().intValue());
			assertEquals("Didn't move mate start two to the left", oldF.getMateStart()-2, f.getMateStart().intValue());
			if (f.isFirstRead()) {
				assertEquals("Didn't move mate end two to the left", oldF.getMateEnd()-2, f.getMateEnd().intValue());
			}
			assertEquals("ISIZE changed even though we just shifted both mate and read left", oldF.getInferredInsertSize(), f.getInferredInsertSize());
			assertEquals("Cigar string changed even though we just shifted left", oldF.getCigar(), f.getCigar());
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testResizeInsertBeforeRead() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949400, 9949401, 1, 9949400, 9949402, 2, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			assertEquals("Didn't move start one to the right", oldF.getStart()+1, f.getStart().intValue());
			assertEquals("Didn't move end one to the right", oldF.getEnd()+1, f.getEnd().intValue());
			assertEquals("Didn't move mate start one to the right", oldF.getMateStart()+1, f.getMateStart().intValue());
			if (f.isFirstRead()) {
				assertEquals("Didn't move mate end one to the right", oldF.getMateEnd()+1, f.getMateEnd().intValue());
			}
			assertEquals("ISIZE changed even though we just shifted both mate and read right", oldF.getInferredInsertSize(), f.getInferredInsertSize());
			assertEquals("Cigar string changed even though we just shifted right", oldF.getCigar(), f.getCigar());
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testResizeDeleteBeforeRead() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949400, 9949402, 2, 9949400, 9949401, 1, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			assertEquals("Didn't move start one to the left", oldF.getStart()-1, f.getStart().intValue());
			assertEquals("Didn't move end one to the left", oldF.getEnd()-1, f.getEnd().intValue());
			assertEquals("Didn't move mate start one to the left", oldF.getMateStart()-1, f.getMateStart().intValue());
			if (f.isFirstRead()) {
				assertEquals("Didn't move mate end one to the left", oldF.getMateEnd()-1, f.getMateEnd().intValue());
			}
			assertEquals("ISIZE changed even though we just shifted both mate and read left", oldF.getInferredInsertSize(), f.getInferredInsertSize());
			assertEquals("Cigar string changed even though we just shifted left", oldF.getCigar(), f.getCigar());
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleInsertBetweenReadAndMate() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949490, 9949490, 0, 9949490, 9949492, 2, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			if (f.isFirstRead()) {
				assertEquals("Moved read start even though insert was between read and mate", oldF.getStart().intValue(), f.getStart().intValue());
				assertEquals("Moved read end even though insert was between read and mate", oldF.getEnd().intValue(), f.getEnd().intValue());
				assertEquals("Didn't move mate start two to the right", oldF.getMateStart()+2, f.getMateStart().intValue());
				assertEquals("Didn't move mate end two to the right", oldF.getMateEnd()+2, f.getMateEnd().intValue());
				assertEquals("Didn't increase ISIZE by two even though we shifted the mate right", oldF.getInferredInsertSize()+2, f.getInferredInsertSize().intValue());
				assertEquals("Cigar string changed even though we just shifted the mate right", oldF.getCigar(), f.getCigar());
			} else {
				assertEquals("For mate: Didn't move mate start even though insert was between read and mate", oldF.getStart().intValue()+2, f.getStart().intValue());
				assertEquals("For mate: Didn't move mate end even though insert was between read and mate", oldF.getEnd().intValue()+2, f.getEnd().intValue());
				assertEquals("For mate: Moved read start even though insert was between read and mate", oldF.getMateStart().intValue(), f.getMateStart().intValue());
				assertEquals("For mate: Didn't increase ISIZE by two even though we shifted the mate right", oldF.getInferredInsertSize()+(-2), f.getInferredInsertSize().intValue());				
				assertEquals("For mate: Cigar string changed even though we just shifted the mate right", oldF.getCigar(), f.getCigar());
			}
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleDeleteBetweenReadAndMate() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949490, 9949492, 2, 9949490, 9949490, 0, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			if (f.isFirstRead()) {
				assertEquals("Moved read start even though deletion was between read and mate", oldF.getStart().intValue(), f.getStart().intValue());
				assertEquals("Moved read end even though deletion was between read and mate", oldF.getEnd().intValue(), f.getEnd().intValue());
				assertEquals("Didn't move mate start two to the left", oldF.getMateStart()-2, f.getMateStart().intValue());
				assertEquals("Didn't move mate end two to the left", oldF.getMateEnd()-2, f.getMateEnd().intValue());
				assertEquals("Didn't decrease ISIZE by two even though we shifted the mate left", oldF.getInferredInsertSize()-2, f.getInferredInsertSize().intValue());
				assertEquals("Cigar string changed even though we just shifted the mate left", oldF.getCigar(), f.getCigar());
			} else {
				assertEquals("For mate: Didn't decrease ISIZE by two even though we shifted the mate right", oldF.getInferredInsertSize()-(-2), f.getInferredInsertSize().intValue());				
			}
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleInsertAfterReadAndMate() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949560, 9949560, 0, 9949560, 9949562, 2, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			if (f.isFirstRead()) {
				assertEquals("Moved read start even though insert was after read and mate", oldF.getStart().intValue(), f.getStart().intValue());
				assertEquals("Moved read end even though insert was after read and mate", oldF.getEnd().intValue(), f.getEnd().intValue());
				assertEquals("Moved mate start even though insert was after read and mate", oldF.getMateStart().intValue(), f.getMateStart().intValue());
				assertEquals("Moved mate end even though insert was after read and mate", oldF.getMateEnd().intValue(), f.getMateEnd().intValue());
				assertEquals("Changed ISIZE even though insert was after read and mate", oldF.getInferredInsertSize().intValue(), f.getInferredInsertSize().intValue());
				assertEquals("Cigar string changed even though insert was after read and mate", oldF.getCigar(), f.getCigar());
			} else {
				assertEquals("Changed ISIZE even though insert was after read and mate", oldF.getInferredInsertSize().intValue(), f.getInferredInsertSize().intValue());
			}
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleDeleteAfterReadAndMate() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949560, 9949562, 2, 9949560, 9949560, 0, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			if (f.isFirstRead()) {
				assertEquals("Moved read start even though delete was after read and mate", oldF.getStart().intValue(), f.getStart().intValue());
				assertEquals("Moved read end even though delete was after read and mate", oldF.getEnd().intValue(), f.getEnd().intValue());
				assertEquals("Moved mate start even though delete was after read and mate", oldF.getMateStart().intValue(), f.getMateStart().intValue());
				assertEquals("Moved mate end even though delete was after read and mate", oldF.getMateEnd().intValue(), f.getMateEnd().intValue());
				assertEquals("Changed ISIZE even though delete was after read and mate", oldF.getInferredInsertSize().intValue(), f.getInferredInsertSize().intValue());
				assertEquals("Cigar string changed even though delete was after read and mate", oldF.getCigar(), f.getCigar());
			} else {
				assertEquals("Changed ISIZE even though delete was after read and mate", oldF.getInferredInsertSize().intValue(), f.getInferredInsertSize().intValue());
			}
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleInsertInsideRead() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949440, 9949440, 0, 9949440, 9949442, 2, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			if (f.isFirstRead()) {
				assertEquals("Moved read start even though insert was inside read", oldF.getStart().intValue(), f.getStart().intValue());
				assertEquals("Didn't move read end two to the right even though insert was inside read", oldF.getEnd()+2, f.getEnd().intValue());
				assertEquals("Didn't move mate start even though insert was inside read", oldF.getMateStart()+2, f.getMateStart().intValue());
				assertEquals("Didn't move mate end even though insert was inside read", oldF.getMateEnd()+2, f.getMateEnd().intValue());
				assertEquals("Didn't change ISIZE even though insert was after read and mate", oldF.getInferredInsertSize().intValue()+2, f.getInferredInsertSize().intValue());
				assertEquals("Cigar string didn't change even though insert was inside read", "10M2D40M", f.getCigar().toString());
			} else {
				assertEquals("For mate: Didn't move start two to the right even though insert was inside read", oldF.getStart()+2, f.getStart().intValue());
				assertEquals("For mate: Didn't move end two to the right even though insert was inside read", oldF.getEnd()+2, f.getEnd().intValue());
				assertEquals("For mate: Moved read start even though insert was inside read", oldF.getMateStart().intValue(), f.getMateStart().intValue());
				assertEquals("For mate: Didn't change ISIZE even though insert was after read and mate", oldF.getInferredInsertSize().intValue()+(-2), f.getInferredInsertSize().intValue());
				assertEquals("For mate: Cigar string changed even though insert was inside read", f.getCigar().toString(), f.getCigar().toString());				
			}
			writer.addAlignment(r);
		}
		writer.close();
	}
	@Test
	public void testSimpleDeleteInsideRead() throws MappingException, java.io.IOException {
		ArrayList<MappingData> mappingData = new ArrayList<MappingData>();
		MappingData mappingChange = new MappingData(1);
		mappingChange.addMismatchPair("I", 9949440, 9949442, 2, 9949440, 9949440, 0, false);
		mappingData.add(mappingChange);
		SAMUpdater updater = new SAMUpdater(mappingData);
		updater.setVerbose(true);

		SAMFileReader reader = new SAMFileReader(new File("test.sam"));
		SAMFileWriter writer = new SAMFileWriterFactory().makeSAMWriter(reader.getFileHeader(), true, System.out);
		for (SAMRecord r : reader) {
			SAMFeature f = updater.new SAMFeature(r);
			SAMFeature oldF = null;
			try {
				oldF = updater.new SAMFeature((SAMRecord)r.clone());
			} catch (CloneNotSupportedException e) {
				assertNull(e.getMessage(), e);
				return;
			}
			f = (SAMFeature)updater.updateFeature(f);
			if (f.isFirstRead()) {
				assertEquals("Moved read start even though delete was inside read", oldF.getStart().intValue(), f.getStart().intValue());
				assertEquals("Didn't move read end two to the left even though delete was inside read", oldF.getEnd()-2, f.getEnd().intValue());
				assertEquals("Didn't move mate start even though delete was inside read", oldF.getMateStart()-2, f.getMateStart().intValue());
				assertEquals("Didn't move mate end even though delete was inside read", oldF.getMateEnd()-2, f.getMateEnd().intValue());
				assertEquals("Cigar string didn't change even though delete was inside read", "10M2I38M", f.getCigar().toString());
				assertEquals("Didn't change ISIZE even though delete was after read and mate", oldF.getInferredInsertSize().intValue()-2, f.getInferredInsertSize().intValue());
			} else {
				assertEquals("For mate: Didn't move start two to the right even though insert was inside read", oldF.getStart()-2, f.getStart().intValue());
				assertEquals("For mate: Didn't move end two to the right even though insert was inside read", oldF.getEnd()-2, f.getEnd().intValue());
				assertEquals("For mate: Moved read start even though insert was inside read", oldF.getMateStart().intValue(), f.getMateStart().intValue());
				assertEquals("For mate: Didn't change ISIZE even though insert was after read and mate", oldF.getInferredInsertSize().intValue()-(-2), f.getInferredInsertSize().intValue());
				assertEquals("For mate: Cigar string changed even though insert was inside read", f.getCigar().toString(), f.getCigar().toString());				
			}
			writer.addAlignment(r);
		}
		writer.close();
	}
}
