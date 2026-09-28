/*
 * RequestQueriesTest.java
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Test;

/**
 * A request's own query list (its details) and finding a request by pid.
 */
public class RequestQueriesTest {

	private RequestInfo ri;

	private RequestInfo newRequest(SeeFusion sf) {
		ri = new RequestInfo(sf, "server", "/index.cfm", "cbevent=main.index", "127.0.0.1", "GET", null, false);
		return ri;
	}

	@After
	public void tearDown() {
		if (ri != null) {
			// The constructor makes it the thread's current request.
			ri.setRequestInfo(null);
		}
	}

	@Test
	public void testKeepsTheFirstQueriesAndCountsTheRest() {
		RequestInfo request = newRequest(mock(SeeFusion.class));
		for (long i = 0; i < 5; i++) {
			request.addQueryRecord(QueryHistoryTest.query(i), 3);
		}
		List<QueryRecord> queries = request.getQueries();
		assertEquals(3, queries.size());
		assertEquals(0, queries.get(0).elapsedMs);
		assertEquals(2, queries.get(2).elapsedMs);
		assertEquals(2, request.getQueriesDropped());
	}

	@Test
	public void testRecordQueryFeedsTheRequestAndTheHistory() {
		SeeFusion sf = mock(SeeFusion.class);
		QueryHistory history = new QueryHistory();
		when(sf.getQueryHistory()).thenReturn(history);
		RequestInfo request = newRequest(sf);

		QueryInfo qi = new QueryInfo();
		qi.setQueryText("SELECT * FROM t WHERE id = ?");
		qi.parameters = new LinkedList<String>(Arrays.asList("1:[Object/INTEGER]7"));
		qi.datasource = "testds";
		qi.setStartTick(1000);
		request.recordQuery(qi, 25);

		List<QueryRecord> queries = request.getQueries();
		assertEquals(1, queries.size());
		QueryRecord q = queries.get(0);
		assertEquals(request.getRequestKey(), q.requestKey);
		assertEquals("server/index.cfm?cbevent=main.index", q.requestUrl);
		assertSame(q, history.getRecent().get(0));
		assertSame(q, history.getSlowest().get(0));

		JSONObject json = q.toJson();
		assertEquals(request.getRequestKey(), json.getString("pid"));
		assertEquals("SELECT * FROM t WHERE id = ?", json.getString("sql"));
		assertEquals("testds", json.getString("datasource"));
		assertEquals("1:[Object/INTEGER]7", json.getJSONArray("params").getString(0));
		assertEquals(25, json.getLong("elapsed"));
		assertEquals(1000, json.getLong("begin"));
		assertFalse(json.getBoolean("active"));

		// Later reuse of the statement doesn't change what was recorded.
		qi.setQueryText("SELECT 2");
		qi.clearParameters();
		assertEquals("SELECT * FROM t WHERE id = ?", request.getQueries().get(0).sql);
		assertEquals(1, request.getQueries().get(0).params.size());
	}

	@Test
	public void testNothingRecordedWhenAllListsAreOff() {
		SeeFusion sf = mock(SeeFusion.class);
		QueryHistory history = new QueryHistory();
		history.setRecentSize(0);
		history.setSlowestSize(0);
		history.setPerRequestSize(0);
		when(sf.getQueryHistory()).thenReturn(history);
		RequestInfo request = newRequest(sf);
		QueryInfo qi = new QueryInfo();
		qi.setQueryText("SELECT 1");
		request.recordQuery(qi, 1);
		assertEquals(0, request.getQueries().size());
		assertEquals(0, request.getQueriesDropped());
	}

	@Test
	public void testFindRequestInActiveAndHistory() {
		RequestList list = new RequestList();
		try {
			RequestInfo request = newRequest(mock(SeeFusion.class));
			list.createRequest(request);
			assertSame(request, list.findRequest(request.getRequestKey()));
			list.releaseRequest(request);
			assertSame(request, list.findRequest(request.getRequestKey()));
			assertNull(list.findRequest("no-such-pid"));
		} finally {
			list.shutdown();
		}
	}

}
