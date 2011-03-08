package org.modencode.tools.liftover.test;


import org.junit.Test;
import org.modencode.tools.liftover.Liftover;
import org.modencode.tools.liftover.MappingException;

import com.martiansoftware.jsap.JSAPException;

public class TestWIGLiftover {


	@Test
	public void testCommandLineWIGLiftoverBadChr() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "190",
		                 "-2", "220",
		                 "-w", "test_chr_prefix.bed",
		                 "-o", "test_chr_prefix.out.bed"
		};
		Liftover.main(args);
		
	}
}
