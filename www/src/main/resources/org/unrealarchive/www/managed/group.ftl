<#assign ogDescription="Content for ${group.game.name}">
<#assign ogImage="${staticPath()}/images/games/${group.game.name}.png">

<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading bg=[ogImage]>
	<span class="crumbs">
			<a href="${relPath(group.game.path + "/index.html")}">${group.game.name}</a>
			/</span> ${group.name}
</@heading>

<@content class="biglist">
	<ul>
		<#list group.subGroups as k, group>
			<@bigitem link="${relPath(group.path + '/index.html')}" meta="${group.count}">${group.name}</@bigitem>
		</#list>
	</ul>
</@content>

<#include "../_footer.ftl">
