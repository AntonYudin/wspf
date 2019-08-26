
/*
 * Copyright Anton Yudin, https://antonyudin.com/software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.antonyudin.wspf.server;


import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import java.io.Closeable;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.HandshakeResponse;
import javax.websocket.EndpointConfig;

import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;


@ServerEndpoint(value = "/passws/{identity}", configurator = Endpoint.Configurator.class)
public class Endpoint {

	private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
		Endpoint.class.getName()
	);


	private final static String initParameters = "initParameters";

	@OnOpen
	public void open(final Session session, final EndpointConfig config) {
		logger.info("open: [" + session + "]");
	}


	@OnClose
	public void close(final @PathParam("identity") String identity, final Session session) throws java.lang.Exception {

		logger.info("close: [" + session + "]");

		final var connection = getConnection(session, identity, false);

		if (connection != null)
			connection.close();
	}


	@OnError
	public void onError(final Throwable error) {
		logger.info("error: [" + error + "]");
	}


	@OnMessage
	public void handleMessage(final String message, final Session session) {
		logger.info("handleMessage(" + message + ", " + session + ")");
	}


	@OnMessage
	public void handleMessage(final @PathParam("identity") String identity, final byte[] data, final Session session) throws java.lang.Exception {

	//	logger.info("handleMessage(" + identity + ", " + data + ", " + session + ")");

		try {
			getConnection(session, identity, true).write(data);
		} catch (java.lang.Exception exception) {
			logger.log(java.util.logging.Level.SEVERE, "exception", exception);
			session.close();
		}
	}

	public final static String remoteAddressName = Connection.class.getName() + ".remoteAddress";

	private String remoteAddress = null;

	public void setRemoteAddress(final String value) {
		remoteAddress = value;
	}


	protected Connection getConnection(
		final Session session,
		final String identity,
		final boolean create
	) throws java.lang.Exception {

	//	logger.info("user properties: [" + session.getUserProperties() + "]");

		var connection = (Connection) session.getUserProperties().get(
			Connection.class.getName()
		);

	//	logger.info("connection: [" + connection + "]");

		if ((connection == null) && create) {

			logger.info("getConnection(" + session + ", " + identity + ", " + create + ")");

			final var pass = getPass(session, identity);

			logger.info("found pass: [" + pass + "]");

	/*		final String remoteAddress = (String) session.getUserProperties().get(
				remoteAddressName
			);
	*/
			if (!pass.isAllowed(remoteAddress))
				throw new IllegalArgumentException("remote address [" + remoteAddress + "] is not allowed, allowed rules: " + pass.getAllowed());

			connection = new Connection(
				pass,
				(buffer, offset, length)-> session.getBasicRemote().sendBinary(
					java.nio.ByteBuffer.wrap(buffer, offset, length)
				),
				()-> session.close()
			);

			logger.info("connection: [" + connection + "]");

			session.getUserProperties().put(connection.getClass().getName(), connection);


		}

		return connection;
	}


	private final List<Pass> passes = new ArrayList<>();


	protected Pass getPass(final Session session, final String identity) {

		logger.info("getPass(" + session + ", " + identity + ")");

		if (passes.isEmpty()) {

			synchronized (passes) {

				if (passes.isEmpty()) {

					for (int i = 0;; i++) { 

						final var value = ((ParameterGetter) session.getUserProperties().get(initParameters)).get("pass." + i);

						if (value == null)
							break;

						final var pass = new Pass(value);

						logger.info("found pass: [" + pass + "]");

						passes.add(pass);
					}
				}
			}
		}

		for (Pass pass: passes) {
			if (pass.getIdentity().equals(identity))
				return pass;
		}

		return null;
	}


	public static class Pass implements java.io.Serializable {


		public Pass(final String configuration) {

			final var parts = configuration.split(";");

			String identity = null;
			String address = null;
			int port = -1;

			for (var part: parts) {

				final var p = part.split("=");

				switch (p[0]) {
					case "identity": identity = p[1]; break;
					case "remote":
						 var s = p[1].split(":");
						 address = s[0];
						 port = Integer.parseInt(s[1]);
						 break;
					case "allowed":
						 for (var a: p[1].split(","))
							 allowed.add(a);
						 break;
					default:
						 throw new IllegalArgumentException("unknown parameter: [" + p[0] + "]");
				}
			}

			this.identity = identity;
			this.address = address;
			this.port = port;
		}


		private final String identity;

		public String getIdentity() {
			return identity;
		}


		private final String address;

		public String getAddress() {
			return address;
		}


		private final int port;

		public int getPort() {
			return port;
		}


		private final List<String> allowed = new ArrayList<>();

		public List<String> getAllowed() {
			return allowed;
		}

		public boolean isAllowed(final String address) {
			logger.info("isAllowed(" + address + ")");
			return (getAllowed().isEmpty() || getAllowed().contains(address));
		}


		public String toString() {
			return (
				getClass().getName() + "[" +
				getIdentity() + " - " +
				getAddress() + ":" +
				getPort() + " {" +
				getAllowed() + "}" +
				"]"
			);
		}
	}


	public static class Connection implements java.io.Serializable, Closeable, Runnable {

		public interface Writer {
			public void write(final byte[] buffer, final int offset, final int length) throws java.lang.Exception;
		}


		private final Pass pass;
		private final Writer writer;
		private final Closeable closeable;

		public Connection(
			final Pass pass,
			final Writer writer,
			final Closeable closeable
		) throws java.io.IOException {
			this.pass = pass;
			this.writer = writer;
			this.socket = new java.net.Socket(pass.getAddress(), pass.getPort());
			this.closeable = closeable;
			logger.info("socket: [" + this.socket + "]");
			(new Thread(this)).start();
		}


		private final java.net.Socket socket;

		public void write(final byte[] data) throws java.lang.Exception {
			socket.getOutputStream().write(data);
		}



		@Override
		public void run() {

			try (closeable) {

				final var buffer = new byte[1024 * 10];

				for (;;) {

					final var read = socket.getInputStream().read(buffer);

					if (read < 0)
						break;

					writer.write(buffer, 0, read);
				}

			} catch (java.lang.Exception exception) {
				logger.log(java.util.logging.Level.SEVERE, "exception", exception);
			}
		}

		@Override
		public void close() throws java.io.IOException {
			logger.info("close()");
			socket.close();
		}

	}


	public interface ParameterGetter {
		public String get(final String name);
	}


	public static class Configurator extends ServerEndpointConfig.Configurator {


		private final static ThreadLocal<String> remoteAddress = new ThreadLocal<>();


		public Configurator() {
			logger.info("Configurator created");
		}


		/*
		 * The functionality of getting the remote address from the endpoint instance
		 * relies on the fact that the Configurator methods are called from the same thread
		 * per request.
		 *
		 * modifyHandshake() has access to the httpSession, but does not have access to the Endpoint
		 * instance or its per-instance user properties.
		 *
		 * getEndpointInstance() has access to the Endpoint instance, but does not have access to the
		 * httpSession.
		 *
		 */

		@Override
		public <T> T getEndpointInstance(final Class<T> endpointClass) throws InstantiationException {
			logger.info("getEndpointInstance(" + endpointClass + ")");

			final T result = super.getEndpointInstance(endpointClass);

			Endpoint.class.cast(result).setRemoteAddress(remoteAddress.get());

			return result;
		}


		@Override
		public void modifyHandshake(
			final ServerEndpointConfig config,
			final HandshakeRequest request,
			final HandshakeResponse response
		) {

			logger.info("modifyHandshake(" + config + ", " + request + ", " + response + ")");

			final Map<String, String> parameters = new HashMap<>();

			final var httpSession = ((javax.servlet.http.HttpSession) request.getHttpSession());

			logger.info("httpSession: [" + httpSession + "]");

			final var servletContext = httpSession.getServletContext();

			logger.info("servletContext: [" + servletContext + "]");

			remoteAddress.set((String) httpSession.getAttribute(remoteAddressName));

			if (config.getUserProperties().get(initParameters) == null) {

				logger.info("create init parameters ...");

				servletContext.getInitParameterNames().asIterator().forEachRemaining(
					(name)->
						parameters.put(name, servletContext.getInitParameter(name))
				);


				config.getUserProperties().put(
					initParameters,
					(ParameterGetter) (name)-> parameters.get(name)
				);
			} else
				logger.info("init parameters had been already initialized ...");
		}

	}

}

