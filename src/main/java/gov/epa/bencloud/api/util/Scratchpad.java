package gov.epa.bencloud.api.util;

import gov.epa.bencloud.api.model.HIFConfig;

public class Scratchpad {

	public static void main(String[] args) {
		int startAge = 10;
		int endAge = 14;
		HIFConfig hif = new HIFConfig();
		hif.startAge = 5;
		hif.endAge = 12;

		double dDiv = 1;
		
		if ((startAge >= hif.startAge || hif.startAge == -1) && (endAge <= hif.endAge || hif.endAge == -1)) {
			// The population age range is fully contained in the hif's age range
		}
		else
		{
			// calculate the percentage of the population age range that falls within the hif's age range
			if (startAge < hif.startAge) {
				dDiv = (double)(endAge - hif.startAge + 1) / (double)(endAge - startAge + 1);
				if (endAge > hif.endAge) {
					dDiv = (double)(hif.endAge - hif.startAge + 1) / (double)(endAge - startAge + 1);
				}
			} else if (endAge > hif.endAge) {
				dDiv = (double)(hif.endAge - startAge + 1) / (double)(endAge - startAge + 1);
			}
		}
		
		System.out.println("pop: " + startAge + "-" + endAge + " hif: " + hif.startAge + "-" + hif.endAge + " pop ratio contained in hif: " + dDiv);
	}

}
