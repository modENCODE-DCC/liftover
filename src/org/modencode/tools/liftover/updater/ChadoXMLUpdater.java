package org.modencode.tools.liftover.updater;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.FileNotFoundException;

import org.modencode.tools.liftover.AbstractFeature;
import org.modencode.tools.liftover.MappingData;
import org.modencode.tools.liftover.MappingException;

public class ChadoXMLUpdater extends AbstractUpdater {

	private enum ChadoXML {
		FEATURE, FEATURELOC, SRCFEATURE_ID
	}
	public ChadoXMLUpdater(List<MappingData> mappingData) {
		super(mappingData);
	}
	
	public void processFile(File chadoXMLFile, File outFile) throws MappingException {
		RandomAccessFile fileReader;
		try {
			fileReader = new RandomAccessFile(chadoXMLFile, "r");
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open " + chadoXMLFile + " for reading.");
			throw new MappingException("Couldn't open " + chadoXMLFile, e);
		}
		
		BufferedWriter writer;
		try {
			FileWriter fileWriter = new FileWriter(outFile);
			writer = new BufferedWriter(fileWriter);
		} catch (IOException e) {
			System.err.println("Couldn't open " + outFile + " for writing.");
			throw new MappingException("Couldn't open " + outFile + " for writing", e);
		}
		
		// Collect all feature_ids for the chromosomes
		ChadoXML inType = null;
		String line;
		String collectedLines = "";
		Matcher m;
		// Initialize a list of chromosomes from the mapping data
		HashMap<String, String> srcFeatures = new HashMap<String, String>();
		for (MappingData md : mappingData) {
			for (String chr : md.getChromosomes()) {
				srcFeatures.put(chr, "");
			}
		}
		// Find the IDs of <feature> elements for the chromosomes
		System.out.println("Collecting chromosome feature IDs.");
		Pattern namePattern = Pattern.compile("<name>([^<]*)</name>");
		Pattern featureIdPattern = Pattern.compile("<feature id=\"([^\"]*)\"");
		try {
			while ((line = fileReader.readLine()) != null) {
				if (line.contains("<feature id=")) { inType = ChadoXML.FEATURE; }
				if (inType == ChadoXML.FEATURE) {
					collectedLines += line + "\n";
					if (line.contains("</feature>")) {
						m = namePattern.matcher(collectedLines);
						if (m.find()) {
							String chr = m.group(1);
							if (srcFeatures.containsKey(chr)) { // Is this a chromosome feature?
								m = featureIdPattern.matcher(collectedLines);
								if (m.find()) {
									srcFeatures.put(chr, m.group(1));
								} else {
									throw new MappingException("Couldn't find feature ID for chromosome " + chr);
								}
							}
							if (!srcFeatures.containsValue("")) {
								// We've found them all
								break;
							}
						}
						inType = null;
						collectedLines = "";
					}
				}
			}
		} catch (IOException e) {
			throw new MappingException("Couldn't read line from " + chadoXMLFile, e);
		}
		System.out.println("Found: " + srcFeatures.toString());
		
		// If there are chromosomes that aren't referenced, ignore them
		Iterator<String> i = srcFeatures.values().iterator();
		while (i.hasNext()) {
			String value = i.next();
			if (value == "") { i.remove(); }
		}
		// Invert srcFeatures
		{
			HashMap<String, String> tmp = new HashMap<String, String>();
			i = srcFeatures.keySet().iterator();
			while (i.hasNext()) {
				String key = i.next();
				tmp.put(srcFeatures.get(key), key);
			}
			srcFeatures = tmp;
		}
		
		// Find the features that lie on these source features
		try {
			// Rewind
			fileReader.seek(0);
		} catch (IOException e) {
			throw new MappingException("Couldn't return to the beginning of the ChadoXML file", e);
		}
		
		inType = null;
		collectedLines = "";
		Pattern xmlTagPattern = Pattern.compile("<([^ >]+)[^>]*>([^<]*)</\\1>", Pattern.DOTALL);

		Pattern featureLocContentsPattern = Pattern.compile("<featureloc[^>]*>(.*?)</featureloc>", Pattern.DOTALL);
		try {
			while ((line = fileReader.readLine()) != null) {
				if (line.contains("<featureloc>")) { inType = ChadoXML.FEATURELOC; }
				if (inType == ChadoXML.FEATURELOC) {
					collectedLines += line + "\n";
					if (line.contains("</featureloc>")) {
						m = featureLocContentsPattern.matcher(collectedLines);
						if (m.find()) {
							String contents = m.group(1);
							m = xmlTagPattern.matcher(contents);
							HashMap<String, String> info = new HashMap<String, String>();
							while (m.find()) {
								if (m.group(2).equals("")) {
									info.put(m.group(1), null);
								} else {
									info.put(m.group(1), m.group(2));
								}
							}
							if (srcFeatures.containsKey(info.get("srcfeature_id"))) {
								// Build a feature object, map it, and replace collectedLines
								if (info.get("fmin") == null || info.get("fmax") == null) { continue; } // Skip if not located
								if (info.get("strand") == null || info.get("strand").equals("0")) {
									info.put("strand", ".");
								} else if (Integer.parseInt(info.get("strand")) < 0) {
									info.put("strand", "-");
								} else {
									info.put("strand", "+");
								}
								ChadoXMLFeature f = new ChadoXMLFeature(
										info.get("feature_id"),
										info.get("srcfeature_id"),
										srcFeatures.get(info.get("srcfeature_id")),
										Integer.parseInt(info.get("fmin").trim()),
										Integer.parseInt(info.get("fmax").trim()),
										info.get("strand"),
										info.get("phase") == null ? null : Integer.parseInt(info.get("phase"))
										);
								f = (ChadoXMLFeature)updateFeature(f);
								writer.write(f.toString());
							} else {
								writer.write(collectedLines);
							}
						} else {
							writer.write(collectedLines);
						}
						collectedLines = "";
						inType = null;
					}
				} else {
					writer.write(line + "\n");
				}
			}
		} catch (IOException e) {
			throw new MappingException("Couldn't read line from " + chadoXMLFile, e);
		}
		
		try {
			fileReader.close();
			writer.close();
		} catch (IOException e) {
			throw new MappingException("Couldn't close file handles", e);
		}		

		
	}
	
