/*
 * JsonDoQueryList.java
 *
 * /json/getqueries?type=active|recent|slowest: the queries running now, the most recent finished
 * queries, or the slowest finished queries since SeeFusion started.
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import java.util.List;

public class JsonDoQueryList extends JsonRequestHandler {

	@Override
	public JSONObject doJson(HttpTalker talker) {
		String type = talker.getUrlParams().getProperty("type");
		if (type == null) {
			throw new ErrorMessage("You must pass a type [active, recent, slowest]");
		}
		SeeFusion sf = talker.getSeeFusion();
		if (!sf.isGloballyEnabled()) {
			throw new ErrorMessage("SeeFusion is Not Enabled");
		}
		List<QueryRecord> queries;
		if (type.equalsIgnoreCase("active")) {
			queries = sf.getMasterRequestList().getRunningQueries();
		} else if (type.equalsIgnoreCase("recent")) {
			queries = sf.getQueryHistory().getRecent();
		} else if (type.equalsIgnoreCase("slowest")) {
			queries = sf.getQueryHistory().getSlowest();
		} else {
			throw new ErrorMessage("Unknown type [" + type + "]; use active, recent or slowest");
		}
		JSONObject ret = new JSONObject();
		ret.put("queries", QueryRecord.toJson(queries));
		return ret;
	}

	@Override
	Perm getPerm() {
		return new Perm(Perm.LOGGEDIN);
	}

}
