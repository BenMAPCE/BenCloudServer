package gov.epa.bencloud;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.model.ScenarioHIFConfig;
import gov.epa.bencloud.api.model.ValuationConfig;
import io.vavr.collection.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;

public class ScenarioHIFConfigTest {

    @Test
    public void ScenarioHIFConfigValidParameters() throws Exception {
        String json = "{\"prevalence_year\": 2, \"hif_instance_id\": 3, \"incidence_year\": 4}";

        ObjectMapper objMapper = new ObjectMapper();
        JsonNode node = objMapper.readTree(json);
        ScenarioHIFConfig shifc = new ScenarioHIFConfig(node);
        assertEquals(3, shifc.hifInstanceId);
        assertEquals(2, shifc.prevalenceYear);
        assertEquals(4, shifc.incidenceYear );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidJsons")
    public void ScenarioHIFConfigInvalidJsons(String json) {
        Exception thrown = assertThrows(JsonParseException.class, () -> {
            ObjectMapper objMapper = new ObjectMapper();
            JsonNode node = objMapper.readTree(json);
            ValuationConfig vf = new ValuationConfig(node);
        }
        );
    }

    private static Stream<Arguments> provideInvalidJsons() {
        return Stream.of(
            Arguments.of("{[}]"),
            Arguments.of("{"),
            Arguments.of("{asdf}")
        );
    }

    @ParameterizedTest
    @MethodSource("provideMissingOrWrongTypeData")
    public void ScenarioHIFConfigMissingOrWrongTypeData(String json) {
        // TODO: Figure out the best way to fail if passed incorrect json
        Exception thrown = assertThrows(NullPointerException.class, () -> {
            ObjectMapper objMapper = new ObjectMapper();
            JsonNode node = objMapper.readTree(json);
            ScenarioHIFConfig shifc = new ScenarioHIFConfig(node);
            // Access variables, so there's a null pointer exception if they're null
            int a = shifc.hifInstanceId;
            int b = shifc.incidenceYear;
            int c = shifc.prevalenceYear;
        }
        );
    }
    public static Stream<Arguments> provideMissingOrWrongTypeData() {
        return Stream.of(
            Arguments.of("{}"),
            Arguments.of("{\"hif_instance_id\": 1}")
        );
    }
}