package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative21 implements HIFNative{
   
    public static final String functionalForm = "INCIDENCE*POPULATION*(1-A)";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return args.incidence * args.population * (1.0 - args.a);
    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
