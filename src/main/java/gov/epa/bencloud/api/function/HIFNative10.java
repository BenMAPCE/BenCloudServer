package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative10 implements HIFNative{

    public static final String functionalForm = "(1-(1/exp(BETA*A*DELTAQ)))*INCIDENCE*POPULATION";

    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / Math.exp(args.beta * args.a * args.deltaQ))) * args.incidence * args.population;
    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
