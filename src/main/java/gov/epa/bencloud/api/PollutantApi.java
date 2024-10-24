package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.util.Optional;

import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.JooqUtil;
import spark.Request;
import spark.Response;

/*
 * Methods related to pollutant data.
 */
public class PollutantApi {
	private static final Logger log = LoggerFactory.getLogger(PollutantApi.class);

	/**
	 * 
	 * @param request
	 * @param response
	 * @param userProfile
	 * @return a JSON representation of all pollutant definitions.
	 * 
	 */
	public static Object getAllPollutantDefinitions(Request request, Response response, Optional<UserProfile> userProfile) {
		Result<Record> aqRecords = DSL.using(JooqUtil.getJooqConfiguration())
				.select(POLLUTANT.asterisk())
				.from(POLLUTANT)
				.orderBy(POLLUTANT.NAME)
				.fetch();
		//log.debug("Requested all pollutants: " + userProfile.get().getId());
		response.type("application/json");
		return aqRecords.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
	
	public static String getPollutantName(Integer id) {
		return DSL.using(JooqUtil.getJooqConfiguration())
		.select(
				POLLUTANT.NAME)
		.from(POLLUTANT)
		.where(POLLUTANT.ID.eq(id))
		.fetchOne().value1();	
	}
}
