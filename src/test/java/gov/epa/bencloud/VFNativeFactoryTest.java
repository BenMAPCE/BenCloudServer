package gov.epa.bencloud;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import gov.epa.bencloud.api.function.VFNative;
import gov.epa.bencloud.api.function.VFNativeFactory;
import gov.epa.bencloud.api.function.VFArguments;

public class VFNativeFactoryTest {
    
    @DisplayName("VFNative factory should calculate using correct formula")
    @ParameterizedTest(name = "{0} test")
    @MethodSource("provideVFNativeArgs")
    void TestVFNativeFactory(String functionalForm, VFArguments args, double expectedResult) {
        VFNative vf = VFNativeFactory.create(functionalForm);
        double actualResult = vf.calculate(args);

        // last argument is the acceptable delta for a double comparison
        assertEquals(expectedResult, actualResult, 0.001);
    }

    private static Stream<Arguments> provideVFNativeArgs() {
        // We use the same argument values for each VF test case to simplify things.
        // I chose prime numbers here just because it's easier to tell what's been multiplied together.
        double a = 2.0, b = 3.0, c = 5.0, d = 7.0, allGoodsIndex = 11.0, 
               medicalCostIndex = 13.0, wageIndex = 17.0, medianIncome = 19.0;
        VFArguments sampleVfArgs = new VFArguments();

        sampleVfArgs.a = a;
        sampleVfArgs.b = b;
        sampleVfArgs.c = c;
        sampleVfArgs.d = d;
        sampleVfArgs.allGoodsIndex = allGoodsIndex;
        sampleVfArgs.medicalCostIndex = medicalCostIndex;
        sampleVfArgs.wageIndex = wageIndex;
        sampleVfArgs.medianIncome = medianIncome;

        return Stream.of(
            Arguments.of("A*MedicalCostIndex+B*WageIndex", sampleVfArgs, a*medicalCostIndex+b*wageIndex),
            Arguments.of("A*MedicalCostIndex", sampleVfArgs, a*medicalCostIndex),
            Arguments.of("A*MedicalCostIndex*B", sampleVfArgs, a*medicalCostIndex*b),
            Arguments.of("A*MedicalCostIndex+B*((median_income)/(52*5))*WageIndex", sampleVfArgs, a*medicalCostIndex+b*(medianIncome/(52.0*5))*wageIndex),
            Arguments.of("A*B*AllGoodsIndex", sampleVfArgs, a*b*allGoodsIndex),
            Arguments.of("A*AllGoodsIndex", sampleVfArgs, a*allGoodsIndex),
            Arguments.of("A*AllGoodsIndex*B", sampleVfArgs, a*allGoodsIndex*b),
            Arguments.of("((median_income)/(52*5))*WageIndex", sampleVfArgs, medianIncome/(52*5)*wageIndex),
            Arguments.of("A*WageIndex", sampleVfArgs, a*wageIndex)
        );
    }

}
