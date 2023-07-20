package gov.epa.bencloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.epa.bencloud.api.model.HIFConfig;

import io.vavr.collection.Stream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;



public class HIFConfigTest {
    @Test
    public void HIFConfigValidParameters() throws Exception {
        String json = "{"
                    + "\"id\": 0,"
                    + "\"start_age\": 0,"
                    + "\"end_age\": 0,"
                    + "\"race_id\": 0,"
                    + "\"ethnicity_id\": 0,"
                    + "\"gender_id\": 0,"
                    + "\"incidence_dataset_id\": 0,"
                    + "\"incidence_year\": 0,"
                    + "\"incidence_race\": 0,"
                    + "\"incidence_ethnicity\": 0,"
                    + "\"incidence_gender\": 0,"
                    + "\"prevalence_dataset_id\": 0,"
                    + "\"prevalence_year\": 0,"
                    + "\"prevalence_race\": 0,"
                    + "\"prevalence_ethnicity\": 0,"
                    + "\"prevalence_gender\": 0,"
                    + "\"variable\": 0"
                    + "}";

        ObjectMapper objMapper = new ObjectMapper();
        JsonNode node = objMapper.readTree(json);
        HIFConfig hif = new HIFConfig(node);

        assertTrue(hif.startAge == 0);
        assertTrue(hif.endAge == 0);

        assertTrue(hif.startAge == 0);
        assertTrue(hif.endAge == 0);
        assertTrue(hif.race == 0);
        assertTrue(hif.ethnicity == 0);
        assertTrue(hif.gender == 0);

        assertTrue(hif.incidence == 0);
        assertTrue(hif.incidenceYear == 0);
        assertTrue(hif.incidenceRace == 0);
        assertTrue(hif.incidenceEthnicity == 0);
        assertTrue(hif.incidenceGender == 0);
        
        assertTrue(hif.prevalence == 0);
        assertTrue(hif.prevalenceYear == 0);
        assertTrue(hif.prevalenceRace == 0);
        assertTrue(hif.prevalenceEthnicity == 0);
        assertTrue(hif.prevalenceGender == 0);
        
        assertTrue(hif.variable == 0);
    }


    @ParameterizedTest
    @MethodSource("provideMissingOrWrongTypeData")
    public void HIFConfigMissingOrWrongTypeData(String json) {
        // TODO: Figure out the best way to fail if passsed incorrect json
        Exception thrown = assertThrows(NullPointerException.class, () -> {
            ObjectMapper objMapper = new ObjectMapper();
            JsonNode node = objMapper.readTree(json);
            HIFConfig hif = new HIFConfig(node);
            // Access variables, so there's a null pointer exception if they're null
            // There might be a better way to do this, but for now this works.
            int a = hif.hifId;
            a = hif.startAge;
            a = hif.endAge;
            a = hif.race;
            a = hif.ethnicity;
            a = hif.gender;
            a = hif.incidence;
            a = hif.incidenceYear;
            a = hif.incidenceRace;
            a = hif.incidenceEthnicity;
            a = hif.incidenceGender;
            a = hif.prevalence;
            a = hif.prevalenceYear;
            a = hif.prevalenceRace;
            a = hif.prevalenceEthnicity;
            a = hif.prevalenceGender;
            a = hif.variable;
        });
    }
    public static Stream<Arguments> provideMissingOrWrongTypeData() {
        return Stream.of(
            Arguments.of("{}"),
            Arguments.of("{\"hif_id\": true}"),
            Arguments.of( "{"
                        + "\"id\": 0,"
                        + "\"start_age\": 0,"
                        + "\"end_age\": 0,"
                        + "\"race_id\": 0,"
                        + "\"ethnicity_id\": 0,"
                        + "\"gender_id\": 0,"
                        + "\"incidence_dataset_id\": 0,"
                        + "\"incidence_year\": 0,"
                        + "\"incidence_race\": \"not an int\","
                        + "\"incidence_ethnicity\": 0,"
                        + "\"incidence_gender\": 0,"
                        + "\"prevalence_dataset_id\": 0,"
                        + "\"prevalence_year\": 0,"
                        + "\"prevalence_race\": 0,"
                        + "\"prevalence_ethnicity\": 0,"
                        + "\"prevalence_gender\": 0,"
                        + "\"variable\": 0"
                        + "}")
            );

    }
}
