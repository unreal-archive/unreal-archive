<#include "../../_header.ftl">

	<section class="header" style='background-image: url("${static}/images/games/${game.name}.png")'>
		<h1>
			<a href="${siteRoot}/index.html">Models</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		</h1>
	</section>

	<article class="list">
		<table class="models">
			<thead>
			<tr>
				<th>Model</th>
				<th>Author</th>
			</tr>
			</thead>
			<tbody>
				<#list models as m>
				<tr>
					<td><a href="${relUrl(game.path, m.path + ".html")}">${m.model.name}</a></td>
					<td>${m.model.author}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</article>

<#include "../../_footer.ftl">