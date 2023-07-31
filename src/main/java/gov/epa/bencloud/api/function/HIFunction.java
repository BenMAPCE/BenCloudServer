package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

import org.mariuszgromada.math.mxparser.Expression;

/*
 * The resources related to a health impact function.
 */
public class HIFunction {
    public Expression interpretedFunction = null;
    public HIFNative nativeFunction = null;
    public HIFArguments hifArguments = null;

    public List<String> getRequiredVariables() {
        List<String> vars = new ArrayList<String>();

        if (interpretedFunction != null) {
            for (String arg : interpretedFunction.getMissingUserDefinedArguments()) {
                vars.add(arg);
            }
        } else {
            // if there are native functions that require variable lookups, we need to handle
            // it here. As of writing this code, there are none.
        }
        
        return vars;
    }
}
