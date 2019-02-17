<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>${siteName} / ${title}</title>
	<link rel="stylesheet" href="${staticPath(static)}/css/style.css">

	<link rel="icon" type="image/png" sizes="192x192" href="${staticPath(static)}/images/logo-192.png">
	<link rel="icon" type="image/png" sizes="96x96" href="${staticPath(static)}/images/logo-96.png">
	<link rel="icon" type="image/png" sizes="32x32" href="${staticPath(static)}/images/logo-32.png">
	<link rel="icon" type="image/png" sizes="16x16" href="${staticPath(static)}/images/logo-16.png">

	<link rel="favicon" type="image/png" href="${staticPath(static)}/images/logo-32.png">

	<meta property="og:title" content="${title}">
	<meta property="og:ste_name" content="${siteName} / ${title}">

	<#if ogDescription??>
		<meta name="description" content="${ogDescription?replace("\"", "&quot;")}">
		<meta property="og:description" content="${ogDescription?replace("\"", "&quot;")}">
	<#else>
		<meta name="description" content="Downloads and guides for maps, mutators, skins, voices, models and mods, for Unreal, Unreal Tournament, and Unreal Tournament 2004">
		<meta property="og:description" content="Downloads and guides for maps, mutators, skins, voices, models and mods, for Unreal, Unreal Tournament, and Unreal Tournament 2004">
	</#if>

	<#if ogImage??>
		<meta property="og:image" content="${ogImage}">
	<#else>
		<meta property="og:image" content="${staticPath(static)}/images/logo-96.png">
	</#if>

	<meta property="og:type" content="website">
</head>

<body>

<header>
	<div class="page">
		<h1>
			<a href="${staticPath(static)}/../index.html">
				<img src="${staticPath(static)}/images/logo.png" alt="Unreal Archive"/>
				<span class="a">UNREAL</span><span class="b">ARCHIVE</span>
			</a>
		</h1>
	</div>
</header>
