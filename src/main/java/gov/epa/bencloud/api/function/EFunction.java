package gov.epa.bencloud.api.function;

import org.mariuszgromada.math.mxparser.Expression;

/*
 * The resources related to an exposure function.
 */
public class EFunction {
	public Expression interpretedFunction = null;
	public EFNative nativeFunction = null;
	public EFArguments efArguments = null;
}
