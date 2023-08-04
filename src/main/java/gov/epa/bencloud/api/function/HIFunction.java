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

    public List<String> requiredExpressionVariables;

    public List<String> getRequiredVariables() {

        if (interpretedFunction != null) {
            return requiredExpressionVariables;
        } else {
            // if there are native functions that require variable lookups, we need to handle
            // it here. As of writing this code, there are none, so we return an empty array
            return new ArrayList<String>();
        }    
    }
}
