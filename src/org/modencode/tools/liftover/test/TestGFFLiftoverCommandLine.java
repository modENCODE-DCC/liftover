package org.modencode.tools.liftover.test;


import org.junit.Test;
import org.modencode.tools.liftover.Liftover;
import org.modencode.tools.liftover.MappingException;

import com.martiansoftware.jsap.JSAPException;

public class TestGFFLiftoverCommandLine {

	@Test
	public void testCommandLineGFFLiftover() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "190",
		                 "-2", "220",
		                 "-g", "test.gff",
		                 "-o", "test.out.gff"
		};
		Liftover.main(args);
		
	}
	@Test
	public void testCommandLineGFFLiftoverGZ() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "190",
		                 "-2", "220",
		                 "-g", "test_gzipped.gff.gz",
		                 "-o", "test_gzipped.out.gff.gz"
		};
		Liftover.main(args);
		
	}
	@Test
	public void testCommandLineGFFLiftoverNoStart() throws MappingException, JSAPException {
		String args[] = {
		                 "-1", "190",
		                 "-2", "220",
		                 "-g", "test_no_start.gff",
		                 "-o", "test_no_start.out.gff"
		};
		Liftover.main(args);
		
	}
	@Test
	public void testCommandLineGFFLiftoverFASTA() throws MappingException, JSAPException {
		String args[] = {
                "-1", "190",
                "-2", "220",
                "-g", "test_fasta.gff",
                "-o", "test_fasta.out.gff"
		};
		Liftover.main(args);
		
	}
	@Test
	public void testCommandLineGFFLiftoverGZFASTA() throws MappingException, JSAPException {
		String args[] = {
                "-1", "190",
                "-2", "220",
                "-g", "test_fasta.gff.gz",
                "-o", "test_fasta.out.gff.gz"
		};
		Liftover.main(args);
		
	}}
