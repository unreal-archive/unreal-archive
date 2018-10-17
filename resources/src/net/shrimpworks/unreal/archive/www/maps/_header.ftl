<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>${title}</title>
	<style>
		body {
			padding: 0;
			margin: 0;
		}

		.page {
			max-width: 1024px;
			margin-left: auto;
			margin-right: auto;
		}

		header {
			height: 100px;
		}

		section.header {
			position: relative;
			height: 150px;
			background-size: cover;
			background-color: gray;
			box-shadow: inset 0px 0px 100px 0px rgba(0, 0, 0, 0.75);
		}

		section.header h1 {
			position: absolute;
			bottom: 0;
			color: white;
			text-shadow: -2px -2px 0 #000, 2px -2px 0 #000, -2px 2px 0 #000, 2px 2px 0 #000;
		}
	</style>
</head>

<body>

<div class="page">
	<header>
	</header>
