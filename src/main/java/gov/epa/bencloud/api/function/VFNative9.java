package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given valuation function functional form.
 */
public class VFNative9 implements VFNative{
    public static final String functionalForm = "A*WageIndex";

    /**
     * Returns the valuation results using the given functional form and arguments.
     */
    @Override
    public double calculate(VFArguments args) {
        return args.a * args.wageIndex;
    }
    
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
