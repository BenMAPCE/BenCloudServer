package gov.epa.bencloud.api.function;

public class VFNative5 implements VFNative{
    public static final String functionalForm = "A*B*AllGoodsIndex";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.b * args.allGoodsIndex;
    }
}
