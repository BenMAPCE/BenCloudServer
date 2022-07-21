package gov.epa.bencloud.api.function;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative22 implements HIFNative{
   
    public static final String functionalForm = "A*POPULATION*PREVALENCE";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return args.a * args.population * args.prevalence;
    }
}
