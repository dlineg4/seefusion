package com.seefusion;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ResourceBundle;

import org.junit.Test;
import org.mockito.MockedStatic;

public class ActiveMonitoringRuleTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testLogIncident() throws Exception {
		final SeeFusion sf = mock(SeeFusion.class);
		// Incident.getRequests() looks up the DbLogger via the static SeeFusion.getInstance(),
		// so point that at the mock too.
		try (MockedStatic<SeeFusion> seeFusionStatics = mockStatic(SeeFusion.class)) {
			seeFusionStatics.when(SeeFusion::getInstance).thenReturn(sf);
			final RequestList requestList = new RequestList();
			final DbLogger dbLogger = new DbLogger(sf);
			when(sf.getDbLogger()).thenReturn(dbLogger);
			when(sf.getMasterRequestList()).thenReturn(requestList);
			RequestInfo ri = new RequestInfo("SomeRequest", "SomeServer");
			ri.setServerName("test");
			requestList.createRequest(ri);
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			DbLoggerConnectionPool pool = new DbLoggerConnectionPool("jdbc:derby:memory:test;create=true", null, null);
			dbLogger.setConnectionPool(pool);

			ActiveMonitoringRule test = new RuleActiveRequests();
			String id = test.logIncident(sf, System.currentTimeMillis(), "foo");
			SeeDAO<Incident> dao = (SeeDAO<Incident>) dbLogger.getDao(Incident.class);
			Thread.sleep(200);
			dao.clearCache();
			Incident i = dao.getById(id);
			assertEquals(1, i.getRequests().length());
		}
	}

	@Test
	public void testDoNotify() throws Exception {
		final SeeFusion sf = mock(SeeFusion.class);
		final RequestList requestList = new RequestList();
		final CountersHistory countersHistory = new CountersHistory(60, sf);
		final ResourceBundle resources = ResourceBundle.getBundle("com.seefusion.SeeFusionMessages");
		final MessageFormatFactory messageFormatFactory = new MessageFormatFactory(resources);
		when(sf.getMasterRequestList()).thenReturn(requestList);
		when(sf.getHistoryMinutes()).thenReturn(countersHistory);
		when(sf.getMessageFormatFactory()).thenReturn(messageFormatFactory);
		when(sf.getInstanceName()).thenReturn("Server1");
		RequestInfo ri = new RequestInfo(sf, "serverName1", "requestUri1", "queryString1", "remoteIp1", "get", "/path1", true);
		requestList.createRequest(ri);
		Thread.sleep(100);
		ri.setResponseCode(200);
		ri.outputStarted();
		Thread.sleep(100);
		requestList.releaseRequest(ri);
		QueryInfo qi = new QueryInfo();
		qi.setActive();
		qi.setQueryText("some sql");
		Thread.sleep(50);
		qi.setInactive();
		qi.setResultCount(123);
		ri.setLongestQueryInfo(qi);
		ri.setQueryCount(1);
		ri.setQueryTimeMs(100);
		
		ri = new RequestInfo(sf, "serverName2", "requestUri2", "queryString2", "remoteIp2", "get", "/path2", true);
		ri.setServerName("test");
		requestList.createRequest(ri);
		Thread.sleep(100);
		ri.setResponseCode(200);
		ri.outputStarted();
		Thread.sleep(100);
		qi = new QueryInfo();
		qi.setActive();
		qi.setQueryText("some more sql");
		qi.setResultCount(234);
		ri.setQueryInfo(qi);
		ri.setQueryCount(0);
		
		ActiveMonitoringRule test = new RuleActiveRequests();
		System.out.println(test.buildMessageBody(sf));
	}

	
}
