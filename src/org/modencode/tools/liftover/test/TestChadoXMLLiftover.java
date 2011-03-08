package org.modencode.tools.liftover.test;


import org.junit.Test;
import org.modencode.tools.liftover.Liftover;
import org.modencode.tools.liftover.MappingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import static org.junit.Assert.* ;

import com.martiansoftware.jsap.JSAPException;

public class TestChadoXMLLiftover {

	// HELPERS
	// Do the two files named by passed paths match each other?
	public boolean filesMatch(String f1, String f2)
	{
		boolean doFilesMatch = true ;
		try {
			BufferedReader reader1 = new BufferedReader(new FileReader(f1));
			BufferedReader reader2 = new BufferedReader(new FileReader(f2));
			String str1, str2;
			while ((str1 = reader1.readLine()) != null){
				str2 = reader2.readLine();
				if(str1.compareTo(str2) != 0){
					System.out.println("Lines don't match:\n" + str1 + "\n" + str2);
					doFilesMatch = false;
				}
			}
		} catch (IOException e) {
			System.out.print("ERROR: " + e.toString());
			return false ;
		}
		return doFilesMatch ;
	}
	
	// Run liftover and ensure that outfile matches target.
	public boolean liftAndMatch(String infile, String outfile, String target) {
		try {
		String args[] = {
					"-1", "75",
					"-2", "76",
					"-x", infile,
					"-o", outfile
		};
		
		Liftover.main(args);
		
		} catch( MappingException e) {
			System.out.println("ERROR:" + e);
			return false ;
		} catch (JSAPException e) {
			System.out.println("ERROR:" + e);
			return false ;
		}
		 return filesMatch(outfile, target);
	}
	// TESTS
	
	@Test // general test
	public void testCommandLineChadoXMLLiftover() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "180",
		                 "-2", "190",
		                 "-x", "test.chadoxml",
		                 "-o", "test.chadoxml.out"
		};
		Liftover.main(args);
	}
	
	@Test // Empty file
	public void testEmpty(){
		System.out.println("TESTING: testEmpty");
		assertTrue(liftAndMatch("test_empty.chadoxml", "test_empty.chadoxml.out", "test_empty.chadoxml" ));
	}
	
	@Test // File with no features, but with featurelocs
	public void testNoFeatures(){
		System.out.println("TESTING: testNoFeatures");
		assertTrue(liftAndMatch("test_nofeat.chadoxml", "tnf.cx.out", "test_nofeat.chadoxml"));
	}
	
	@Test // Malformed XML in feature -- should crash ? or what ?
	public void testBadXML(){
		System.out.println("TESTING: testBadXML");
		assertTrue(liftAndMatch("test_bad.chadoxml", "tbad.cx.out", "test_bad.chadoxml"));
	}
	
	@Test // Lots of different things to lift! -- The feature locations are copied from the GFF test file.
	// This file doesn't have much in it other than features
	public void testLift(){
		System.out.println("TESTING: testLift");
		assertTrue(liftAndMatch("test_lift.chadoxml", "tlift.cx.out", "test_lift_goal.chadoxml"));
	}
	

	// notes from EO :
	// -- we don't need to worry about things wrapping to a different line 'cause the validator doesn't generate that
	// the liftover actually looks only for features relative to chrom-features to lift
	// 
	
	// two features with same id -- won't happen

}
