package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

/*
 * Provides resources related to a given hif functional form.
 */
public class HIFNative28 implements HIFNative{
   
    public static final String functionalForm = "(1-(1/exp(BETA*(((0.850673892941373*ATAN(Q1/2849.13030520793)) + (0.121439181663039*ATAN(Q1/ 62.1950521861947)) + (9.28850247944535*ATAN(Q1/23.9287615384047))+(-9.26061555404977*ATAN(Q1/23.8921933141567)))-((0.850673892941373*ATAN(Q0/2849.13030520793)) + (0.121439181663039*ATAN(Q0/ 62.1950521861947)) + (9.28850247944535*ATAN(Q0/23.9287615384047)) + (-9.26061555404977*ATAN(Q0/23.8921933141567)))))))*INCIDENCE*POPULATION";
    
    /**
     * Returns the health impact results using the given functional form and arguments.
     */
    @Override
    public double calculate(HIFArguments args) {
    	//(1-(1/exp(BETA*(((0.850673892941373*ATAN(Q1/2849.13030520793)) + (0.121439181663039*ATAN(Q1/ 62.1950521861947)) + (9.28850247944535*ATAN(Q1/23.9287615384047))+(-9.26061555404977*ATAN(Q1/23.8921933141567)))-((0.850673892941373*ATAN(Q0/2849.13030520793)) + (0.121439181663039*ATAN(Q0/ 62.1950521861947)) + (9.28850247944535*ATAN(Q0/23.9287615384047)) + (-9.26061555404977*ATAN(Q0/23.8921933141567)))))))*INCIDENCE*POPULATION
    	
    	return (1-(1/Math.exp(args.beta*(((0.850673892941373*Math.atan(args.q1/2849.13030520793)) + (0.121439181663039*Math.atan(args.q1/ 62.1950521861947)) + 
    			(9.28850247944535*Math.atan(args.q1/23.9287615384047))+(-9.26061555404977*Math.atan(args.q1/23.8921933141567)))-((0.850673892941373*Math.atan(args.q0/2849.13030520793)) + 
    					(0.121439181663039*Math.atan(args.q0/ 62.1950521861947)) + (9.28850247944535*Math.atan(args.q0/23.9287615384047)) + (-9.26061555404977*Math.atan(args.q0/23.8921933141567)))))))
    			* args.incidence*args.population;
    	
    			
    }
     
    @Override
    public List<String> getRequiredVariables() {
        return new ArrayList<String>();
    }
}
