package gov.epa.bencloud.api.function;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative3 implements HIFNative{
    
    public static final String functionalForm = "(1-(1/((1-A)*exp(BETA*DELTAQ)+A)))*A*POPULATION*PREVALENCE";
   
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / ((1.0 - args.a) * Math.exp(args.beta * args.deltaQ) + args.a))) * args.a * args.prevalence * args.population;
    }
}
