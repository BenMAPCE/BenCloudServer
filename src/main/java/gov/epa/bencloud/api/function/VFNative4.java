package gov.epa.bencloud.api.function;

public class VFNative4 implements VFNative{
    public static final String functionalForm = "A*MedicalCostIndex+B*((median_income)/(52*5))*WageIndex";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.medicalCostIndex + args.b * (args.medianIncome / (52.0 * 5.0))  * args.wageIndex;
    }
}
