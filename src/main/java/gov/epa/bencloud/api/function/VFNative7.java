package gov.epa.bencloud.api.function;

public class VFNative7 implements VFNative{
    public static final String functionalForm = "A*AllGoodsIndex*B";

    @Override
    public double calculate(VFArguments args) {
        return args.a * args.allGoodsIndex + args.b;
    }
}
