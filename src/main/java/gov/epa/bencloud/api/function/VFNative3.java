package gov.epa.bencloud.api.function;

public class VFNative3 implements VFNative{
    public static final String functionalForm = "A*MedicalCostIndex*B";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.medicalCostIndex * args.b;
    }
}
