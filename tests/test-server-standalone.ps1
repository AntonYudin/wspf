
keytool -genkey -alias sitename -keyalg RSA -keystore server-standalone.keystore -keysize 2048 -storepass wspassword

java								`
 "-Djavax.net.ssl.keyStore=keystore"				`
 "-Djavax.net.ssl.keyStorePassword=wspassword"			`
 "-Djavax.net.ssl.trustStore=keystore"				`
 "-Djavax.net.ssl.trustStorePassword=wspassword"		`
 -jar ../server-standalone/target/server-standalone-1.0.jar	`
	-port 8080						`
	-keyStorePath server-standalone.keystore		`
	-keyStorePassword wspassword				`
	-pass "identity=ssh;remote=localhost:81"


