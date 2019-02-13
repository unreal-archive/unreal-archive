<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>${siteName} / ${title}</title>
	<link rel="stylesheet" href="${static!"static"}/css/style.css">

	<link rel="icon" type="image/png" sizes="192x192" href="${static!"static"}/images/logo-192.png">
	<link rel="icon" type="image/png" sizes="96x96" href="${static!"static"}/images/logo-96.png">
	<link rel="icon" type="image/png" sizes="32x32" href="${static!"static"}/images/logo-32.png">
	<link rel="icon" type="image/png" sizes="16x16" href="${static!"static"}/images/logo-16.png">

	<link rel="favicon" type="image/png" href="${static!"static"}/images/logo-32.png">

	<meta property="og:title" content="${title}">
	<meta property="og:ste_name" content="${siteName}">

	<#if ogDescription??>
		<meta name="description" content="${ogDescription?replace("\"", "&quot;")}">
		<meta property="og:description" content="${ogDescription?replace("\"", "&quot;")}">
	</#if>

	<#if ogImage??>
		<meta property="og:image" content="${ogImage}">
	</#if>
	<#--<meta property="og:url" content="">-->
	<meta property="og:type" content="website">
</head>

<body>

<header>
	<div class="page">
		<h1>
			<a href="${static}/../index.html">
				<img src="${static!"static"}/images/logo.png" alt="Unreal Archive"/>
				<span class="a">UNREAL</span><span class="b">ARCHIVE</span>
			</a>
		</h1>
	</div>
</header>
