package gov.epa.bencloud.api.function;

import org.mariuszgromada.math.mxparser.Expression;

/*
 * The resources related to a valuation function.
 */
public class VFunction {
    public Expression interpretedFunction = null;
    public VFNative nativeFunction = null;
    public VFArguments vfArguments = null;    
}
