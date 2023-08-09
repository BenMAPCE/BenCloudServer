package gov.epa.bencloud.api.function;

import java.util.ArrayList;
import java.util.Arrays;
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
            return nativeFunction.getRequiredVariables();
        }    
    }

    public void createInterpretedFunctionFromExpression(Expression e) {
        e.disableImpliedMultiplicationMode();

        this.requiredExpressionVariables = Arrays.asList(e.getMissingUserDefinedArguments());
        e.defineArguments(e.getMissingUserDefinedArguments());

        this.interpretedFunction = e;
    }
}
