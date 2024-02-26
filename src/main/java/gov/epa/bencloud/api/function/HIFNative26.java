package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative26 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/exp(BETA*(((0.720606875550022*ATAN(Q1/36344.7182167901)) + (0.0779352105363792*ATAN(Q1/ 1098.79339780469)) + (0.126896265978495*ATAN(Q1/54.614609748278))+(0.0745616479351036*ATAN(Q1/286.180643354887)))-((0.720606875550022*ATAN(Q0/36344.7182167901)) + (0.0779352105363792*ATAN(Q0/ 1098.79339780469)) + (0.126896265978495*ATAN(Q0/54.614609748278)) + (0.0745616479351036*ATAN(Q0/286.180643354887)))))))*INCIDENCE*POPULATION";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
    	//(1-(1/exp(BETA*(((0.720606875550022*ATAN(Q1/36344.7182167901)) + (0.0779352105363792*ATAN(Q1/ 1098.79339780469)) + (0.126896265978495*ATAN(Q1/54.614609748278))+(0.0745616479351036*ATAN(Q1/286.180643354887)))-((0.720606875550022*ATAN(Q0/36344.7182167901)) + (0.0779352105363792*ATAN(Q0/ 1098.79339780469)) + (0.126896265978495*ATAN(Q0/54.614609748278)) + (0.0745616479351036*ATAN(Q0/286.180643354887)))))))*INCIDENCE*POPULATION
        return (1-(1/Math.exp(args.beta*(((0.720606875550022*Math.atan(args.q1/36344.7182167901)) + (0.0779352105363792*Math.atan(args.q1/ 1098.79339780469)) 
        		+ (0.126896265978495*Math.atan(args.q1/54.614609748278))+(0.0745616479351036*Math.atan(args.q1/286.180643354887)))-((0.720606875550022*Math.atan(args.q0/36344.7182167901)) 
        		+ (0.0779352105363792*Math.atan(args.q0/ 1098.79339780469)) + (0.126896265978495*Math.atan(args.q0/54.614609748278)) + (0.0745616479351036*Math.atan(args.q0/286.180643354887)))))))
        		* args.incidence*args.population;
    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
