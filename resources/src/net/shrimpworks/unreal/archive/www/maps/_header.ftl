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
			font-size: 1.8em;
			position: absolute;
			bottom: 0;
			color: white;
			text-shadow: -2px -2px 0 #000, 2px -2px 0 #000, -2px 2px 0 #000, 2px 2px 0 #000;
			padding: 0;
			margin: 10px 20px;
		}

		.maplist nav {
			text-align: justify;
			text-align-last: justify;
			text-justify: distribute;
			font-size: 1.2em;
			font-weight: bold;
			margin: 10px 0;
			padding: 5px 0;
		}

		.maplist nav.letters a {
			background-color: #dee3e9;
			border: 1px solid #4D7A97;
			padding: 5px;
			margin: 0 2px;
			text-decoration: none;
		}

		.maplist nav.pages a {
			background-color: #dee3e9;
			border: 1px solid #4D7A97;
			padding: 5px;
			margin: 0 2px;
			text-decoration: none;
		}

		.maplist nav.letters a.active {
			background-color: white;
		}

		.maplist nav.pages a.active {
			background-color: white;
		}

		table.maps {
			width: 100%;
		}

		table.maps tr.odd {
			background-color: #d0d9e0;
		}

		table.maps td {
			padding: 5px 0;
		}
	</style>
</head>

<body>

<div class="page">
	<header>
	</header>
