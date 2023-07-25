package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.List;

import org.mariuszgromada.math.mxparser.Expression;

/*
 * The resources related to a valuation function.
 */
public class VFunction {
    public Expression interpretedFunction = null;
    public VFNative nativeFunction = null;
    public VFArguments vfArguments = null;    

    public List<String> getRequiredVariables() {
        List<String> vars = new ArrayList<String>();

        if (interpretedFunction != null) {
            for (String arg : interpretedFunction.getMissingUserDefinedArguments()) {
                vars.add(arg);
            }
        } else if (nativeFunction != null) {
            vars = nativeFunction.getRequiredVariables();
        }

        return vars;
    }
}
