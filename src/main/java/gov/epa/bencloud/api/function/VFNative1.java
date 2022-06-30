package gov.epa.bencloud.api.function;

public class VFNative1 implements VFNative{
    public static final String functionalForm = "A*MedicalCostIndex+B*WageIndex";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.medicalCostIndex + args.b * args.wageIndex;
    }
}
