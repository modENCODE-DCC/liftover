package org.modencode.tools.liftover.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMTextWriter;
import net.sf.samtools.util.Md5CalculatingOutputStream;
import net.sf.samtools.util.RuntimeIOException;

public class SAMFileWriterFactoryCompressed extends SAMFileWriterFactory {
	@Override
    public SAMFileWriter makeSAMOrBAMWriter(final SAMFileHeader header, final boolean presorted, final File outputFile) {
        final String filename = outputFile.getName();
        if (filename.endsWith(".sam.gz")) {
        	return makeSAMGZWriter(header, presorted, outputFile);
        }
        return super.makeSAMOrBAMWriter(header, presorted, outputFile);
    }
	
    public SAMFileWriter makeSAMGZWriter(final SAMFileHeader header, final boolean presorted, final File outputFile) {
        try {
        	OutputStream os = new GZIPOutputStream(new FileOutputStream(outputFile, false), 16384);
            final SAMTextWriter ret = this.createMd5File
                ? new SAMTextWriter(new Md5CalculatingOutputStream(os,
                    new File(outputFile.getAbsolutePath() + ".md5")))
                : new SAMTextWriter(os);
            ret.setSortOrder(header.getSortOrder(), presorted);
            if (maxRecordsInRam != null) {
                ret.setMaxRecordsInRam(maxRecordsInRam);
            }
            ret.setHeader(header);
            return ret;
        }
        catch (IOException ioe) {
            throw new RuntimeIOException("Error opening file: " + outputFile.getAbsolutePath());
        }
    }

}
