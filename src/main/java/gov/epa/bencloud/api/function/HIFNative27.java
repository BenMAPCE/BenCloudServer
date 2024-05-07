package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative27 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/exp(BETA*(LOG(Max(0,Q1-2.4)/1.6+1)/(1+exp(-(Max(0,Q1-2.4)-(15.5))/36.8))-(LOG(Max(0,Q0-2.4)/1.6+1)/(1+exp(-(Max(0,Q0-2.4)-(15.5))/36.8)))))))*INCIDENCE*POPULATION";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
    	//(1-(1/exp(BETA*(LOG(Max(0,Q1-2.4)/1.6+1)/(1+exp(-(Max(0,Q1-2.4)-(15.5))/36.8))-(LOG(Max(0,Q0-2.4)/1.6+1)/(1+exp(-(Max(0,Q0-2.4)-(15.5))/36.8)))))))*INCIDENCE*POPULATION
    	return (1-(1/Math.exp(args.beta*(Math.log(Math.max(0,args.q1-2.4)/1.6+1)/(1+Math.exp(-(Math.max(0,args.q1-2.4)-(15.5))/36.8))-(Math.log(Math.max(0,args.q0-2.4)/1.6+1)/(1+Math.exp(-(Math.max(0,args.q0-2.4)-(15.5))/36.8)))))))
    			* args.incidence*args.population;

    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
