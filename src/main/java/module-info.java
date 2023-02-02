open module unreal.archive {
	requires java.base;
	requires java.net.http;
	requires java.desktop;

	requires shrimpworks.unreal.packages;
	requires shrimpworks.unreal.dependencies;

	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires com.fasterxml.jackson.datatype.jsr310;

	requires b2.sdk.core;
	requires b2.sdk.httpclient;
	requires okhttp3;
	requires minio;

	requires freemarker;

	requires org.commonmark;

	requires org.jsoup;

//	exports net.shrimpworks.unreal.archive;
//	exports net.shrimpworks.unreal.archive.content;
//	exports net.shrimpworks.unreal.archive.docs;
//	exports net.shrimpworks.unreal.archive.managed;
//	exports net.shrimpworks.unreal.archive.storage;
//	exports net.shrimpworks.unreal.archive.wiki;
//	exports net.shrimpworks.unreal.archive.content.gametypes;
//	exports net.shrimpworks.unreal.archive.content.mappacks;
//	exports net.shrimpworks.unreal.archive.content.maps;
//	exports net.shrimpworks.unreal.archive.content.models;
//	exports net.shrimpworks.unreal.archive.content.mutators;
//	exports net.shrimpworks.unreal.archive.content.skins;
//	exports net.shrimpworks.unreal.archive.content.voices;
//	exports net.shrimpworks.unreal.archive.www;
//	exports net.shrimpworks.unreal.archive.www.managed;
//	exports net.shrimpworks.unreal.archive.www.wikis;
//	exports net.shrimpworks.unreal.archive.www.docs;
//	exports net.shrimpworks.unreal.archive.www.search;
//	exports net.shrimpworks.unreal.archive.www.submit;
//	exports net.shrimpworks.unreal.archive.www.content;
//	exports net.shrimpworks.unreal.archive.www.content.authors;
//	exports net.shrimpworks.unreal.archive.www.content.files;
//	exports net.shrimpworks.unreal.archive.www.content.gametypes;
//	exports net.shrimpworks.unreal.archive.www.content.latest;
//	exports net.shrimpworks.unreal.archive.www.content.mappacks;
//	exports net.shrimpworks.unreal.archive.www.content.maps;
//	exports net.shrimpworks.unreal.archive.www.content.models;
//	exports net.shrimpworks.unreal.archive.www.content.mutators;
//	exports net.shrimpworks.unreal.archive.www.content.packages;
//	exports net.shrimpworks.unreal.archive.www.content.skins;
//	exports net.shrimpworks.unreal.archive.www.content.voices;
}