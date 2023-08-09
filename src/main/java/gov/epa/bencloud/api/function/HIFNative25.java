package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative25 implements HIFNative{
   
    public static final String functionalForm = "INCIDENCE*POPULATION*A*B";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return args.incidence * args.population * args.a * args.b;
    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
