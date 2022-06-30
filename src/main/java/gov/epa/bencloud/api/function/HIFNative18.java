package gov.epa.bencloud.api.function;

public class HIFNative18 implements HIFNative{
   
    public static final String functionalForm = "(1-exp(-BETA*DELTAQ))*INCIDENCE*POPULATION";
    
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - Math.exp( (-1.0 * args.beta) * args.deltaQ)) * args.incidence * args.population;
    }
}
