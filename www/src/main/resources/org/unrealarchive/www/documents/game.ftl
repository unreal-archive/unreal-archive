<#assign ogDescription="Documents and Reference for ${game.name}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/</span> Guides &amp; Reference
	</@heading>

	<@content class="biglist">
		<ul>
			<#list game.groups as k, group>
				<@bigitem link="${relPath(group.path + '/index.html')}" meta="${group.count}">${group.name}</@bigitem>
			</#list>
		</ul>
	</@content>

<#include "../_footer.ftl">
