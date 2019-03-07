module net.shrimpworks.unreal.archive {
	requires net.shrimpworks.unreal.packages;

	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires java.desktop;
	requires jackson.annotations;
	requires txtmark;
	requires freemarker;
	requires org.apache.httpcomponents.httpclient;
	requires org.apache.httpcomponents.httpclient.fluent;
	requires jsoup;
	requires org.apache.httpcomponents.httpcore;
	requires b2.sdk.core;
	requires b2.sdk.httpclient;

	exports net.shrimpworks.unreal.archive;
	exports net.shrimpworks.unreal.archive.content;
	exports net.shrimpworks.unreal.archive.content.mappacks;
	exports net.shrimpworks.unreal.archive.content.maps;
	exports net.shrimpworks.unreal.archive.content.models;
	exports net.shrimpworks.unreal.archive.content.mutators;
	exports net.shrimpworks.unreal.archive.content.skins;
	exports net.shrimpworks.unreal.archive.content.voices;
}