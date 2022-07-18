package gov.epa.bencloud.api.function;

public class HIFNative19 implements HIFNative{
   
    public static final String functionalForm = "INCIDENCE*POPULATION";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.incidence * args.population;
    }
}
