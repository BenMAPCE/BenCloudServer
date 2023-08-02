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

    public List<String> requiredExpressionArguments;

    public List<String> getRequiredVariables() {

        if (interpretedFunction != null) {
            return requiredExpressionArguments;
        } else if (nativeFunction != null) {
            return nativeFunction.getRequiredVariables();
        }

        return null;
    }
}
