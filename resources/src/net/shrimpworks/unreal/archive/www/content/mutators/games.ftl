<#assign ogDescription="Custom gameplay mutators for Unreal, Unreal Tournament, and Unreal Tournament 2004">
<#assign ogImage="${staticPath(static)}/images/contents/skins.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=["${staticPath(static)}/images/contents/mutators.png"]>
		Mutators
	</@heading>

	<@content class="biglist">
		<ul>
		<#list games.games as k, v>
			<li style='background-image: url("${staticPath(static)}/images/games/${v.name}.png")'>
				<span class="meta">${v.mutators}</span>
				<a href="${v.path}/index.html">${v.name}</a>
			</li>
		</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">