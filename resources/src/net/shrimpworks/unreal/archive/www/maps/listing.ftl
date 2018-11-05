<#include "../_header.ftl">

	<#assign game=page.letter.gametype.game>
	<#assign gametype=page.letter.gametype>

	<section class="header" style='background-image: url("${static}/images/gametypes/${game.name}/${gametype.name}.png"), url("${static}/images/games/${game.name}.png")'>
		<h1>
			<a href="${relUrl(game.path, siteRoot)}/index.html">Maps</a>
			/ <a href="${relUrl(gametype.path, siteRoot)}/index.html">${game.name}</a>
			/ <a href="${relUrl(page.path, siteRoot)}/index.html">${gametype.name}</a> <!-- FIXME -->
			/ ${page.letter.letter}
			/ pg ${page.number}
		</h1>
	</section>

	<article class="list">

		<nav class="letters">
			<#list gametype.letters as k, letter><a href="${relUrl(root, letter.path + "/index.html")}"<#if letter.letter == page.letter.letter>class="active"</#if>>${letter.letter}</a></#list>
		</nav>

		<#if page.letter.pages?size gt 1>
			<nav class="pages">
				<#list page.letter.pages as pg><a href="${relUrl(root, pg.path + "/index.html")}" <#if pg.number == page.number>class="active"</#if>>${pg.number}</a></#list>
			</nav>
		</#if>

		<table class="maps">
			<thead>
			<tr>
				<th>Map</th>
				<th>Title</th>
				<th>Author</th>
				<th>Players</th>
			</tr>
			</thead>
			<tbody>
				<#list page.maps as m>
				<tr class="${m?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, m.path + ".html")}">${m.map.name}</a></td>
					<td>${m.map.title}</td>
					<td>${m.map.author}</td>
					<td>${m.map.playerCount}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</article>

<#include "../_footer.ftl">