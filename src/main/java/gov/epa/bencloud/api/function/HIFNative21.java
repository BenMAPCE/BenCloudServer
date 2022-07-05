package gov.epa.bencloud.api.function;

public class HIFNative21 implements HIFNative{
   
    public static final String functionalForm = "INCIDENCE*POPULATION*(1-A)";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.incidence * args.population * (1.0 - args.a);
    }
}
