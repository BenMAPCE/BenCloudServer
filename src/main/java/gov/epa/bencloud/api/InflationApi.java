package gov.epa.bencloud.api;

import static gov.epa.bencloud.server.database.jooq.data.Tables.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jooq.JSONFormat;
import org.jooq.Result;
import org.jooq.JSONFormat.RecordFormat;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.pac4j.core.profile.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.epa.bencloud.server.database.JooqUtil;
import spark.Request;
import spark.Response;

public class InflationApi {
	private static final Logger log = LoggerFactory.getLogger(InflationApi.class);
	
	public static Object getAllInflationYears(Response response, Optional<UserProfile> userProfile) {
		return getAllInflationYears(response);
	}
	
	public static Object getAllInflationYears(Response response) {
		//int inflationDatasetId = 4; //currently this is the only available dataset and we don't allow users to select dataset			
			
		Result<Record1<Integer>> records = DSL.using(JooqUtil.getJooqConfiguration())
				.select(INFLATION_ENTRY.ENTRY_YEAR
						)
				.from(INFLATION_ENTRY)
				//.where(INFLATION_ENTRY.INFLATION_DATASET_ID.eq(inflationDatasetId))
				.orderBy(INFLATION_ENTRY.ENTRY_YEAR.desc())
				.fetch();
		
		response.type("application/json");
		return records.formatJSON(new JSONFormat().header(false).recordFormat(RecordFormat.OBJECT));
	}
}
