package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative5 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/((1-INCIDENCE)*exp(BETA*B*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION*A";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / ((1.0 - args.incidence) * Math.exp(args.beta * args.b * args.deltaQ) + args.incidence))) * args.incidence * args.population * args.a;
    }
    
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }

}
