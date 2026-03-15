open module unreal.archive.tools {
	requires java.base;
	requires java.net.http;
	requires java.desktop;

	requires shrimpworks.unreal.packages;
	requires shrimpworks.unreal.dependencies;

	requires unreal.archive.common;
	requires unreal.archive.content;
	requires unreal.archive.storage;
	requires unreal.archive.indexing;
}
