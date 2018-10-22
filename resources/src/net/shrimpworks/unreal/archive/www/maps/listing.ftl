<#include "_header.ftl">

	<#list page.maps as m>
		<#list m.map.attachments as a>
			<#if a.type == "IMAGE">
				<#assign headerbg>${a.url}</#assign>
				<#break>
			</#if>
		</#list>
	</#list>

	<section class="header" <#if headerbg??>style="background-image: url( '${headerbg?url_path?replace("https%3A", "https:")}' )"</#if>>
		<h1>
		${page.letter.gametype.game.name} / ${page.letter.gametype.name} / ${page.letter.letter} / pg ${page.number}
		</h1>
	</section>
	<article>
		<table class="maplist">
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

<#include "_footer.ftl">