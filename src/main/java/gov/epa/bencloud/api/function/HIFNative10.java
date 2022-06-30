package gov.epa.bencloud.api.function;

public class HIFNative10 implements HIFNative{

    public static final String functionalForm = "(1-(1/exp(BETA*A*DELTAQ)))*INCIDENCE*POPULATION";

    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / Math.exp(args.beta * args.A * args.deltaQ))) * args.incidence * args.population;
    }
}
