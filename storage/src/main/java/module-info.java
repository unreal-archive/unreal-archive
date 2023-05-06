module unreal.archive.storage {
	requires java.base;
	requires java.net.http;

	requires unreal.archive.common;

	requires b2.sdk.core;
	requires b2.sdk.httpclient;
	requires okhttp3;
	requires minio;

	exports net.shrimpworks.unreal.archive.storage;
}
