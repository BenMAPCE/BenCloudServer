package gov.epa.bencloud.api.function;

/*
 * Provides resources related to a given valuation functional form.
 */
public class EFNative2 implements EFNative{
    public static final String functionalForm = "DELTA * POPULATION * VARIABLE";

    /**
     * Returns the valuation result using the given functional form and arguments.
     */
    @Override
    public double calculate(EFArguments args) {
        return args.deltaQ * args.population * args.v1;
    }
}
