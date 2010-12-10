package org.modencode.tools.liftover.test;
//import java.util.List;

import org.junit.Test;
import org.modencode.tools.liftover.Liftover;
//import org.modencode.tools.liftover.MappingData;
//import org.modencode.tools.liftover.MappingDataFactory;
import org.modencode.tools.liftover.MappingException;
import org.modencode.tools.liftover.updater.BEDUpdater ;
//import org.modencode.tools.liftover.updater.WIGUpdater ;
import org.modencode.tools.liftover.updater.GFFUpdater ;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.ParseException;

import static org.junit.Assert.* ;
//import org.modencode.tools.liftover.Liftover;

public class TestGFFLiftover {
	
	/*@Test
	public void testWigLiftover() {
		// WIGType is private, so it's not possible to declare a WIGFeature from a method
		// outside of hte WIGUpdater class. If a tester is to be written, it'll have to be
		// on a whole file.

		WIGUpdater bupdater = new WIGUpdater(null) ;
		bupdater.setVerbose(true);
		
		//WIGType type = bupdater.WIGType.VARIABLE_STEP ;
		String chr = "X";
		Integer start = 1 ;
		Integer end = 100 ;
		String score = "153" ;
		
		WIGUpdater.WIGFeature f = bupdater.new WIGFeature(WIGUpdater.WIGType.BED, chr, start, end, score);
		assertEquals("BED printed wrong", f.toString(), "X 1 100 153");
		
		f.setChanged(true);
		// VERBOSE
		// Just changed
		System.out.println(f.toString());
		//  & Indeterminate
		f.setIndeterminate(true);
		System.out.println(f.toString());
		//  & Dropped
		f.setIndeterminate(false);
		f.setDropped(true);
		System.out.println(f.toString());
		//  & flipped
		f.setDropped(false);
		f.setFlipped(true);
		System.out.println(f.toString());
		
		System.out.println("----NONVERBOSE----");
		bupdater.setVerbose(false);
		f.setFlipped(false);
		
		// Nonverbose
		// just changed
		System.out.println(f.toString());
		// indet
		f.setIndeterminate(true);
		System.out.println(f.toString());
		// dropped
		f.setIndeterminate(false);
		f.setDropped(true);
		System.out.println(f.toString());
		// flipped
		f.setDropped(false);
		f.setFlipped(true);
		System.out.println(f.toString());
	
	}*/
	
	@Test
	public void testBedLiftover() {
		// i have satisfied myself that this works
		BEDUpdater bupdater = new BEDUpdater(null) ;
		bupdater.setVerbose(true);
		
		String chr = "X";
		Integer start = 1 ;
		Integer end = 100 ;
		String score = "153" ;
		
		BEDUpdater.BEDFeature f = bupdater.new BEDFeature(chr, start, end, score);
		assertEquals("BED printed wrong", f.toString(), "X 1 100 153");
		
		f.setChanged(true);
		// VERBOSE
		// Just changed
		System.out.println(f.toString());
		//  & Indeterminate
		f.setIndeterminate(true);
		System.out.println(f.toString());
		//  & Dropped
		f.setIndeterminate(false);
		f.setDropped(true);
		System.out.println(f.toString());
		//  & flipped
		f.setDropped(false);
		f.setFlipped(true);
		System.out.println(f.toString());
		
		System.out.println("----NONVERBOSE----");
		bupdater.setVerbose(false);
		f.setFlipped(false);
		
		// Nonverbose
		// just changed
		System.out.println(f.toString());
		// indet
		f.setIndeterminate(true);
		System.out.println(f.toString());
		// dropped
		f.setIndeterminate(false);
		f.setDropped(true);
		System.out.println(f.toString());
		// flipped
		f.setDropped(false);
		f.setFlipped(true);
		System.out.println(f.toString());
	
	}
	@Test
	public void testGffNoFasta() {
		 // i am satisfied that this works
		System.out.println("______GFF NO FASTA ____") ;
		GFFUpdater bupdater = new GFFUpdater(null) ;
		bupdater.setVerbose(true);
		
		String chr = "X";
		String source = "sawce";
		String type = "typo";
		Integer start = 1 ;
		Integer end = 100 ;
		String score = "153" ;
		String strand = "holmes";
		String phase = "stun";
		String attributes = "attribs";
		
		GFFUpdater.GFFFeature f = bupdater.new GFFFeature(chr, source, type, start, end, score, strand, phase, attributes);
		
		System.out.println(f.toString()) ;
		
		assertEquals("GFF printed wrong", f.toString(), "X	sawce	typo	1	100	153	holmes	stun	attribs");
		
		f.setChanged(true);
		// VERBOSE
		// Just changed
		System.out.println(f.toString());
		//  & Indeterminate
		f.setIndeterminate(true);
		System.out.println(f.toString());
		//  & Dropped
		f.setIndeterminate(false);
		f.setDropped(true);
		System.out.println(f.toString());
		//  & flipped
		f.setDropped(false);
		f.setFlipped(true);
		System.out.println(f.toString());
		
		System.out.println("----NONVERBOSE----");
		bupdater.setVerbose(false);
		f.setFlipped(false);
		
		// Nonverbose
		// just changed
		System.out.println(f.toString());
		// indet
		f.setIndeterminate(true);
		System.out.println(f.toString());
		// dropped
		f.setIndeterminate(false);
		f.setDropped(true);
		System.out.println(f.toString());
		// flipped
		f.setDropped(false);
		f.setFlipped(true);
		System.out.println(f.toString());
	
	}
	
	@Test
	public void testGffFasta() {

		String srcFile = "test_fasta.gff" ;
		String destFile = "test_fasta.out.gff";
		
		// Run a whole liftover
		String args[] = {
                "-1", "75",
                "-2", "76",
                "-g", srcFile,
                "-o", destFile
		};
		
		try {
			Liftover.main(args);
		} catch (ParseException e1) {
			e1.printStackTrace();
			fail();
		} catch (JSAPException e1) {
			e1.printStackTrace();
			fail() ;
		} catch (MappingException e1) {
			e1.printStackTrace();
			fail();
		}
			
	}


}
