<#include "../_header.ftl">
<#include "../content/macros.ftl">

	<@heading bg=["${static}/images/contents/maps.png"]>
		Maps
	</@heading>

	<@content class="biglist">
		<ul>
		<#list games.games as k, v>
			<li style='background-image: url("${static}/images/games/${v.name}.png")'>
				<span class="meta">${v.maps}</span>
				<a href="${v.path}/index.html">${v.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../_footer.ftl">