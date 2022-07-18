package gov.epa.bencloud.api.function;

public class VFNative9 implements VFNative{
    public static final String functionalForm = "A*WageIndex";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.wageIndex;
    }
}
