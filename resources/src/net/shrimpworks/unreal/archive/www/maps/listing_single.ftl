<#include "../_header.ftl">

	<section class="header" style='background-image: url("${static}/images/gametypes/${gametype.game.name}/${gametype.name}.png")'>
		<h1>
			Maps / ${gametype.game.name} / ${gametype.name}
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