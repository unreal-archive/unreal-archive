<#include "../../_header.ftl">

	<#assign game=page.letter.game>

	<section class="header" style='background-image: url("${static}/images/games/${game.name}.png")'>
		<h1>
			<a href="${siteRoot}/index.html">Skins</a>
			/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
			/ ${page.letter.letter}
			/ pg ${page.number}
		</h1>
	</section>

	<article class="list">

		<nav class="letters">
			<#list game.letters as k, letter><a href="${relUrl(root, letter.path + "/index.html")}"<#if letter.letter == page.letter.letter>class="active"</#if>>${letter.letter}</a></#list>
		</nav>

		<#if page.letter.pages?size gt 1>
			<nav class="pages">
				<#list page.letter.pages as pg><a href="${relUrl(root, pg.path + "/index.html")}" <#if pg.number == page.number>class="active"</#if>>${pg.number}</a></#list>
			</nav>
		</#if>

		<table class="skins">
			<thead>
			<tr>
				<th>Skin</th>
				<th>Author</th>
			</tr>
			</thead>
			<tbody>
				<#list page.skins as s>
				<tr class="${s?item_parity}">
					<td nowrap="nowrap"><a href="${relUrl(root, s.path + ".html")}">${s.skin.name}</a></td>
					<td>${s.skin.author}</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</article>

<#include "../../_footer.ftl">