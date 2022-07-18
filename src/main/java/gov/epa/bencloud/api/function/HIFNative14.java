package gov.epa.bencloud.api.function;

public class HIFNative14 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/exp(BETA*DELTAQ)))*A*POPULATION*PREVALENCE";
    
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / Math.exp(args.beta * args.deltaQ))) * args.a * args.population * args.prevalence;
    }
}
