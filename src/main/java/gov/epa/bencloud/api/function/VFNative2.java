package gov.epa.bencloud.api.function;

public class VFNative2 implements VFNative{
    public static final String functionalForm = "A*MedicalCostIndex";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.medicalCostIndex;
    }
}
