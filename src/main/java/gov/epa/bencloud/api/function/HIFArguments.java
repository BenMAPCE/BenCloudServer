package gov.epa.bencloud.api.function;

import java.util.HashMap;
import java.util.Map;

/*
 * Represents potential arguments used in a given HIF.
 */
public class HIFArguments {
    public double a;
    public double b;
    public double c;
    public double beta;
    public double deltaQ;
    public double q0;
    public double q1;
    public double incidence;
    public double prevalence;
    public double population;

    public Map<String, Double> otherArguments = new HashMap<String, Double>();
}
