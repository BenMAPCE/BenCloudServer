package gov.epa.bencloud.api.function;

public class HIFNative24 implements HIFNative{
   
    public static final String functionalForm = "A*POPULATION";
    
    @Override
    public double calculate(HIFArguments args) {
        return args.a * args.population;
    }
}
