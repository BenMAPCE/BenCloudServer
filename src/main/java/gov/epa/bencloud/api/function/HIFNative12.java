package gov.epa.bencloud.api.function;

public class HIFNative12 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/exp(BETA*B*DELTAQ)))*INCIDENCE*POPULATION*A";
    
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / Math.exp(args.beta * args.B * args.deltaQ))) * args.incidence * args.population * args.A;
    }
}
