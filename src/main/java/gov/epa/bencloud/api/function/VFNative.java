package gov.epa.bencloud.api.function;

import java.util.List;

/*
 * Interface implemented for VFNative classes.
 */
public interface VFNative {
    public double calculate(VFArguments args);

    public abstract List<String> getRequiredVariables();
}