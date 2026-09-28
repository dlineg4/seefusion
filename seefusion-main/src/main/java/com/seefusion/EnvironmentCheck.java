/*
 * EnvironmentCheck.java
 *
 * Startup checks for environments this fork doesn't support: Jakarta servlet containers (SeeFusion is
 * javax-only) and Java 20 and later (kill-stop uses Thread.stop(), which always throws there).
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import java.util.logging.Level;
import java.util.logging.Logger;

final class EnvironmentCheck {

	/** Last Java version where Thread.stop() still works (it always throws from Java 20). */
	static final int LAST_JAVA_WITH_THREAD_STOP = 19;

	private EnvironmentCheck() {
	}

	/** Major Java version of this JVM, e.g. 8, 11 or 21; 0 if unknown. */
	static int javaMajorVersion() {
		return javaMajorVersion(System.getProperty("java.specification.version"));
	}

	/** Parses a java.specification.version such as "1.8", "11" or "21"; 0 if it can't. */
	static int javaMajorVersion(String spec) {
		if (spec == null) {
			return 0;
		}
		String major = spec.startsWith("1.") ? spec.substring(2) : spec;
		int dot = major.indexOf('.');
		if (dot >= 0) {
			major = major.substring(0, dot);
		}
		try {
			return Integer.parseInt(major);
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * True when the loader sees the Jakarta servlet API but not the javax one, as on CommandBox's Jakarta
	 * server (Runwar 6). Just "no javax" isn't enough: in e.g. Tomcat the servlet API isn't on the system
	 * class path at all.
	 */
	static boolean isJakartaOnly(ClassLoader loader) {
		return !canLoad(loader, "javax.servlet.Filter") && canLoad(loader, "jakarta.servlet.Filter");
	}

	private static boolean canLoad(ClassLoader loader, String className) {
		try {
			loader.loadClass(className);
			return true;
		}
		catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	/** Logs a warning if this JVM can't do kill-stop. */
	static void warnIfKillStopUnsupported(Logger log) {
		int major = javaMajorVersion();
		if (major > LAST_JAVA_WITH_THREAD_STOP) {
			log.warning("Java " + major + ": SeeFusion's kill-stop doesn't work on Java 20 and later "
				+ "(Thread.stop() throws UnsupportedOperationException). The regular kill still works.");
		}
	}

	/** Logs lines as one framed banner, so it stands out in server logs. */
	static void logBanner(Logger log, Level level, String... lines) {
		StringBuilder sb = new StringBuilder();
		String rule = "==========================================================================";
		sb.append('\n').append(rule).append('\n');
		for (String line : lines) {
			sb.append("  ").append(line).append('\n');
		}
		sb.append(rule);
		log.log(level, sb.toString());
	}
}
