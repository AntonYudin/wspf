
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


import java.util.List;
import java.util.ArrayList;

import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.servlet.ServletContextHandler;

import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import org.eclipse.jetty.http.HttpVersion;

import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;


public class Server {

	private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
		Server.class.getName()
	);


	public static class Configuration implements java.io.Serializable {

		public Configuration(final String[] argv) {

			int port = -1;
			String keyStorePath = null;
			String keyStorePassword = null;
			String context = "/";

			for (int i = 0; i < argv.length; i++) {
				switch (argv[i]) {
					case "-port": port = Integer.parseInt(argv[++i]); break;
					case "-keyStorePath": keyStorePath = argv[++i]; break;
					case "-keyStorePassword": keyStorePassword = argv[++i]; break;
					case "-pass": passes.add(argv[++i]); break;
					case "-context": context = argv[++i]; break;
					default: throw new IllegalArgumentException("unknown argument: [" + argv[i] + "]");
				}
			}

			if (port <= 0)
				throw new IllegalArgumentException("port number is not valid");

			this.port = port;
			this.keyStorePath = keyStorePath;
			this.keyStorePassword = keyStorePassword;
			this.context = context;
		}


		private final List<String> passes = new ArrayList<>();

		public List<String> getPasses() {
			return passes;
		}


		private final int port;

		public int getPort() {
			return port;
		}


		private final String keyStorePath;

		public String getKeyStorePath() {
			return keyStorePath;
		}


		private final String keyStorePassword;

		public String getKeyStorePassword() {
			return keyStorePassword;
		}


		private final String context;

		public String getContext() {
			return context;
		}
	}


	public static void main(final String[] argv) {

		if (argv.length <= 0) {
			System.out.println(
				"parameters:\n" +
				"\t-port PORTNUMBER                - set port number to listen on\n" +
				"\t-keyStorePath PATH              - set path to the key store\n" +
				"\t-keyStorePassword PASSWORD      - set key store password\n" +
				"\t-pass \"identity;host;port\"      - port forwarding configuration\n" +
				"\t-context CONTEXT                - servlet context prefix\n"
			);
			return;
		}
	

		final var configuration = new Configuration(argv);

		final var server = new org.eclipse.jetty.server.Server();

		final var sslContextFactory = new SslContextFactory.Server();

		sslContextFactory.setKeyStorePath(configuration.getKeyStorePath());

		sslContextFactory.setKeyStorePassword(configuration.getKeyStorePassword());

		final var httpsConfig = new HttpConfiguration();

		httpsConfig.addCustomizer(new SecureRequestCustomizer());


		final var connector = new ServerConnector(
			server,
			new SslConnectionFactory(
				sslContextFactory,
				HttpVersion.HTTP_1_1.asString()
			),
			new HttpConnectionFactory(httpsConfig)
		);

		connector.setPort(configuration.getPort());

		server.addConnector(connector);

		final var context = new ServletContextHandler(ServletContextHandler.SESSIONS);

		context.setContextPath(configuration.getContext());

		context.addEventListener(
			new javax.servlet.ServletContextListener() {
				@Override
				public void contextInitialized(final javax.servlet.ServletContextEvent event) {
					logger.info("contextInitialized: [" + event + "]");

					for (int i = 0; i < configuration.getPasses().size(); i++)
						event.getServletContext().setInitParameter("pass." + i, configuration.getPasses().get(i));

					event.getServletContext().addListener(new WebListener());
				}

				@Override
				public void contextDestroyed(final javax.servlet.ServletContextEvent event) {
					logger.info("contextDestroyed: [" + event + "]");
				}
			}
		);

		server.setHandler(context);

		try {

			WebSocketServerContainerInitializer.configure(
				context, (servletContext, serverContainer) -> {
					logger.info("wscontainer -> [" + serverContainer + "]");
					serverContainer.addEndpoint(
						com.antonyudin.wspf.server.Endpoint.class
					);
				}
			);

			server.start();

	//		server.dump(System.err);

			server.join();

		} catch (final Throwable throwable) {
			throwable.printStackTrace(System.err);
		}

	}

}

