package gov.epa.bencloud.api.util;

import com.fasterxml.jackson.databind.JsonNode;

/*
 * Methods for json parsing to cover use cases not 
 * in the jackson parser
 */
public class JSONUtil {
    public static Integer getInteger(JsonNode n, String field) {
        return n.has(field) && n.get(field).isInt() ? n.get(field).asInt() : null;
    }
}
