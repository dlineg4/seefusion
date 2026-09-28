/*
 * QueryRecord.java
 *
 * One query as shown in the query lists and in a request's details: a copy of what its QueryInfo knew
 * when it finished (or, for a running query, when it was looked at). It keeps no reference to the
 * statement, result set or request, so query history doesn't keep those in memory.
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class QueryRecord {

	private static final AtomicLong nextId = new AtomicLong();

	final String id;
	// The request that ran the query (its pid) and its URL; null and "" for queries outside a request.
	final String requestKey;
	final String requestUrl;
	final String datasource;
	final String sql;
	final List<String> params;
	final String exception;
	final long elapsedMs;
	final int rows;
	final long beginTime;
	// When the query finished; 0 while it runs.
	final long completedTime;

	QueryRecord(String requestKey, String requestUrl, String datasource, String sql, List<String> params,
			String exception, long elapsedMs, int rows, long beginTime, long completedTime) {
		this.id = Long.toString(nextId.incrementAndGet());
		this.requestKey = requestKey;
		this.requestUrl = requestUrl == null ? "" : requestUrl;
		this.datasource = datasource == null ? "" : datasource;
		this.sql = sql == null ? "" : sql;
		this.params = params == null ? Collections.<String>emptyList() : params;
		this.exception = exception;
		this.elapsedMs = elapsedMs;
		this.rows = rows;
		this.beginTime = beginTime;
		this.completedTime = completedTime;
	}

	/**
	 * A query that just finished, on the thread that ran it. elapsedMs includes reading its result set.
	 */
	static QueryRecord finished(QueryInfo qi, long elapsedMs, RequestInfo ri) {
		return new QueryRecord(ri == null ? null : ri.getRequestKey(), ri == null ? "" : ri.getUrl(),
				qi.getDatasource(), qi.queryText, copyParams(qi), qi.exceptionText, elapsedMs, qi.getResultCount(),
				qi.getBeginTime(), System.currentTimeMillis());
	}

	/**
	 * A query that is still running, looked at from another thread.
	 */
	static QueryRecord running(QueryInfo qi, RequestInfo ri) {
		List<String> params;
		try {
			params = copyParams(qi);
		} catch (RuntimeException e) {
			// The request's thread may be changing the parameters right now; show none rather than fail.
			params = null;
		}
		return new QueryRecord(ri == null ? null : ri.getRequestKey(), ri == null ? "" : ri.getUrl(),
				qi.getDatasource(), qi.queryText, params, qi.exceptionText, qi.getElapsedTime(), qi.getResultCount(),
				qi.getBeginTime(), 0);
	}

	private static List<String> copyParams(QueryInfo qi) {
		return qi.parameters == null ? null : new ArrayList<String>(qi.parameters);
	}

	boolean isActive() {
		return completedTime == 0;
	}

	JSONObject toJson() {
		JSONObject ret = new JSONObject();
		ret.put("id", id);
		ret.put("pid", requestKey == null ? "" : requestKey);
		ret.put("url", requestUrl);
		ret.put("datasource", datasource);
		ret.put("sql", sql);
		JSONArray paramArray = new JSONArray();
		for (String param : params) {
			paramArray.put(param);
		}
		ret.put("params", paramArray);
		ret.put("exception", exception == null ? "" : exception);
		ret.put("elapsed", elapsedMs);
		ret.put("rows", rows);
		ret.put("begin", beginTime);
		ret.put("active", isActive());
		if (isActive()) {
			ret.put("completed", "");
			ret.put("completedAgoMs", "");
		} else {
			ret.put("completed", completedTime);
			ret.put("completedAgoMs", System.currentTimeMillis() - completedTime);
		}
		return ret;
	}

	static JSONArray toJson(Collection<QueryRecord> queries) {
		JSONArray ret = new JSONArray();
		for (QueryRecord query : queries) {
			ret.put(query.toJson());
		}
		return ret;
	}

}
