package gov.epa.bencloud.api.function;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative18 implements HIFNative{
   
    public static final String functionalForm = "(1-exp(-BETA*DELTAQ))*INCIDENCE*POPULATION";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - Math.exp( (-1.0 * args.beta) * args.deltaQ)) * args.incidence * args.population;
    }
}
