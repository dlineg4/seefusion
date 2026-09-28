/*
 * QueryHistory.java
 *
 * The finished queries kept for the query lists: the most recent ones, and the slowest ones since
 * SeeFusion started. Also holds how many queries each request keeps for its details (see RequestInfo).
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

final class QueryHistory {

	static final int DEFAULT_SIZE = 100;

	// Newest first.
	private final LinkedList<QueryRecord> recent = new LinkedList<QueryRecord>();

	// Slowest first; among equally slow queries, the earlier one first.
	private final ArrayList<QueryRecord> slowest = new ArrayList<QueryRecord>();

	private volatile int recentSize = DEFAULT_SIZE;

	private volatile int slowestSize = DEFAULT_SIZE;

	private volatile int perRequestSize = DEFAULT_SIZE;

	synchronized void add(QueryRecord query) {
		if (recentSize > 0) {
			recent.addFirst(query);
			while (recent.size() > recentSize) {
				recent.removeLast();
			}
		}
		if (slowestSize > 0
				&& (slowest.size() < slowestSize || query.elapsedMs > slowest.get(slowest.size() - 1).elapsedMs)) {
			int i = slowest.size();
			while (i > 0 && slowest.get(i - 1).elapsedMs < query.elapsedMs) {
				i--;
			}
			slowest.add(i, query);
			while (slowest.size() > slowestSize) {
				slowest.remove(slowest.size() - 1);
			}
		}
	}

	/**
	 * @return true if finished queries are kept anywhere (so they're worth recording)
	 */
	boolean isRecording() {
		return recentSize > 0 || slowestSize > 0 || perRequestSize > 0;
	}

	synchronized List<QueryRecord> getRecent() {
		return new ArrayList<QueryRecord>(recent);
	}

	synchronized List<QueryRecord> getSlowest() {
		return new ArrayList<QueryRecord>(slowest);
	}

	synchronized void setRecentSize(int size) {
		recentSize = Math.max(0, size);
		while (recent.size() > recentSize) {
			recent.removeLast();
		}
	}

	synchronized void setSlowestSize(int size) {
		slowestSize = Math.max(0, size);
		while (slowest.size() > slowestSize) {
			slowest.remove(slowest.size() - 1);
		}
	}

	int getPerRequestSize() {
		return perRequestSize;
	}

	void setPerRequestSize(int size) {
		perRequestSize = Math.max(0, size);
	}

}
