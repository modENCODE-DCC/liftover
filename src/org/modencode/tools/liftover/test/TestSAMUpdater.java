package org.modencode.tools.liftover.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;
import net.sf.samtools.TextCigarCodec;

import org.modencode.tools.liftover.MappingData;

import org.junit.Test;
import org.modencode.tools.liftover.updater.SAMUpdater;

public class TestSAMUpdater {

	@Test
	public void testInsertIntoCigarElement() {
		SAMUpdater su = new SAMUpdater(new ArrayList<MappingData>());
		Cigar c = new Cigar();
		c.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		c.add(new CigarElement(5, CigarOperator.DELETION));
		c.add(new CigarElement(5, CigarOperator.INSERTION));
		
		System.out.println(cigarToString(c));
		assertEquals("5M5D5I", cigarToString(c));
		
		c = su.addCigarElement(c, 20, new CigarElement(2, CigarOperator.INSERTION));
		System.out.println(cigarToString(c));
		assertEquals("5M5D2I2I3I", cigarToString(c));
	}
	@Test
	public void testDeleteFromCigarElement() {
		SAMUpdater su = new SAMUpdater(new ArrayList<MappingData>());
		Cigar c = new Cigar();
		c.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		c.add(new CigarElement(5, CigarOperator.DELETION));
		c.add(new CigarElement(5, CigarOperator.INSERTION));
		
		System.out.println(cigarToString(c));		
		assertEquals("5M5D5I", cigarToString(c));
		
		c = su.addCigarElement(c, 12, new CigarElement(3, CigarOperator.DELETION));
		System.out.println(cigarToString(c));		
		assertEquals("5M5D1I3D1I", cigarToString(c));
	}
	@Test
	public void testDeleteFromMultipleCigarElements() {
		SAMUpdater su = new SAMUpdater(new ArrayList<MappingData>());
		Cigar c = new Cigar();
		c.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		c.add(new CigarElement(5, CigarOperator.INSERTION));
		c.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		c.add(new CigarElement(5, CigarOperator.DELETION));
		c.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		
		System.out.println(cigarToString(c));		
		assertEquals("5M5I5M5D5M", cigarToString(c));		

		c = su.addCigarElement(c, 12, new CigarElement(5, CigarOperator.DELETION));
		System.out.println(cigarToString(c));
		//assertEquals("5M5D5I5M", cigarToString(c));		
	}
	private String cigarToString(Cigar c) {
		String res = "";
		for (CigarElement e : c.getCigarElements()) {
			res += e.getLength();
			res += e.getOperator();
		}
		return res;
	}
}
