package gov.epa.bencloud.api.function;

import java.util.List;

/*
 * Interface implemented for HIFNative classes.
 */
public interface HIFNative {
    public double calculate(HIFArguments args);

    public List<String> getRequiredVariables();
}
