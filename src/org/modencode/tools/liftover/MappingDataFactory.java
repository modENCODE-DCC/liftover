package org.modencode.tools.liftover;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MappingDataFactory {
	
	public final static String RELEASE_FILE_TEMPLATE = "CHROMOSOME_DIFFERENCES/sequence_differences.WS%d";

	public static List<MappingData> generateMappings(int startRelease, int destinationRelease) throws MappingException {
		List<MappingData> mappings;
		if (startRelease > destinationRelease) {
			mappings =  doGenerateMappings(destinationRelease, startRelease);
			// Invert mapping information
			for (MappingData md : mappings) {
				for (String chr : md.getChromosomes()) {
					for (MappingData.MismatchPair mp : md.getMismatchPairs(chr)) {
						MappingData.MismatchCoords tmp = mp.previousMismatch;
						mp.previousMismatch = mp.thisMismatch;
						mp.thisMismatch = tmp;
					}
				}
			}
			List<MappingData> reverseList = new ArrayList<MappingData>();
			for (int i = mappings.size()-1; i >= 0; i--) {
				reverseList.add(mappings.get(i));
			}
			mappings = reverseList;
		} else {
			mappings =  doGenerateMappings(startRelease, destinationRelease);
		}
		return mappings;
	}
	private static List<MappingData> doGenerateMappings(int startRelease, int destinationRelease) throws MappingException {
		ArrayList<MappingData> allMappingData = new ArrayList<MappingData>();
		
		for (int curRelease = startRelease+1; curRelease <= destinationRelease; curRelease++) {
			File differencesFile = new File(String.format(RELEASE_FILE_TEMPLATE, curRelease));
			BufferedReader reader;
			try {
				FileReader fileReader = new FileReader(differencesFile);
				reader = new BufferedReader(fileReader);
			} catch (FileNotFoundException e) {
				System.err.println("Couldn't open " + differencesFile);
				throw new MappingException("Couldn't open " + differencesFile, e);
			}
			
			MappingData md = new MappingData(curRelease);
			try {
				String currentChromosome = null;
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (line.matches("^\\s*#")) continue; // Comments
					if (line.matches("^\\s*$")) continue; // Blank lines
					if (line.startsWith("Chromosome: ")) {
						// Set the chromosome
						currentChromosome = line.substring(12);
					} else {
						String[] fields = line.split("\t");
						boolean flipped = false;
						if (Integer.parseInt(fields[6]) > 0) { flipped = true; }
						
						md.addMismatchPair(currentChromosome,
								Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2]),
								Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
								flipped);
					}
				}
				reader.close();
			} catch (IOException e) {
				System.err.println("Couldn't read line from " + differencesFile);
				throw new MappingException("Couldn't read line from " + differencesFile, e);
			}
			allMappingData.add(md);
		}
		
		
		
		return allMappingData;
	}

}
