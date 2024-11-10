open module unreal.archive.indexing {
	requires java.base;
	requires java.net.http;
	requires java.desktop;

	requires shrimpworks.unreal.packages;
	requires shrimpworks.unreal.dependencies;

	requires unreal.archive.common;
	requires unreal.archive.storage;
	requires unreal.archive.content;

	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires com.fasterxml.jackson.datatype.jsr310;

	exports org.unrealarchive.indexing;
}
