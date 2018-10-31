<#include "../_header.ftl">

	<section class="header" style='background-image: url("${static}/images/games/${page.game.name}.png")'>
		<h1>
			Map Packs / ${page.game.name} / pg ${page.number}
		</h1>
	</section>
	<article class="list">

		<nav class="pages">
			<#list page.game.pages as pg><a href="${relUrl(root, pg.path + "/index.html")}" <#if pg.number == page.number>class="active"</#if>>${pg.number}</a></#list>
		</nav>

		<table class="mappacks">
			<thead>
			<tr>
				<th>Name</th>
				<th>Author</th>
				<th>Maps</th>
			</tr>
			</thead>
			<tbody>
				<#list page.packs as p>
				<tr>
					<td><a href="${relUrl(root, p.path + ".html")}">${p.pack.name}</a></td>
					<td>${p.pack.author}</td>
					<td>${p.pack.maps?size}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</article>

<#include "../_footer.ftl">