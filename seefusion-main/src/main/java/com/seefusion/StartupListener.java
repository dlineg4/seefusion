/*
 * StartupListener.java
 *
 * Starts SeeFusion, and with it the monitoring port, when the web application starts. Without it SeeFusion
 * starts in Filter.init(), and servlet containers such as Undertow (CommandBox) only initialize a filter when
 * the first request reaches it, so the monitoring port stays closed until then. Register it in web.xml next
 * to com.seefusion.Filter.
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License, version 3 or (at your option) any later version; see LICENSE and README.md in the root of
 * this repository.
 */

package com.seefusion;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class StartupListener implements ServletContextListener {

	private static final Logger LOG = Logger.getLogger(StartupListener.class.getName());

	@Override
	public void contextInitialized(ServletContextEvent event) {
		try {
			SeeFusion.getInstance();
		} catch (RuntimeException e) {
			// Don't stop the application from starting; Filter.init() tries again on the first request.
			LOG.log(Level.WARNING, "SeeFusion could not start with the application; it will try again on the first request", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// Nothing to do: Filter.destroy() handles shutdown.
	}

}
