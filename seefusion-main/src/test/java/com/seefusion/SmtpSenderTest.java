package com.seefusion;

//import static org.junit.Assert.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.PrintWriter;

import org.junit.Test;
import org.mockito.InOrder;

public class SmtpSenderTest {

	protected static final String CRLF = "\r\n";

	@Test
	public void testAuth() throws Exception {
		final BufferedReader in = mock(BufferedReader.class);
		final PrintWriter out = mock(PrintWriter.class);
		// SMTP server replies, returned in order by successive readLine() calls
		when(in.readLine()).thenReturn(
				"220 smtp.server.com Simple Mail Transfer Service Ready",
				"250-whatever" + CRLF,
				"250 whatever" + CRLF,
				"334 VXNlcm5hbWU6" + CRLF,
				"334 UGFzc3dvcmQ6" + CRLF,
				"235 OK whatever" + CRLF,
				"250 OK" + CRLF,
				"250 OK" + CRLF,
				"354 OK" + CRLF,
				"250 OK" + CRLF);
		SmtpSender test = new SmtpSender("localhost", "username", "password");
		SmtpMessage message = new SmtpMessage();
		message.setSmtpFrom("me@my.com");
		message.setSmtpTo("me@my.com");
		message.setSmtpSubject("Hello, cruel world!");
		message.setSmtpBody("Bah!");
		test.send(in, out, message);
		InOrder inOrder = inOrder(out);
		inOrder.verify(out).print("EHLO SeeFusion" + CRLF);
		inOrder.verify(out).flush();
		inOrder.verify(out).print("AUTH LOGIN" + CRLF);
		inOrder.verify(out).flush();
		inOrder.verify(out).print("dXNlcm5hbWU=" + CRLF);
		inOrder.verify(out).flush();
		inOrder.verify(out).print("cGFzc3dvcmQ=" + CRLF);
		inOrder.verify(out).flush();
		
	}

}
