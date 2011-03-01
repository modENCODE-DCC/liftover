package org.modencode.tools.liftover;

import java.io.File;
import java.util.List;

import net.sf.samtools.SAMFileReader;

import org.modencode.tools.liftover.updater.BEDUpdater;
import org.modencode.tools.liftover.updater.ChadoXMLUpdater;
import org.modencode.tools.liftover.updater.GFFUpdater;
import org.modencode.tools.liftover.updater.SAMUpdater;
import org.modencode.tools.liftover.updater.WIGUpdater;

import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.martiansoftware.jsap.stringparsers.StringStringParser;

public class Liftover {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws com.martiansoftware.jsap.ParseException, com.martiansoftware.jsap.JSAPException, MappingException {
		JSAP jsap = new SimpleJSAP(
				(new Exception()).getStackTrace()[0].getClassName(),
				"Liftover a GFF from one set of coordinates to another.",
				new Parameter[] {
						new FlaggedOption("gff", FileStringParser.getParser(), null, JSAP.NOT_REQUIRED, 'g', "gff", 
								"An input GFF file."
						),
						new FlaggedOption("wig", FileStringParser.getParser(), null, JSAP.NOT_REQUIRED, 'w', "wig", 
								"An input WIG file."
						),
						new FlaggedOption("bed", FileStringParser.getParser(), null, JSAP.NOT_REQUIRED, 'b', "bed", 
								"An input BED file."
						),
						new FlaggedOption("sam", FileStringParser.getParser(), null, JSAP.NOT_REQUIRED, 's', "sam", 
								"An input SAM file."
						),
						new FlaggedOption("xml", FileStringParser.getParser(), null, JSAP.NOT_REQUIRED, 'x', "xml", 
								"An input ChadoXML file."
						),
						new FlaggedOption("out", FileStringParser.getParser(), null, JSAP.REQUIRED, 'o', "output", 
								"The output file."
						),
						new FlaggedOption("release1", JSAP.INTEGER_PARSER, null, JSAP.REQUIRED, '1', "release1",
								"The original release number."
						),
						new FlaggedOption("release2", JSAP.INTEGER_PARSER, null, JSAP.REQUIRED, '2', "release2",
								"The destination release number."
						),
						new FlaggedOption("sam-stringency", StringStringParser.getParser(), null, JSAP.NOT_REQUIRED, 'y', "sam-stringency",
								"The SAM validation stringency; one of STRICT, LENIENT, or SILENT."
						),
				}
		);
		
		JSAPResult config = jsap.parse(args);
		if (!config.success()) {
			System.err.println();
			System.err.println("Usage: java " + (new Exception()).getStackTrace()[0].getClassName());
			System.err.println("                " + jsap.getUsage());
			System.err.println();
			System.exit(1);
		}
		
		List<MappingData> mappingData = MappingDataFactory.generateMappings(config.getInt("release1"), config.getInt("release2"));
		File outFile = config.getFile("out");
		
		if (config.contains("gff")) {
			File gffFile = config.getFile("gff");

			GFFUpdater gffu = new GFFUpdater(mappingData);
			gffu.setVerbose(true); // Includes commented lines showing what's changed
			gffu.processFile(gffFile, outFile);
		} else if (config.contains("wig")) {
			File wigFile = config.getFile("wig");
			
			WIGUpdater wigu = new WIGUpdater(mappingData);
			wigu.setVerbose(true);
			wigu.processFile(wigFile, outFile);
		} else if (config.contains("bed")) {
			File bedFile = config.getFile("bed");
			
			BEDUpdater bedu = new BEDUpdater(mappingData);
			bedu.setVerbose(true);
			bedu.processFile(bedFile, outFile);
		} else if (config.contains("sam")) {
			File samFile = config.getFile("sam");
			
			SAMUpdater samu = new SAMUpdater(mappingData);
			
			if (config.contains("sam-stringency")) {
				SAMFileReader.ValidationStringency stringency = SAMFileReader.ValidationStringency.valueOf(config.getString("sam-stringency"));
				samu.setValidationStringency(stringency);
			}
			
			samu.setVerbose(true);
			samu.processFile(samFile, outFile);
			
		} else if (config.contains("xml")) {
			File xmlFile = config.getFile("xml");
			ChadoXMLUpdater xmlu = new ChadoXMLUpdater(mappingData);
			xmlu.setVerbose(true);
			xmlu.processFile(xmlFile, outFile);
		}
		
	}
}
