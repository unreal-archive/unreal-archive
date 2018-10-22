<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<title>${title}</title>
	<style>
		body {
			padding: 0;
			margin: 0;

			font-family: sans-serif;
			font-size: 0.9em;
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
			box-shadow: inset 0 0 100px 0 rgba(0, 0, 0, 0.75);
			background: gray center;
			background-size: cover;
		}

		section.header h1 {
			position: absolute;
			bottom: 0;
			color: white;
			text-shadow: -2px -2px 0 #000, 2px -2px 0 #000, -2px 2px 0 #000, 2px 2px 0 #000;
		}

		.maplist {
			width: 100%;
		}

		.maplist tr.odd {
			background-color: #d0d9e0;
		}
	</style>
</head>

<body>

<div class="page">
	<header>
	</header>
