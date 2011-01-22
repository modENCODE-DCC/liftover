package org.modencode.tools.liftover.test;


import org.junit.Test;
import org.modencode.tools.liftover.Liftover;
import org.modencode.tools.liftover.MappingException;

import com.martiansoftware.jsap.JSAPException;

public class TestChadoXMLLiftover {


	@Test
	public void testCommandLineChadoXMLLiftover() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "180",
		                 "-2", "190",
		                 "-x", "test.chadoxml",
		                 "-o", "test.chadoxml.out"
		};
		Liftover.main(args);
		
	}
}
