package gov.epa.bencloud.api.function;

public class VFNative8 implements VFNative{
    public static final String functionalForm = "((median_income)/(52*5))*WageIndex";

    @Override
    public double calculate(VFArguments args) {
        return ((args.medianIncome)/(52.0*5.0))*args.wageIndex;
    }
}
