package gov.epa.bencloud;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import gov.epa.bencloud.api.function.HIFNative;
import gov.epa.bencloud.api.function.HIFNativeFactory;
import gov.epa.bencloud.api.function.HIFArguments;


public class HIFNativeFactoryTest {
    
    @ParameterizedTest()
    @MethodSource("provideHIFNativeArgs")
    void TestHIFNativeFactory(String functionalForm, HIFArguments args, double expectedResult) {
        HIFNative hif = HIFNativeFactory.create(functionalForm);
        double actualResult = hif.calculate(args);

        // last argument is the acceptable delta for a double comparison
        assertEquals(expectedResult, actualResult, 0.001);
    }

    private static Stream<Arguments> provideHIFNativeArgs() {
        // We use the same argument values for each VF test case to simplify things.
        // I chose prime numbers here just because it's easier to tell what's been multiplied together.
        double a = 2.0, b = 3.0, c = 5.0, beta = 7.0,
               deltaQ = 11.0, q0 = 13.0, q1 = 17.0, 
               incidence = 19.0, prevalence = 23.0,
               population = 29.0;
        HIFArguments sampleHifArgs = new HIFArguments();
        sampleHifArgs.a = a;
        sampleHifArgs.b = b;
        sampleHifArgs.c = c;
        sampleHifArgs.beta = beta;
        sampleHifArgs.deltaQ = deltaQ;
        sampleHifArgs.q0 = q0;
        sampleHifArgs.q1 = q1;
        sampleHifArgs.incidence = incidence;
        sampleHifArgs.prevalence = prevalence;
        sampleHifArgs.population = population;

        return Stream.of(
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*INCIDENCE*POPULATION", sampleHifArgs, (1 - (1.0 / Math.exp(beta*deltaQ)))*incidence*population),
            Arguments.of("(1-(1/((1-A)*exp(BETA*DELTAQ)+A)))*A*POPULATION*PREVALENCE", sampleHifArgs, (1-(1/((1-a)*Math.exp(beta*deltaQ)+a)))*a*population*prevalence),
            Arguments.of("(1-(1/((1-INCIDENCE)*exp(BETA*A*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION", sampleHifArgs, (1-(1/((1-incidence)*Math.exp(beta*a*deltaQ)+incidence)))*incidence*population),
            Arguments.of("(1-(1/((1-INCIDENCE)*exp(BETA*B*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION*A", sampleHifArgs, (1-(1/((1-incidence)*Math.exp(beta*b*deltaQ)+incidence)))*incidence*population*a),
            Arguments.of("(1-(1/((1-INCIDENCE)*exp(BETA*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION", sampleHifArgs, (1-(1/((1-incidence)*Math.exp(beta*deltaQ)+incidence)))*incidence*population),
            Arguments.of("(1-(1/((1-INCIDENCE)*exp(BETA*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION*(1-A)", sampleHifArgs, (1-(1/((1-incidence)*Math.exp(beta*deltaQ)+incidence)))*incidence*population*(1-a)),
            Arguments.of("(1-(1/((1-INCIDENCE)*exp(BETA*DELTAQ)+INCIDENCE)))*INCIDENCE*POPULATION*A", sampleHifArgs, (1-(1/((1-incidence)*Math.exp(beta*deltaQ)+incidence)))*incidence*population*a),
            Arguments.of("(1-(1/((1-PREVALENCE)*exp(BETA*A*DELTAQ)+PREVALENCE)))*PREVALENCE*POPULATION", sampleHifArgs, (1-(1/((1-prevalence)*Math.exp(beta*a*deltaQ)+prevalence)))*prevalence*population),
            Arguments.of("(1-(1/((1-PREVALENCE)*exp(BETA*DELTAQ)+PREVALENCE)))*PREVALENCE*POPULATION", sampleHifArgs, (1-(1/((1-prevalence)*Math.exp(beta*deltaQ)+prevalence)))*prevalence*population),
            Arguments.of("(1-(1/exp(BETA*A*DELTAQ)))*INCIDENCE*POPULATION", sampleHifArgs, (1-(1/Math.exp(beta*a*deltaQ)))*incidence*population),
            Arguments.of("(1-(1/exp(BETA*B*DELTAQ)))*A*POPULATION", sampleHifArgs, (1-(1/Math.exp(beta*b*deltaQ)))*a*population),
            Arguments.of("(1-(1/exp(BETA*B*DELTAQ)))*INCIDENCE*POPULATION*A", sampleHifArgs, (1-(1/Math.exp(beta*b*deltaQ)))*incidence*population*a),
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*A*POPULATION", sampleHifArgs, (1-(1/Math.exp(beta*deltaQ)))*a*population),
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*A*POPULATION*PREVALENCE", sampleHifArgs, (1-(1/Math.exp(beta*deltaQ)))*a*population*prevalence),
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*INCIDENCE*POPULATION", sampleHifArgs, (1-(1/Math.exp(beta*deltaQ)))*incidence*population),
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*INCIDENCE*POPULATION*(1-A)", sampleHifArgs, (1-(1/Math.exp(beta*deltaQ)))*incidence*population*(1-a)),
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*INCIDENCE*POPULATION*A", sampleHifArgs, (1-(1/Math.exp(beta*deltaQ)))*incidence*population*a),
            Arguments.of("(1-(1/exp(BETA*DELTAQ)))*INCIDENCE*POPULATION*A*B", sampleHifArgs, (1-(1/Math.exp(beta*deltaQ)))*incidence*population*a*b),
            Arguments.of("(1-exp(-BETA*DELTAQ))*INCIDENCE*POPULATION", sampleHifArgs, (1-Math.exp(-beta*deltaQ))*incidence*population)
        );
    }

}
