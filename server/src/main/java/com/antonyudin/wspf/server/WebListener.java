
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


import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import javax.servlet.http.HttpServletRequest;


@javax.servlet.annotation.WebListener
public class WebListener implements ServletRequestListener {

	private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
		WebListener.class.getName()
	);


	@Override
	public void requestDestroyed(final ServletRequestEvent event) {
	}

	@Override
	public void requestInitialized(final ServletRequestEvent event) {
		final var session = ((HttpServletRequest) event.getServletRequest()).getSession();
		session.setAttribute(
			Endpoint.remoteAddressName,
			event.getServletRequest().getRemoteAddr()
		);
		logger.info("session: [" + session + "]");
	}

}

