package gov.epa.bencloud.api.function;

import java.util.HashMap;
import java.util.Map;

/*
 * Represents potential arguments used in a given valuation function.
 */
public class VFArguments {
	public double a;
	public double b;
	public double c;
	public double d;
	public double allGoodsIndex;
	public double medicalCostIndex;
	public double wageIndex;
	public Map<String, Double> otherArguments = new HashMap<String, Double>();
}
