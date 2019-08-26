
keytool -import -alias ws  -file ${CERTIFICATE_1} -keystore keystore -storepass wspassword
keytool -import -alias ws1 -file ${CERTIFICATE_2} -keystore keystore -storepass wspassword

java									`
 "-Djavax.net.ssl.keyStore=keystore"					`
 "-Djavax.net.ssl.keyStorePassword=wspassword"				`
 "-Djavax.net.ssl.trustStore=keystore"					`
 "-Djavax.net.ssl.trustStorePassword=wspassword"			`
 -jar ../client-standalone/target/client-standalone-1.0.jar		`
	-port 123							`
	-url ${SERVER_URL}/vnc						`
	-multiple



