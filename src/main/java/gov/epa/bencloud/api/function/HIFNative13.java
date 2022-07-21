package gov.epa.bencloud.api.function;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative13 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/exp(BETA*DELTAQ)))*A*POPULATION";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / Math.exp(args.beta * args.deltaQ))) * args.a * args.population;
    }
}
