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

	<#if description??>
		<meta name="description" content="${description?replace("\"", "&quot;")}">
	</#if>
</head>

<body>

<header>
	<div class="page">
		<h1>
			<a href="${static}/../index.html">
				<img src="${static!"static"}/images/logo.png"/>
				<span class="a">UNREAL</span><span class="b">ARCHIVE</span>
			</a>
		</h1>
	</div>
</header>
