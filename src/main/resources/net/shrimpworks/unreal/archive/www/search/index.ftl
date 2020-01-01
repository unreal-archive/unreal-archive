<#assign extraCss="search.css"/>
<#include "../_header.ftl">
<#include "../macros.ftl">

<@content class="readable">

	<h1>
		Search
	</h1>

	<div id="search-form">
		<input type="text" id="q"/>
		<button>Search</button>
	</div>

	<div id="search-results">
	</div>

</@content>

<script type="application/javascript">
	let searchRoot = "http://localhost:8080/search/api";

	document.addEventListener("DOMContentLoaded", function() {
		const searchQ = document.querySelector('#q');
		const searchButton = document.querySelector('#search-form button');
		const results = document.querySelector('#search-results');

		searchButton.addEventListener('click', () => {
			search(searchQ.value);
		});

		function search(query, offset = 0, limit = 20) {
			while (results.childNodes.length > 0) results.removeChild(results.childNodes[0]);
			const loading = document.createElement("h2");
			loading.innerText = "... Searching ...";
			results.append(loading);

			fetch(searchRoot + "/search?q=" + query + "&offset=" + offset + "&limit=" + limit)
				.then((response) => {
					results.removeChild(loading);
					return response.json();
				})
				.then((data) => {
					console.log("got results", data.totalResults);
					data.docs.forEach(d => addResult(d));
				});
		}

		function addResult(result) {
			const image = document.createElement("img");
			if (result.fields.image.length === 0) {
				image.setAttribute("src", "${staticPath()}/images/none.png");
			} else {
				image.setAttribute("src", result.fields.image);
			}
			image.setAttribute("alt", result.fields.name);

			const imageDiv = document.createElement("div");
			imageDiv.classList.add('image');
			imageDiv.append(image);

			const game = document.createElement("img");
			game.setAttribute("src", "${staticPath()}/images/games/icons/" + result.fields.game + ".png");
			game.setAttribute("alt", result.fields.game);
			const link = document.createElement("a");
			link.setAttribute("href", result.fields.url);
			link.innerText = result.fields.name;
			const title = document.createElement("h2");
			title.append(game, link);

			const author = document.createElement("div");
			author.classList.add('author');
			author.innerText = "by " + result.fields.author;

			const description = document.createElement("div");
			description.classList.add('description');
			description.innerText = result.fields.description;

			const info = document.createElement("div");
			info.classList.add('info');
			info.append(title, author, description);

			const resultRow = document.createElement("div");
			resultRow.classList.add('result');
			resultRow.append(imageDiv, info);
			results.append(resultRow);
		}
	});

</script>

<#include "../_footer.ftl">