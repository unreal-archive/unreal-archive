module unreal.archive.storage {
	requires java.base;
	requires java.net.http;

	requires unreal.archive.common;

	requires okhttp3;
	requires minio;
	requires org.apache.commons.compress;

	exports org.unrealarchive.storage;
}
