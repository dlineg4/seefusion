package com.seefusion;

//import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.PrintWriter;

import org.junit.Test;

import mockit.Expectations;
import mockit.Injectable;
import mockit.VerificationsInOrder;

public class SmtpSenderTest {

	protected static final String CRLF = "\r\n";

	@Test
	public void testAuth(@Injectable final BufferedReader in, @Injectable final PrintWriter out) throws Exception {
		new Expectations() {{
			// SMTP server replies, returned in order by successive readLine() calls
			in.readLine(); returns(
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
		}};
		SmtpSender test = new SmtpSender("localhost", "username", "password");
		SmtpMessage message = new SmtpMessage();
		message.setSmtpFrom("me@my.com");
		message.setSmtpTo("me@my.com");
		message.setSmtpSubject("Hello, cruel world!");
		message.setSmtpBody("Bah!");
		test.send(in, out, message);
		new VerificationsInOrder() {{
			out.print("EHLO SeeFusion" + CRLF);
			out.flush();
			out.print("AUTH LOGIN" + CRLF);
			out.flush();
			out.print("dXNlcm5hbWU=" + CRLF);
			out.flush();
			out.print("cGFzc3dvcmQ=" + CRLF);
			out.flush();
		}};
		
	}

}
