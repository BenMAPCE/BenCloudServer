package gov.epa.bencloud.api.function;

/*
 * Provides resources related to a given valuation functional form.
 */
public class EFNative1 implements EFNative{
    public static final String functionalForm = "DELTA * POPULATION";

    /**
     * Returns the valuation result using the given functional form and arguments.
     */
    @Override
    public double calculate(EFArguments args) {
        return args.deltaQ * args.population;
    }
}
