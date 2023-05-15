module unreal.archive.common {
	requires java.base;
	requires java.net.http;

	// required for TLS 1.3
	requires jdk.crypto.ec;

	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires com.fasterxml.jackson.datatype.jsr310;

	exports org.unrealarchive.common;
}
