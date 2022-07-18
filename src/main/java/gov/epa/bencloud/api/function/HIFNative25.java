package gov.epa.bencloud.api.function;

public class HIFNative25 implements HIFNative{
   
    public static final String functionalForm = "INCIDENCE*POPULATION*A*B";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.incidence * args.population * args.a * args.b;
    }
}
