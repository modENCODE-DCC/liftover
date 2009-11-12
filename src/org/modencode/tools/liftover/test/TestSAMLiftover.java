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
		                 "-o", "test.sam.out"
		};
		Liftover.main(args);
		
	}
}
