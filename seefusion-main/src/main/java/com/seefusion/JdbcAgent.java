/*
 * JdbcAgent.java
 *
 * Java agent (-javaagent:seefusion.jar) that monitors queries on datasources that are not wrapped with
 * com.seefusion.Driver, by wrapping the connection returned from every JDBC driver's connect().
 *
 * This file is part of SeeFusion. SeeFusion is free software, licensed under the GNU General Public
 * License version 3; see the LICENSE file in the root of this repository.
 */

package com.seefusion;

import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isSubTypeOf;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import java.lang.instrument.Instrumentation;
import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.utility.JavaModule;

public class JdbcAgent {

	private static final Logger LOG = Logger.getLogger(JdbcAgent.class.getName());

	private static boolean installed = false;

	public static void premain(String args, Instrumentation inst) {
		install(inst);
	}

	public static void agentmain(String args, Instrumentation inst) {
		install(inst);
	}

	static synchronized void install(Instrumentation inst) {
		if (installed) {
			return;
		}
		installed = true;
		new AgentBuilder.Default()
			.disableClassFormatChanges()
			.with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
			.ignore(nameStartsWith("com.seefusion.")
				.or(nameStartsWith("net.bytebuddy."))
				.or(nameStartsWith("java."))
				.or(nameStartsWith("javax."))
				.or(nameStartsWith("jdk."))
				.or(nameStartsWith("sun."))
				.or(isSynthetic()))
			.type(isSubTypeOf(java.sql.Driver.class).and(not(isInterface())).and(not(isAbstract())))
			.transform(new AgentBuilder.Transformer.ForAdvice()
				.include(JdbcAgent.class.getClassLoader())
				.advice(named("connect")
						.and(takesArguments(String.class, Properties.class))
						.and(returns(Connection.class))
						.and(not(isAbstract())),
					ConnectAdvice.class.getName()))
			.with(new InstrumentationLogger())
			.installOn(inst);
		LOG.info("SeeFusion JDBC agent installed");
	}

	/**
	 * Called (reflectively, from ConnectAdvice) with each connection a JDBC driver returns. Returns it
	 * wrapped for monitoring, or unchanged if SeeFusion already monitors it. Never throws.
	 */
	public static Object wrap(Object driver, String url, Properties info, Object connection) {
		if (!(connection instanceof Connection)) {
			return connection;
		}
		// com.seefusion.Driver is calling the real driver: that wrapper monitors this connection itself.
		if (Driver.CONNECTING.get() != null) {
			return connection;
		}
		if (connection.getClass().getName().startsWith("com.seefusion.")) {
			return connection;
		}
		try {
			return Driver.wrapConnection((Connection) connection, info == null ? new Properties() : info, new Properties());
		}
		catch (Throwable t) {
			LOG.log(Level.WARNING, "Unable to monitor connection from " + driver.getClass().getName(), t);
			return connection;
		}
	}

	/**
	 * Inlined into each driver's connect(). That code runs in the driver's class loader (for Lucee, often
	 * an OSGi bundle), which can't see SeeFusion's classes, so it only uses java.* types: it reaches
	 * JdbcAgent.wrap() through the system class loader, where the SeeFusion jar always is.
	 */
	public static class ConnectAdvice {

		@Advice.OnMethodExit(suppress = Throwable.class)
		public static void exit(@Advice.This Object driver,
				@Advice.Argument(0) String url,
				@Advice.Argument(1) Properties info,
				@Advice.Return(readOnly = false, typing = Assigner.Typing.DYNAMIC) Object connection) {
			if (connection != null) {
				try {
					connection = ClassLoader.getSystemClassLoader()
						.loadClass("com.seefusion.JdbcAgent")
						.getMethod("wrap", Object.class, String.class, Properties.class, Object.class)
						.invoke(null, driver, url, info, connection);
				}
				catch (Throwable t) {
					// Never break the application's connection.
				}
			}
		}
	}

	/**
	 * Logs which drivers get monitored. Type-resolution errors happen for many unrelated classes, so
	 * they're only logged at FINE.
	 */
	static class InstrumentationLogger extends AgentBuilder.Listener.Adapter {

		@Override
		public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
				boolean loaded, DynamicType dynamicType) {
			LOG.info("Monitoring JDBC driver " + typeDescription.getName());
		}

		@Override
		public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
			LOG.log(Level.FINE, "Could not instrument " + typeName, throwable);
		}
	}
}
