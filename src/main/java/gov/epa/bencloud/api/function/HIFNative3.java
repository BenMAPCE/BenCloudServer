package gov.epa.bencloud.api.function;

public class HIFNative3 implements HIFNative{
    
    public static final String functionalForm = "(1-(1/((1-A)*exp(BETA*DELTAQ)+A)))*A*POPULATION*PREVALENCE";
   
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / ((1.0 - args.a) * Math.exp(args.beta * args.deltaQ) + args.a))) * args.a * args.incidence * args.population;
    }
}
