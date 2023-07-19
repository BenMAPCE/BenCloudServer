package gov.epa.bencloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.model.ScenarioHIFConfig;
import gov.epa.bencloud.api.model.ScenarioPopConfig;
import io.vavr.collection.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;

public class ScenarioPopConfigTest {

    @Test
    public void ScenarioPopConfigValidParameters() throws Exception {
        String json = "{"
                    + "\"hif_configs\":"
                        + "[{"
                        + "\"hif_instance_id\": 0,"
                        + "\"incidence_year\": 0,"
                        + "\"prevalence_year\": 0"
                        + "}],"
                    + "\"population_year\": 2010"
                    + "}";

        
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode node = objMapper.readTree(json);
        ScenarioPopConfig sp = new ScenarioPopConfig(node);

        ScenarioHIFConfig shif = sp.scenarioHifConfigs.get(0);

        assertEquals(0, shif.hifInstanceId);
        assertEquals(0, shif.incidenceYear);
        assertEquals(0, shif.prevalenceYear);
    }


    @Test
    public void ScenarioPopConfigEmptyHIFConfigsArray() throws Exception {
        String json = "{"
                    + "\"hif_configs\": [],"
                    + "\"population_year\": 2010"
                    + "}";

        
        ObjectMapper objMapper = new ObjectMapper();
        JsonNode node = objMapper.readTree(json);
        ScenarioPopConfig sp = new ScenarioPopConfig(node);

        assertThrows(IndexOutOfBoundsException.class, () -> {
            ScenarioHIFConfig shifc = sp.scenarioHifConfigs.get(0);
        });
    }



    @ParameterizedTest
    @MethodSource("provideInvalidJsons")
    public void ScenarioPopConfigInvalidJson(String json) {
        Exception thrown = assertThrows(JsonParseException.class, () -> {
            ObjectMapper objMapper = new ObjectMapper();
            JsonNode node = objMapper.readTree(json);
            ScenarioPopConfig sp = new ScenarioPopConfig(node);
        });
    }

    private static Stream<Arguments> provideInvalidJsons() {
        return Stream.of(
            Arguments.of("{"),
            Arguments.of("aaaa"),
            Arguments.of("{"
                        + "\"hif_configs\": [{],"
                        + "\"population_year\": 2010"
                        + "}")

        );
    }


}