/**
 *
 */
package com.lafaspot.sled.client;

import java.util.concurrent.ExecutionException;

import org.testng.annotations.Test;

import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;
import com.lafaspot.sled.session.SledSession;

/**
 * @author kraman
 *
 */
public class SledClientIT {

	@Test
	public void testGetSled() throws SledException, InterruptedException, ExecutionException {

		LogManager logManager = new LogManager(Level.DEBUG, 5);
		logManager.setLegacy(true);
		Logger logger = logManager.getLogger(new LogContext(SledClientIT.class.getName()) {
		});
		SledClient cli = new SledClient(10, logManager);

		final String server = "localhost";
		final int port = 14053;
		final int connectTimeout = 100;
		final int inactivityTimeout = 100;

		SledSession sess = cli.createSession(server, port);

		System.out.println("connecting to " + server + ", " + port);
		SledFuture<Boolean> f1 = sess.connect(connectTimeout, inactivityTimeout);

		f1.get();
		System.out.println("connected");

		SledFuture<String> f2 = sess.getSled();
		System.out.println("getting sled");
		System.out.println(" SLED " + f2.get());
		System.out.println("done");
		cli.shutdown();
	}

}
