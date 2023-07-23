<#assign extraCss="search.css"/>
<#include "../_header.ftl">
<#include "../macros.ftl">

<@content class="readable">

	<h1>
		Search
	</h1>

	<form id="search-form">
		<span>
			<select id="kind">
				<option value="content">Downloads &amp; Content</option>
				<option value="wiki">Wikis</option>
			</select>
			<input type="search" id="q" autofocus="autofocus" />
			<button><img src="${staticPath()}/images/icons/search.svg" alt="Search"/> Search</button>
		</span>
		<span>
			<label for="pageSize"> Results per page</label>
			<select id="pageSize">
				<option value="30">30</option>
				<option value="50">50</option>
				<option value="100">100</option>
			</select>
		</span>
		<span>
			<input type="checkbox" id="compact"> <label for="compact">Compact Results</label>
		</span>
		<span>
			<a href="#" id="syntax-toggle"><img src="${staticPath()}/images/icons/info.svg" alt="Info"/> Advanced Search Help</a>
		</span>

	</form>

	<div id="syntax">
		<h2>Advanced Search Help</h2>
		<div class="row">
			<div class="example"><code>giant</code></div>
			<div class="explain">Find all content with the whole word "giant" in their name, author, or other available information.</div>
		</div>
		<div class="row">
			<div class="example"><code>giant*</code></div>
			<div class="explain">Find all content with a word starting with "giant".</div>
		</div>
		<div class="row">
			<div class="example"><code>giant* -*room*</code></div>
			<div class="explain">Find all content with a word starting with "giant", and NOT ending in "room".
				For example this will exclude something named "Giant Bedroom".</div>
		</div>
		<div class="row">
			<div class="example"><code>bedroom | bathroom</code></div>
			<div class="explain">Find all content containing the full words "bedroom" OR "bathroom".</div>
		</div>
		<div class="row">
			<div class="example"><code>bed* | bath*</code></div>
			<div class="explain">Find all content containing words starting with "bed" OR "bath".</div>
		</div>
		<div class="row">
			<div class="example"><code>deck* ~*16*</code></div>
			<div class="explain">Find all content containing words starting with "deck" and optionally, "16".
				Results containing "16" will be ranked higher.</div>
		</div>
	</div>

	<div id="search-results">
	</div>

	<div id="search-nav">
		<button id="nav-back"><img src="${staticPath()}/images/icons/chevron-left.svg" alt="Previous"/> Previous</button>
		<span id="nav-text"></span>
		<button id="nav-next">Next <img src="${staticPath()}/images/icons/chevron-right.svg" alt="Next"/></button>
	</div>

</@content>

