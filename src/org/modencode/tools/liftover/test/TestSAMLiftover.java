package org.modencode.tools.liftover.test;


import org.junit.Test;
import org.modencode.tools.liftover.Liftover;
import org.modencode.tools.liftover.MappingException;

import com.martiansoftware.jsap.JSAPException;

public class TestSAMLiftover {


	@Test
	public void testCommandLineSAMLiftover() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "180",
		                 "-2", "190",
		                 "-s", "test.sam",
		                 "-o", "test.out.sam"
		};
		Liftover.main(args);
		
	}
	@Test
	public void testCommandLineSAMLiftoverBadTags() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "190",
		                 "-2", "220",
		                 "-s", "test2.sam",
		                 "-o", "test2.out.sam",
		                 "-y", "LENIENT"
		};
		Liftover.main(args);
		
	}
	@Test
	public void testCommandLineSAMLiftoverGZ() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "190",
		                 "-2", "220",
		                 "-s", "test.sam.gz",
		                 "-o", "test.out.sam.gz",
		                 "-y", "LENIENT"
		};
		Liftover.main(args);
		
	}

}
