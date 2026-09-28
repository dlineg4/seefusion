package com.seefusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EnvironmentCheckTest {

	@Test
	public void testJavaMajorVersion() {
		assertEquals(8, EnvironmentCheck.javaMajorVersion("1.8"));
		assertEquals(11, EnvironmentCheck.javaMajorVersion("11"));
		assertEquals(17, EnvironmentCheck.javaMajorVersion("17.0.2"));
		assertEquals(21, EnvironmentCheck.javaMajorVersion("21"));
		assertEquals(0, EnvironmentCheck.javaMajorVersion(null));
		assertEquals(0, EnvironmentCheck.javaMajorVersion("unknown"));
		assertTrue(EnvironmentCheck.javaMajorVersion() >= 8);
	}

	@Test
	public void testJavaxClasspathIsNotJakartaOnly() {
		// The test class path has the javax servlet API.
		assertFalse(EnvironmentCheck.isJakartaOnly(getClass().getClassLoader()));
	}

	@Test
	public void testNoServletApiIsNotJakartaOnly() {
		// e.g. Tomcat's system class path: no servlet API at all. Must not count as Jakarta.
		assertFalse(EnvironmentCheck.isJakartaOnly(new FakeLoader(false, false)));
	}

	@Test
	public void testJakartaOnly() {
		assertTrue(EnvironmentCheck.isJakartaOnly(new FakeLoader(false, true)));
		assertFalse(EnvironmentCheck.isJakartaOnly(new FakeLoader(true, true)));
	}

	/** Pretends the javax and/or jakarta servlet API is present. */
	private static class FakeLoader extends ClassLoader {
		private final boolean javax;
		private final boolean jakarta;

		FakeLoader(boolean javax, boolean jakarta) {
			super(null);
			this.javax = javax;
			this.jakarta = jakarta;
		}

		@Override
		public Class<?> loadClass(String name) throws ClassNotFoundException {
			if ((javax && name.startsWith("javax.servlet.")) || (jakarta && name.startsWith("jakarta.servlet."))) {
				return Object.class;
			}
			throw new ClassNotFoundException(name);
		}
	}
}
