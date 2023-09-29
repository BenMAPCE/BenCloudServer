package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative24 implements HIFNative{
   
    public static final String functionalForm = "A*POPULATION";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return args.a * args.population;
    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
