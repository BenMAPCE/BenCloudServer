package gov.epa.bencloud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mariuszgromada.math.mxparser.Expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.epa.bencloud.api.function.VFNative;
import gov.epa.bencloud.api.function.VFNativeFactory;
import gov.epa.bencloud.api.function.VFunction;

public class VFunctionTest {
    

    @ParameterizedTest
    @MethodSource("provideVFNatives")
    void getRequiredVariableNamesNativeFunction(VFNative vfn, List<String> expectedVariables) {
        VFunction vf = new VFunction();
        vf.interpretedFunction = null;
        vf.nativeFunction = vfn;
        vf.vfArguments = null;

        for (String expectedVariable : expectedVariables) {
            assertTrue(vf.getRequiredVariables().contains(expectedVariable));
        }
    }


    static Stream<Arguments> provideVFNatives() {
        return Stream.of(
            Arguments.of(VFNativeFactory.create("A*B*AllGoodsIndex"), new ArrayList<String>()),
            Arguments.of(VFNativeFactory.create("A*MedicalCostIndex+B*((median_income)/(52*5))*WageIndex"), Arrays.asList("median_income"))
        );
    }

    @ParameterizedTest
    @MethodSource("provideVFInterpreteds")
    void getRequiredVariableNamesInterpretedFunction(Expression expr, List<String> expectedVariables) {
        VFunction vf = new VFunction();
        vf.createInterpretedFunctionFromExpression(expr);

        for (String expectedVariable : expectedVariables) {
            assertTrue(vf.getRequiredVariables().contains(expectedVariable));
        }
    }

    static Stream<Arguments> provideVFInterpreteds() {
        Expression partiallySetExpression = new Expression("A*median_income");
        partiallySetExpression.setArgumentValue("A", 0.0);

        return Stream.of(
            Arguments.of(new Expression("median_income"), Arrays.asList("median_income")),
            Arguments.of(new Expression("median_income*my_var"), Arrays.asList("median_income", "my_var")),
            Arguments.of(partiallySetExpression, Arrays.asList("median_income"))
        );
    }
}
