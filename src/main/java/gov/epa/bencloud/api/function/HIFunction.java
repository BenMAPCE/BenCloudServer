package gov.epa.bencloud.api.function;

import org.mariuszgromada.math.mxparser.Expression;

/*
 * The resources related to a health impact function.
 */
public class HIFunction {
    public Expression interpretedFunction = null;
    public HIFNative nativeFunction = null;
    public HIFArguments hifArguments = null;
}
