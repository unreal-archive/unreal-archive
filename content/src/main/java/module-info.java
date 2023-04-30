module unreal.archive.content {
	requires java.base;
	requires java.desktop;

	requires unreal.archive.common;

	requires com.fasterxml.jackson.annotation;

	exports net.shrimpworks.unreal.archive.content;
	exports net.shrimpworks.unreal.archive.content.addons;
	exports net.shrimpworks.unreal.archive.content.docs;
	exports net.shrimpworks.unreal.archive.content.managed;
	exports net.shrimpworks.unreal.archive.content.wiki;
}