<script type="application/javascript">
	const searchRoots = {
		"content": "./api/ua",
		"wiki": "./api/uaw",
		"files": "./api/uaf",
  };

	let pageSize = 30;

	document.addEventListener("DOMContentLoaded", function() {
		const searchForm = document.querySelector('#search-form');

		const searchKind = document.querySelector('#kind');
		const searchQ = document.querySelector('#q');
		const results = document.querySelector('#search-results');
		const pageSizeSelect = document.querySelector('#pageSize');
		const compact = document.querySelector('#compact');

		const navBack = document.querySelector('#nav-back');
		const navNext = document.querySelector('#nav-next');
		const navText = document.querySelector('#nav-text');

	  const syntaxToggle = document.querySelector('#syntax-toggle');
	  const syntax = document.querySelector('#syntax');

		let currentKind;
		let currentQuery;

		searchForm.addEventListener('submit', e => {
			search(searchKind.value, searchQ.value);
			e.preventDefault();
			return false
		});

		const navClick = function(e) {
			search(currentKind, currentQuery, e.target.dataset.offset, pageSize);
		};

		navBack.addEventListener('click', navClick);
		navNext.addEventListener('click', navClick);

		pageSizeSelect.addEventListener('change', (e) => {
			pageSize = e.target.value;
		});

		compact.addEventListener('change', (e) => {
			if (e.target.checked) results.classList.add("compact");
			else results.classList.remove("compact");
		});

		// set initial values, on page load
		compact.dispatchEvent(new Event('change'));
		pageSizeSelect.dispatchEvent(new Event('change'));

	  syntaxToggle.addEventListener('click', () => {
				syntax.classList.toggle("open")
		});

		function search(kind, query, offset = 0, limit = pageSize) {
			currentKind = kind;
			currentQuery = query;
			window.history.replaceState(null, null, "?kind=" + kind + "&q=" + query);

			while (results.childNodes.length > 0) results.removeChild(results.childNodes[0]);
			const loading = document.createElement("h2");
			loading.innerText = "... Searching ...";
			results.append(loading);

			if (!searchRoots[currentKind]) currentKind = "content";
			const searchRoot = searchRoots[currentKind];

			// allows for searching by map literal names, such as "DM-MapName", without RediSearch excluding "-MapName" from the results
			let q = query.replace(/([A-Za-z]{2,3})-/g, "$1\\-");

			const url = searchRoot + "/search?q=" + encodeURIComponent(q) + "&offset=" + offset + "&limit=" + limit;
			console.log("Query URL is ", url);

			fetch(url)
				.then((response) => {
					results.removeChild(loading);
					return response.json();
				})
				.then((data) => {
					if (data.totalResults === 0) {
						noResult();
					} else {
						data.docs.forEach(d => addResult(d));
					}

					navigation(data.totalResults, data.offset, data.limit);
				});
		}

		function noResult() {
			while (results.childNodes.length > 0) results.removeChild(results.childNodes[0]);
			const loading = document.createElement("h2");
			loading.innerText = "No results matching your search";
			results.append(loading);
		}

		function addResult(result) {
			if (result.fields.wiki) addWikiResult(result);
			else addContentResult(result);
		}

	  function addWikiResult(result) {
		  const image = document.createElement("img");
		  if (!result.fields.image) {
			  image.setAttribute("src", "${staticPath()}/images/none-document-small.png");
		  } else {
			  image.setAttribute("src", result.fields.image);
		  }
		  image.setAttribute("alt", result.fields.name.replace(/\\-/g, "-"));

		  const imageDiv = document.createElement("div");
		  imageDiv.classList.add('image');
		  imageDiv.append(image);

		  const link = document.createElement("a");
		  link.setAttribute("href", result.fields.url);
		  link.innerText = result.fields.name.replace(/\\-/g, "-");
		  const title = document.createElement("h2");
		  title.append(link);

			const description = document.createElement("div");
			description.classList.add('description');
			description.innerText = "From Wiki " + result.fields.wiki;

		  const info = document.createElement("div");
		  info.classList.add('info');
		  info.append(title, description);

		  const resultRow = document.createElement("div");
		  resultRow.classList.add('result', 'wiki');
		  resultRow.append(imageDiv, info);
		  results.append(resultRow);
	  }

		function addContentResult(result) {
			const image = document.createElement("img");
			if (result.fields.image.length === 0) {
				image.setAttribute("src", "${staticPath()}/images/none.png");
			} else {
				image.setAttribute("src", result.fields.image);
			}
			image.setAttribute("alt", result.fields.name.replace(/\\-/g, "-"));

			const imageDiv = document.createElement("div");
			imageDiv.classList.add('image');
			imageDiv.append(image);

			const game = document.createElement("img");
			game.setAttribute("src", "${staticPath()}/images/games/icons/" + result.fields.game + ".png");
			game.setAttribute("alt", result.fields.game);
			game.setAttribute("title", result.fields.game);
			const link = document.createElement("a");
			link.setAttribute("href", result.fields.url);
			link.innerText = result.fields.name.replace(/\\-/g, "-");
			const title = document.createElement("h2");
			title.append(game, link);

			const author = document.createElement("div");
			author.classList.add('author');
			author.innerText = result.fields.type + " by " + result.fields.author.replace(/\\-/g, "-");

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

		function navigation(totalResults = 0, offset = 0, limit = 0) {
			navBack.disabled = offset === 0;
			navNext.disabled = (totalResults <= limit) || (offset + limit > totalResults);
			navText.innerText = totalResults === 0
				? "-"
				: `Showing ${"$"}{offset + 1} to ${"$"}{Math.min(offset + limit, totalResults)} of ${"$"}{totalResults} results`;
			if (!navBack.disabled) navBack.dataset.offset = offset - limit;
			if (!navNext.disabled) navNext.dataset.offset = offset + limit;
		}

		// initialise based on passed-in query string
		const urlParams = new URLSearchParams(window.location.search);
		const kindString = urlParams.get('kind');
		const searchString = urlParams.get('q');
		if (searchString && searchString !== '') {
			searchKind.value = kindString;
			searchQ.value = searchString;
			search(kindString, searchString);
		} else {
			// initialise navigation buttons in disabled state
			navigation();
		}
	});

</script>

<#include "../_footer.ftl">
