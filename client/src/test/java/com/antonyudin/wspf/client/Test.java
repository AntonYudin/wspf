
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


import java.util.logging.Logger;

import java.util.List;
import java.util.ArrayList;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class Test implements java.io.Serializable {


	private final static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(
		Test.class.getName()
	);

	@org.junit.jupiter.api.Test
	public void test() throws java.lang.Exception {


		Client.main(
			new String[] {"-p", "123", "-u", "ws://localhost/wspf/web/passws/local"}
		);

	//	Client4.main(new String[] {"123", "ws://localhost/pass"});
	}

}


