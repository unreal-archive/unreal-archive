<#include "../_header.ftl">

	<section class="header" style='background-image: url("${static}/images/games/${game.name}.png")'>
		<h1>
			Map Packs / ${game.name}
		</h1>
	</section>
	<article class="list">
		<table class="mappacks">
			<thead>
			<tr>
				<th>Name</th>
				<th>Author</th>
				<th>Maps</th>
			</tr>
			</thead>
			<tbody>
				<#list packs as p>
				<tr>
					<td><a href="${relUrl(game.path, p.path + ".html")}">${p.pack.name}</a></td>
					<td>${p.pack.author}</td>
					<td>${p.pack.maps?size}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</article>

<#include "../_footer.ftl">