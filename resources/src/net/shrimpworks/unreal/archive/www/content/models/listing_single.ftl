<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${static}/images/games/${game.name}.png"]>
		<a href="${siteRoot}/index.html">Models</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
	</@heading>

	<@content class="list">
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
	</@content>

<#include "../../_footer.ftl">