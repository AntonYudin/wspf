
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


package com.antonyudin.wspf.client;


import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.ClientEndpoint;


@ClientEndpoint
public class Client {


	private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
		Client.class.getName()
	);


	public Client(
		final Closeable closeable,
		final InputStream inputStream,
		final OutputStream outputStream
	) {
		this.closeable = closeable;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	private final Closeable closeable;
	private final InputStream inputStream;
	private final OutputStream outputStream;


	@OnOpen
	public void open(final Session session) throws java.lang.Exception {
		logger.info("open: [" + session + "]");
		write(session, new byte[0], 0, 0);
	}

	@OnClose
	public void close(final Session session) throws java.io.IOException {
		logger.info("close: [" + session + "]");
		closeable.close();
	}

	@OnError
	public void onError(final Throwable error) {
		logger.info("onError: [" + error + "]");
	}

	@OnMessage
	public void handleMessage(final byte[] data, final Session session) throws java.io.IOException {
		outputStream.write(data);
	}

	public void write(final Session session, final byte[] data, final int offset, final int length) throws java.io.IOException {
		session.getBasicRemote().sendBinary(ByteBuffer.wrap(data, offset, length));
	}



	public static class Configuration implements java.io.Serializable {

		public Configuration(final String[] argv) {

			int port = -1;
			String url = null;
			boolean multiple = false;
			long maxIdleTimeout = 0L;

			for (int i = 0; i < argv.length; i++) {
				switch (argv[i]) {
					case "-port": port = Integer.parseInt(argv[++i]); break;
					case "-url": url = argv[++i]; break;
					case "-multiple": multiple = true; break;
					case "-idleTimeout": maxIdleTimeout = Long.parseLong(argv[++i]); break;
					default: throw new IllegalArgumentException("unknown argument: [" + argv[i] + "]");
				}
			}

			if (port <= 0)
				throw new IllegalArgumentException("port number is not valid");

			if ((url == null) || (url.trim().length() <= 0))
				throw new IllegalArgumentException("url is not valid");

			this.port = port;
			this.url = url;
			this.multiple = multiple;
			this.maxIdleTimeout = maxIdleTimeout;
		}


		private final int port;

		public int getPort() {
			return port;
		}


		private final String url;;

		public String getURL() {
			return url;
		}


		private final boolean multiple;

		public boolean isMultiple() {
			return multiple;
		}


		private final long maxIdleTimeout;

		public long getMaxIdleTimeout() {
			return maxIdleTimeout;
		}
	}


	public static void main(final String[] argv) throws java.lang.Exception {


		if (argv.length <= 0) {
			System.out.println(
				"parameters:\n" +
				"\t-port PORTNUMBER        - set port number to listen on\n" +
				"\t-url URL                - set url to connect to\n" +
				"\t-multiple               - accept multiple connections\n" +
				"\t-idleTimeout TIMEOUT    - set WebSocket maxIdleTimeout in milliseconds\n"
			);
			return;
		}

		final var configuration = new Configuration(argv);

	/*
	 	javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier(
			(hostname, session) -> {
				logger.info("verify: [" + hostname + ", " + session + "]");
				return true;
			}
		);
	*/

		logger.info("port: [" + configuration.getPort() + "] ...");
		logger.info("using url: [" + configuration.getURL() + "] ...");

		try (final var listener = new java.net.ServerSocket(configuration.getPort())) {

			logger.info("listening on port [" + configuration.getPort() + "] [" + listener + "] ...");

			for (int i = 0; configuration.isMultiple()? true: i < 1; i++) {

				final var socket = listener.accept();

				logger.info("accepted [" + socket + "]");

				final Runnable runnable = ()-> {

					logger.info("thread started ...");

					try (socket) {

						final var endpoint = new Client(
							socket::close,
							socket.getInputStream(),
							socket.getOutputStream()
						);

						logger.info("endpoint: [" + endpoint + "]");

						try (final var session = javax.websocket.ContainerProvider.getWebSocketContainer().connectToServer(
							endpoint, new java.net.URI(configuration.getURL())
						)) {

							logger.info("session: [" + session + "]");

							logger.info("setting maxIdleTimeout to [" + configuration.getMaxIdleTimeout() + "]");

							session.setMaxIdleTimeout(configuration.getMaxIdleTimeout());

							final var buffer = new byte[1024 * 100];

							for (;;) {

								final var read = socket.getInputStream().read(buffer);

								if (read < 0)
									break;

								endpoint.write(session, buffer, 0, read);
							}
						}

					} catch (java.lang.Exception exception) {
						logger.log(java.util.logging.Level.SEVERE, "exception", exception);
					}
				};

				if (configuration.isMultiple())
					(new Thread(runnable)).start();
				else
					runnable.run();
			}
		}

		logger.info("done.");
	}


}

