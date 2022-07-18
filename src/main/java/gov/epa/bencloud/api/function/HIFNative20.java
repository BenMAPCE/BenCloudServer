package gov.epa.bencloud.api.function;

public class HIFNative20 implements HIFNative{
   
    public static final String functionalForm = "INCIDENCE*POPULATION*A";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.incidence * args.population * args.a;
    }
}
