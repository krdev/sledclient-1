package com.lafaspot.sled.session;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Handles channel inactivity.
 *
 * @author kraman
 *
 */
public class SledInactivityHandler extends IdleStateHandler {

	/** the session object. */
	private final SledSession session;

	/** the logger object. */
	private final Logger logger;

	/**
	 * Constructor to handle inactivity events.
	 *
	 * @param session
	 *            the session
	 * @param inactivityTimeout
	 *            timeout value
	 * @param logger
	 *            the logger
	 */
	public SledInactivityHandler(@Nonnull final SledSession session, final long inactivityTimeout,
			@Nonnull final Logger logger) {
		super(inactivityTimeout, 0, inactivityTimeout, TimeUnit.MILLISECONDS);
		this.session = session;
		this.logger = logger;
	}

	@Override
	protected void channelIdle(final ChannelHandlerContext ctx, final IdleStateEvent evt) {
		logger.debug(" <-> channel inactive " + evt.state(), null);
		if (evt.state() == IdleState.ALL_IDLE) {
			ctx.close();
		}
		session.onTimeout();
	}
}

