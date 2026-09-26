package com.seefusion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class SeeFusionTest extends TestCase {
    
    /**
     * Test of isDebugIP method, of class com.seefusion.SeeFusion.
     */
	@Test
	public void testIsDebugIP() {
        SeeFusion sf = new SeeFusion(true);
        sf.setDebugIPs("127.0.0.1");
        assertTrue(sf.isDebugIP("127.0.0.1"));
        assertFalse(sf.isDebugIP(""));
        assertFalse(sf.isDebugIP(null));
        sf.setDebugIPs(null);
        assertFalse(sf.isDebugIP("127.0.0.1"));
        assertFalse(sf.isDebugIP(""));
        assertFalse(sf.isDebugIP(null));
        sf.setDebugIPs("");
        assertFalse(sf.isDebugIP("127.0.0.1"));
        assertFalse(sf.isDebugIP(""));
        assertFalse(sf.isDebugIP(null));
    }

	@Test
	public void testFileUrlToPathDecodesSpaces() throws Exception {
		// CommandBox server homes contain the server name, which may have spaces.
		File jar = new File(System.getProperty("java.io.tmpdir"), "My Server/seefusion/seefusion.jar");
		String url = jar.toURI().toURL().toString();
		assertTrue(url.contains("%20"));
		assertEquals(jar.getPath(), SeeFusion.fileUrlToPath(url));
	}

	@Test
	public void testFileUrlToPathKeepsDirectorySeparator() throws Exception {
		File dir = new File(System.getProperty("java.io.tmpdir"), "My Server/classes");
		String url = dir.toURI().toString();
		if (!url.endsWith("/")) {
			url += "/";
		}
		assertEquals(dir.getPath() + File.separator, SeeFusion.fileUrlToPath(url));
	}

}
