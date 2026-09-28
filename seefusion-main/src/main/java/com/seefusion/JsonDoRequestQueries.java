/*
 * JsonDoRequestQueries.java
 *
 * /json/getrequestqueries?pid=: one request and its queries, for its details. The finished ones it kept
 * (see the queriesPerRequest setting), then the one it is running, if any. Fetched when the details are
 * opened rather than sent with every request list.
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import java.util.List;

public class JsonDoRequestQueries extends JsonRequestHandler {

	@Override
	public JSONObject doJson(HttpTalker talker) {
		String pid = talker.getUrlParams().getProperty("pid");
		if (pid == null) {
			throw new ErrorMessage("You must pass a pid");
		}
		SeeFusion sf = talker.getSeeFusion();
		if (!sf.isGloballyEnabled()) {
			throw new ErrorMessage("SeeFusion is Not Enabled");
		}
		RequestInfo ri = sf.getMasterRequestList().findRequest(pid);
		if (ri == null) {
			throw new ErrorMessage("Request " + pid + " is no longer in the active, recent or slow lists");
		}
		List<QueryRecord> queries = ri.getQueries();
		QueryRecord running = ri.getRunningQuery();
		if (running != null) {
			queries.add(running);
		}
		JSONObject ret = new JSONObject();
		ret.put("pid", pid);
		ret.put("url", ri.getUrl());
		// The request itself, so its details can be shown from the query lists.
		ret.put("request", ri.toJson());
		ret.put("queryCount", ri.getQueryCount());
		ret.put("queriesDropped", ri.getQueriesDropped());
		ret.put("queries", QueryRecord.toJson(queries));
		return ret;
	}

	@Override
	Perm getPerm() {
		return new Perm(Perm.LOGGEDIN);
	}

}
