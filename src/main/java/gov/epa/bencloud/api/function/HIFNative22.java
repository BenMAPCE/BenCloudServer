package gov.epa.bencloud.api.function;

public class HIFNative22 implements HIFNative{
   
    public static final String functionalForm = "A*POPULATION*PREVALENCE";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.a * args.population * args.prevalence;
    }
}
