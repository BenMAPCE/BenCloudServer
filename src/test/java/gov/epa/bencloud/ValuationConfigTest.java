package gov.epa.bencloud;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.model.ValuationConfig;
import io.vavr.collection.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

public class ValuationConfigTest {

    @Test
    public void valuationConfigValidParameters() throws Exception {
        String json = "{\"hif_id\": 2, \"hif_instance_id\": 3, \"vf_id\": 4}";

        ObjectMapper objMapper = new ObjectMapper();
        JsonNode node = objMapper.readTree(json);
        ValuationConfig vf = new ValuationConfig(node);
        assertEquals(2, vf.hifId);
        assertEquals(3, vf.hifInstanceId);
        assertEquals(4, vf.vfId);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidJsons")
    public void valuationConfigInvalidJsons(String json) {
        Exception thrown = assertThrows(NullPointerException.class, () -> {
            ObjectMapper objMapper = new ObjectMapper();
            JsonNode node = objMapper.readTree(json);
            ValuationConfig vf = new ValuationConfig(node);
        }
        );
    }

    private static Stream<Arguments> provideInvalidJsons() {
        return Stream.of(
            Arguments.of("{}"),
            Arguments.of("null"),
            Arguments.of("{\"hif_id\": 2}"),
            Arguments.of("{\"hif_id\": null}"),
            Arguments.of("{\"hif_id\": 2, \"hif_isntance_id\": 3, \"vf_id\": 4}"), /* Typo intentional */
            Arguments.of("[1,5]")
        );
    }
}
