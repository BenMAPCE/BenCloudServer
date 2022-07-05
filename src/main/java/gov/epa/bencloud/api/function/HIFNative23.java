package gov.epa.bencloud.api.function;

public class HIFNative23 implements HIFNative{
   
    public static final String functionalForm = "PREVALENCE*POPULATION";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.prevalence * args.population;
    }
}