	public class ChadoXMLFeature extends AbstractFeature {
		private ChadoXMLFeature originalFeature;
		protected Integer phase, locgroup, rank;
		protected String featureId, srcFeatureId, residueInfo;
		protected Boolean fminPartial, fmaxPartial;
		public ChadoXMLFeature() {};
		public ChadoXMLFeature(String featureId, String srcFeatureId, String chr, Integer start, Integer end, String strand, Integer phase) {
			this.originalFeature = new ChadoXMLFeature();
			this.originalFeature.featureId = this.featureId = featureId;
			this.originalFeature.srcFeatureId = this.srcFeatureId = srcFeatureId;
			this.originalFeature.chr = this.chr = chr;
			this.originalFeature.start = this.start = start;
			this.originalFeature.end = this.end = end;
			this.originalFeature.strand = this.strand = strand;
			this.originalFeature.phase = this.phase = phase;
		}
		public ChadoXMLFeature(String featureId, String srcFeatureId, String chr, Integer start, Integer end, String strand, Integer phase, Integer rank, Integer locgroup) {
			this(featureId, srcFeatureId, chr, start, end, strand, phase);
			this.originalFeature.rank = this.rank = rank;
			this.originalFeature.locgroup = this.locgroup = locgroup;
		}
		public ChadoXMLFeature(String featureId, String srcFeatureId, String chr, Integer start, Integer end, String strand, Integer phase, Integer rank, Integer locgroup, String residueInfo, Boolean fminPartial, Boolean fmaxPartial) {
			this(featureId, srcFeatureId, chr, start, end, strand, phase, rank, locgroup);
			this.originalFeature.residueInfo = this.residueInfo = residueInfo;
			this.originalFeature.fminPartial = this.fminPartial = fminPartial;
			this.originalFeature.fmaxPartial = this.fmaxPartial = fmaxPartial;			
		}
		
		public String toString() {
			String out = "";
			if (this.originalFeature != null) {
				if (this.changed && isVerbose()) {
					out += "<!--\n" + this.originalFeature.toString() + "-->\n";
					if (indeterminate || dropped)
						return out;
				} else if (isVerbose()) {
					if (this.originalFeature.start != this.start || this.originalFeature.end != this.end) {
						out += "   <!-- this featureloc is shifted right or left, but not changed internally -->\n";
					}
				}
			}
			out += "  <feature_id>" + this.featureId + "</feature_id>\n";
			out += "  <srcfeature_id>" + this.srcFeatureId + "</srcfeature_id>\n";
			
			out += "  <fmin>" + this.start + "</fmin>\n";
			out += "  <fmax>" + this.end + "</fmax>\n";
			if (this.strand != null) {
				String strand = "0";
				if (this.strand.equals("+"))
					strand = "1";
				else if (this.strand.equals("-"))
					strand = "-1";
				out += "  <strand>" + strand + "</strand>\n";
			}
			if (this.phase != null)
				out += "  <phase>" + this.phase + "</phase>\n";
			if (this.rank != null)
				out += "  <rank>" + this.rank + "</rank>\n";
			if (this.locgroup != null)
				out += "  <locgroup>" + this.locgroup + "</locgroup>\n";

			if (this.residueInfo != null)
				out += "  <residue_info>" + this.residueInfo + "</residue_info>\n";
			if (this.fminPartial != null)
				out += "  <is_fmin_partial>" + this.fminPartial + "</is_fmin_partial>\n";
			if (this.fmaxPartial != null)
				out += "  <is_fmax_partial>" + this.fmaxPartial + "</is_fmax_partial>\n";

			
			return "<featureloc>\n" + out + "</featureloc>\n";
		}
	}

}
