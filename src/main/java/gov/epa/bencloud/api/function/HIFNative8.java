package gov.epa.bencloud.api.function;

public class HIFNative8 implements HIFNative{
      
    public static final String functionalForm = "(1-(1/((1-PREVALENCE)*exp(BETA*A*DELTAQ)+PREVALENCE)))*PREVALENCE*POPULATION";
 
    @Override
    public double calculate(HIFArguments args) {
        return (1.0 - (1.0 / ((1.0 - args.prevalence) * Math.exp(args.beta * args.a * args.deltaQ) + args.prevalence))) * args.prevalence * args.population;
    }
}
