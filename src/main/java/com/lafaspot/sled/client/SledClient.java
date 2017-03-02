/**
 *
 */
package com.lafaspot.sled.client;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.sled.session.SledSession;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * POP client that supports secure connection and POP3 protocol.
 *
 * @author kraman
 *
 */
public class SledClient {

	/** instance id used for debug. */
	private final String instanceId = Integer.toString(new Random(System.nanoTime()).nextInt());

	/** counter for sessions. */
	private AtomicInteger sessionCounter = new AtomicInteger(1);

	/** The netty bootstrap. */
	private final Bootstrap bootstrap;

	/** Event loop group that will serve all channels for IMAP client. */
	private final EventLoopGroup group;

	/** The log manger. */
	private final LogManager logManager;

	/** The logger. */
	private Logger logger;

	/**
	 * Constructor to create a new POP client.
	 *
	 * @param threads
	 *            number of threads to use
	 * @param logManager
	 *            the log manager
	 * @throws SledException
	 *             on failure
	 */
	public SledClient(final int threads, @Nonnull final LogManager logManager) throws SledException {
		this.bootstrap = new Bootstrap();
		this.group = new NioEventLoopGroup(threads);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.group(group);
		bootstrap.handler(new SledClientInitializer(logger));

		this.logManager = logManager;
		LogContext context = new SessionLogContext("SledClient");
		this.logger = logManager.getLogger(context);

	}

	/**
	 * Create a sled session object.
	 *
	 * @param server
	 *            to connect to
	 * @param port
	 *            to connect to
	 * @return SledSession the newly created session
	 */
	public SledSession createSession(@Nonnull final String server, final int port) {
		return new SledSession(bootstrap, server, port, logger);
	}

	/**
	 * Shutdown this instance of the client.
	 */
	public void shutdown() {
		this.group.shutdown();
	}

}
