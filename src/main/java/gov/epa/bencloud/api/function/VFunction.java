package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mariuszgromada.math.mxparser.Expression;

/*
 * The resources related to a valuation function.
 */
public class VFunction {
    public Expression interpretedFunction = null;
    public VFNative nativeFunction = null;
    public VFArguments vfArguments = null;    

    public List<String> requiredExpressionArguments = new ArrayList<String>();

    public List<String> getRequiredVariables() {

        if (interpretedFunction != null) {
            return requiredExpressionArguments;
        } else if (nativeFunction != null) {
            return nativeFunction.getRequiredVariables();
        }

        return null;
    }

    public void createInterpretedFunctionFromExpression(Expression e) {
        e.disableImpliedMultiplicationMode(); // This is necessary to avoid situations like "median_income" being interpreted as "m*e*dian_incom*e".

        // We need to get list of the required arguments before we define them in the expression, 
        // since otherwise we can't differentiate between the predefined args like AllGoodsIndex, and other args like median_income.
        this.requiredExpressionArguments = Arrays.asList(e.getMissingUserDefinedArguments()); 
        e.defineArguments(e.getMissingUserDefinedArguments()); 

        this.interpretedFunction = e;
    }
}
