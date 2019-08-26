
java -jar ../server-standalone/target/server-standalone-1.0.jar				\
	-port 8443									\
	-context "/wspf/web/"								\
	-keyStorePath "server-standalone.keystore"					\
	-keyStorePassword "wspassword"							\
	-pass "identity=ssh;remote=localhost:22"					\
	-pass "identity=vnc;remote=localhost:5900"					\


