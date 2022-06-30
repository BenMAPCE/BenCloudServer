package gov.epa.bencloud.api.function;

public class HIFNative2 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/((1-INCIDENCE)*exp(BETA*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION";
    
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / ((1.0 - args.incidence) * Math.exp(args.beta * args.deltaQ) + args.incidence))) * args.incidence * args.population;
    }
}
