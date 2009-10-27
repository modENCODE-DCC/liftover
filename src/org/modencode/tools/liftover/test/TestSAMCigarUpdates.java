package org.modencode.tools.liftover.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;

import org.modencode.tools.liftover.MappingData;

import org.junit.Test;
import org.modencode.tools.liftover.updater.SAMUpdater;

public class TestSAMCigarUpdates {
	@Test
	public void testInsertCigarElement() {
		System.out.println("Delete");
		SAMUpdater su = new SAMUpdater(new ArrayList<MappingData>());
		ArrayList<CigarElement> testElements = new ArrayList<CigarElement>();
		testElements.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		testElements.add(new CigarElement(5, CigarOperator.DELETION));
		testElements.add(new CigarElement(5, CigarOperator.INSERTION));
		testElements.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));

		Cigar c = new Cigar(testElements);

		System.out.println(cigarToString(c));
		assertEquals("Didn't get expected cigar string on init", "5M5D5I5M", cigarToString(c));
		
		c = su.updateCigarForInsertedReference(c, 3, 2);
		System.out.println(cigarToString(c));
		assertEquals("Couldn't insert into 5M region at beginning of cigar", "3M2D2M5D5I5M", cigarToString(c));
		
		c = new Cigar(testElements);
		c = su.updateCigarForInsertedReference(c, 7, 2);
		System.out.println(cigarToString(c));
		assertEquals("Couldn't insert into 5D in middle of cigar string", "5M7D5I5M", cigarToString(c));
	}

	@Test
	public void testDeleteCigarElement() {
		System.out.println("Delete");
		SAMUpdater su = new SAMUpdater(new ArrayList<MappingData>());
		ArrayList<CigarElement> testElements = new ArrayList<CigarElement>();
		testElements.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));
		testElements.add(new CigarElement(5, CigarOperator.DELETION));
		testElements.add(new CigarElement(5, CigarOperator.INSERTION));
		testElements.add(new CigarElement(5, CigarOperator.MATCH_OR_MISMATCH));

		Cigar c = new Cigar(testElements);

		System.out.println(cigarToString(c));
		assertEquals("Didn't get expected cigar string on init", "5M5D5I5M", cigarToString(c));
		
		c = su.updateCigarForDeletedReference(c, 1, 2);
		System.out.println(cigarToString(c));
		assertEquals("Couldn't delete second 2M of cigar string", "1M2I2M5D5I5M", cigarToString(c));

		c = new Cigar(testElements);
		c = su.updateCigarForDeletedReference(c, 6, 2);
		System.out.println(cigarToString(c));
		assertEquals("Couldn't delete second 2D at end of cigar string", "5M3D5I5M", cigarToString(c));

		c = new Cigar(testElements);
		c = su.updateCigarForDeletedReference(c, 10, 2);
		System.out.println(cigarToString(c));
		assertEquals("Couldn't delete 2M across 5I at end of cigar string", "5M5D7I3M", cigarToString(c));

		c = new Cigar(testElements);
		c = su.updateCigarForDeletedReference(c, 4, 2);
		System.out.println(cigarToString(c));
		assertEquals("Couldn't delete 1M1D near beginning of cigar string", "4M1I4D5I5M", cigarToString(c));
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
