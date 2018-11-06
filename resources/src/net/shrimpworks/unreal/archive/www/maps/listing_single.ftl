<#include "../_header.ftl">

	<#assign game=gametype.game>

	<section class="header" style='background-image: url("${static}/images/gametypes/${game.name}/${gametype.name}.png"), url("${static}/images/games/${game.name}.png")'>
		<h1>
			<a href="${siteRoot}/index.html">Maps</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ ${gametype.name}
		</h1>
	</section>
	<article class="list">
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
				<#list maps as m>
				<tr>
					<td><a href="${relUrl(gametype.path, m.path + ".html")}">${m.map.name}</a></td>
					<td>${m.map.title}</td>
					<td>${m.map.author}</td>
					<td>${m.map.playerCount}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</article>

<#include "../_footer.ftl">