/*
 * QueryHistoryTest.java
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class QueryHistoryTest {

	static QueryRecord query(long elapsedMs) {
		return new QueryRecord("1", "server/page.cfm", "ds", "SELECT " + elapsedMs, null, null, elapsedMs, 0, 0, 1);
	}

	private static List<Long> elapsed(List<QueryRecord> queries) {
		List<Long> ret = new ArrayList<Long>();
		for (QueryRecord q : queries) {
			ret.add(q.elapsedMs);
		}
		return ret;
	}

	@Test
	public void testRecentKeepsTheNewestFirst() {
		QueryHistory history = new QueryHistory();
		history.setRecentSize(3);
		for (long i = 1; i <= 5; i++) {
			history.add(query(i));
		}
		assertEquals(Arrays.asList(5L, 4L, 3L), elapsed(history.getRecent()));
	}

	@Test
	public void testSlowestKeepsTheLongestSinceStart() {
		QueryHistory history = new QueryHistory();
		history.setSlowestSize(3);
		for (long ms : new long[] { 50, 10, 400, 20, 300, 5, 100 }) {
			history.add(query(ms));
		}
		assertEquals(Arrays.asList(400L, 300L, 100L), elapsed(history.getSlowest()));
		// Newer, faster queries don't push them out.
		history.add(query(60));
		history.add(query(100));
		assertEquals(Arrays.asList(400L, 300L, 100L), elapsed(history.getSlowest()));
	}

	@Test
	public void testEquallySlowQueriesKeepTheEarlierFirst() {
		QueryHistory history = new QueryHistory();
		QueryRecord first = query(100);
		QueryRecord second = query(100);
		history.add(first);
		history.add(second);
		assertSame(first, history.getSlowest().get(0));
		assertSame(second, history.getSlowest().get(1));
	}

	@Test
	public void testDefaultsAndResizing() {
		QueryHistory history = new QueryHistory();
		assertEquals(QueryHistory.DEFAULT_SIZE, history.getPerRequestSize());
		for (long i = 1; i <= QueryHistory.DEFAULT_SIZE + 10; i++) {
			history.add(query(i));
		}
		assertEquals(QueryHistory.DEFAULT_SIZE, history.getRecent().size());
		assertEquals(QueryHistory.DEFAULT_SIZE, history.getSlowest().size());
		history.setRecentSize(2);
		history.setSlowestSize(1);
		assertEquals(2, history.getRecent().size());
		assertEquals(Arrays.asList((long) QueryHistory.DEFAULT_SIZE + 10), elapsed(history.getSlowest()));
	}

	@Test
	public void testSizeZeroTurnsListsOff() {
		QueryHistory history = new QueryHistory();
		history.setRecentSize(0);
		history.setSlowestSize(0);
		assertTrue(history.isRecording());
		history.add(query(1));
		assertTrue(history.getRecent().isEmpty());
		assertTrue(history.getSlowest().isEmpty());
		history.setPerRequestSize(0);
		assertFalse(history.isRecording());
	}

}
