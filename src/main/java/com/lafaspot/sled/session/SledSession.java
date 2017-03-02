/**
 *
 */
package com.lafaspot.sled.session;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.sled.client.SledException;
import com.lafaspot.sled.client.SledException.Type;
import com.lafaspot.sled.client.SledFuture;
import com.lafaspot.sled.client.SledMessageDecoder;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * @author kraman
 *
 */
public class SledSession {

	/** current state. */
	private final AtomicReference<State> stateRef = new AtomicReference<>(State.NULL);

	/** Netty bootstrap object. */
	private final Bootstrap bootstrap;

	/** Server ip/name to connect to. */
	@Nonnull
	private final String server;

	/** Server port to connect to. */
	private final int port;

	/** The logger object. */
	private final Logger logger;

	/** Netty channel. */
	private Channel sessionChannel;

	/** The future object. */
	private SledFuture<String> getSledFuture;

	/** State. */
	public enum State {
		/** not initialized. */
		NULL,
		/** sent connect request. */
		CONNECT_SENT,
		/** connected to server. */
		CONNECTED,
		/** command was sent to server. */
		COMMAND_SENT
	}

	/**
	 * Constructor.
	 *
	 * @param bootstrap
	 *            netty bootstrap
	 * @param server
	 *            ip/name of server
	 * @param port
	 *            server port
	 * @param logger
	 *            logger object
	 */
	public SledSession(@Nonnull final Bootstrap bootstrap, @Nonnull final String server, final int port,
			@Nonnull final Logger logger) {
		this.bootstrap = bootstrap;
		this.server = server;
		this.port = port;
		this.logger = logger;
	}

	/**
	 * Connect to server.
	 *
	 * @param connectTimeout
	 *            value
	 * @param inactivityTimeout
	 *            value
	 * @return the future object
	 * @throws SledException
	 *             on failure
	 */
	public SledFuture<Boolean> connect(final int connectTimeout, final int inactivityTimeout) throws SledException {
		// bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
		// connectTimeout);
		ChannelFuture future = bootstrap.connect(server, port);

		stateRef.compareAndSet(State.NULL, State.CONNECT_SENT);
		sessionChannel = future.channel();
		sessionChannel.config().setConnectTimeoutMillis(50);

		final SledSession thisSession = this;
		final SledFuture<Boolean> connectFuture = new SledFuture<Boolean>(future);
		future.addListener(new GenericFutureListener<Future<? super Void>>() {

			@Override
			public void operationComplete(final Future<? super Void> future) throws Exception {
				if (future.isSuccess()) {
					if (!stateRef.compareAndSet(State.CONNECT_SENT, State.CONNECTED)) {
						connectFuture.done(new SledException(SledException.Type.INTERNAL_FAILURE));
						logger.error("Connect success in invalid state " + stateRef.get().name(), null);
						return;
					}

					sessionChannel.pipeline()
							.addFirst(new SledInactivityHandler(thisSession, inactivityTimeout, logger));
					sessionChannel.pipeline().addLast(new SledMessageDecoder(thisSession, logger));

					// register for close event
					sessionChannel.closeFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							if (null != getSledFuture && !getSledFuture.isDone()) {
								getSledFuture.done(new SledException(Type.INTERNAL_FAILURE));
							} else {
								if (!connectFuture.isDone()) {
									connectFuture.done(new SledException(Type.INTERNAL_FAILURE));
								}
							}
						}
					});
					connectFuture.done(Boolean.TRUE);
				} else {
					connectFuture.done(new SledException(SledException.Type.INTERNAL_FAILURE));
				}
			}
		});
		return connectFuture;
	}

	/**
	 * Fetch SLEDID from server on a connected session.
	 *
	 * @return SledFuture the future object
	 * @throws SledException
	 *             on failure
	 */
	public SledFuture<String> getSled() throws SledException {

		if (stateRef.get() != State.CONNECTED) {
			throw new SledException(Type.INVALID_STATE);
		}

		Future f = sessionChannel.writeAndFlush("\n");
		getSledFuture = new SledFuture<String>(f);
		return getSledFuture;
	}

	/**
	 * Received a response on the channe.
	 *
	 * @param msg
	 *            received from server
	 */
	public void onResponse(@Nonnull final String msg) {
		getSledFuture.done(msg);
		this.sessionChannel.close();
	}

	/**
	 * Timeout on channel, inactivity.
	 */
	public void onTimeout() {
		getSledFuture.done(new SledException(SledException.Type.TIMEDOUT));
		this.sessionChannel.close();
	}

}
