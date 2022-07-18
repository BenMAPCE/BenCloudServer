package gov.epa.bencloud.api.function;

public class VFNative6 implements VFNative{
    public static final String functionalForm = "A*AllGoodsIndex";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.allGoodsIndex;
    }
}
