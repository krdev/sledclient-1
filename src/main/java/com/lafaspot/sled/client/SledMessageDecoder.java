/**
 *
 */
package com.lafaspot.sled.client;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.sled.session.SledSession;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

/**
 * Class to decode messages from POP server.
 *
 * @author kraman
 *
 */
public class SledMessageDecoder extends MessageToMessageDecoder<String> {

	/** The session to be used. */
	private final SledSession session;

	/**
	 * Constructor for the message decoder.
	 *
	 * @param session
	 *            the session object
	 * @param logger
	 *            the logger object
	 */
	public SledMessageDecoder(@Nonnull final SledSession session, @Nonnull final Logger logger) {
		this.session = session;
	}

	@Override
	protected void decode(final ChannelHandlerContext ctx, final String msg, final List<Object> out)
			throws IOException {
		session.onResponse(msg);
	}
}
