<#compress><!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">

	<title>${siteName} / ${title}</title>
	<link rel="stylesheet" href="${staticPath()}/css/all.css?v=20230806">
	<#if extraCss??>
		<link rel="stylesheet" href="${staticPath()}/css/${extraCss}">
	</#if>

	<link rel="icon" type="image/png" sizes="192x192" href="${staticPath()}/images/logo-192.png">
	<link rel="icon" type="image/png" sizes="96x96" href="${staticPath()}/images/logo-96.png">
	<link rel="icon" type="image/png" sizes="32x32" href="${staticPath()}/images/logo-32.png">
	<link rel="icon" type="image/png" sizes="16x16" href="${staticPath()}/images/logo-16.png">

	<link rel="favicon" type="image/png" href="${staticPath()}/images/logo-32.png">

	<#if features.latest>
	<link rel="alternate" type="application/rss+xml" title="Unreal Archive Latest Additions" href="${relPath(siteRoot + "/latest/feed.xml")}" />
  </#if>

	<meta property="og:title" content="${title}">
	<meta property="og:site_name" content="${siteName}">

	<#if !(ogDescription??)>
		<#assign ogDescription="Downloads and guides for maps, mutators, skins, voices, models and mods, for the original classic Unreal, Unreal Tournament (UT99), and Unreal Tournament 2004 (UT2004) games">
	</#if>

	<meta name="description" content="${ogDescription?replace("\"", "&quot;")}">
	<meta property="og:description" content="${ogDescription?replace("\"", "&quot;")}">

	<#if ogImage??>
		<meta property="og:image" content="${ogImage}">
	<#else>
		<meta property="og:image" content="${staticPath()}/images/logo-96.png">
	</#if>

	<meta property="og:type" content="website">

  <#if (schemaItemName!"")?length gt 0 && (schemaItemAuthor!"")?length gt 0>
		<script type="application/ld+json">{
				"@context": "https://schema.org",
				"@type": "DigitalDocument",
				"abstract": "${(schemaItemDesc!ogDescription)?replace("\"", "\\\"")?replace("\\", "\\\\")}",
				"author": "${schemaItemAuthor?replace("\"", "\\\"")?replace("\\", "\\\\")}",
				"image": "${headerbg}",
				"name": "${schemaItemName?replace("\"", "\\\"")?replace("\\", "\\\\")}",
				"datePublished": "${schemaItemDate}"
		}</script>
	</#if>
</head>

<body>
<#include "title.ftl">

<div class="mainpage"></#compress>