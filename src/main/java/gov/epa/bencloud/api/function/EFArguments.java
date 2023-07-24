package gov.epa.bencloud.api.function;

/*
 * Represents potential arguments used in a given Exposure Function.
 */
public class EFArguments {
    public double deltaQ;
    public double q0; //post-policy scenario
    public double q1; //baseline scenario
    public double population;
    public double v1; //variable 1
}
