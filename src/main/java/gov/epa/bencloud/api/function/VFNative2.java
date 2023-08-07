package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given valuation function functional form.
 */
public class VFNative2 implements VFNative{
    public static final String functionalForm = "A*MedicalCostIndex";

    /**
     * Returns the valuation results using the given functional form and arguments.
     */
    @Override
    public double calculate(VFArguments args) {
        return args.a * args.medicalCostIndex;
    }

    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
