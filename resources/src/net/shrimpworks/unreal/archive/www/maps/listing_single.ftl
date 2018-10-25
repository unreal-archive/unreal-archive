<#include "_header.ftl">

	<#list maps as m>
		<#list m.map.attachments as a>
			<#if a.type == "IMAGE">
				<#assign headerbg=a.url?url_path?replace('https%3A', 'https:')>
				<#break>
			</#if>
		</#list>
		<#if headerbg??><#break></#if>
	</#list>

	<section class="header" <#if headerbg??>style='background-image: url("${headerbg}")'</#if>>
		<h1>
		${gametype.game.name} / ${gametype.name}
		</h1>
	</section>
	<article class="maplist">
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

<#include "_footer.ftl">