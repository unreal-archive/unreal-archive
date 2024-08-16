<#assign ogDescription="Custom announcer packs for ${game.game.bigName} released in ${year?c}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Announcers</a>
			/</span> ${year?c}
	</@heading>


	<@tline timeline=timeline game=game activeYear=year></@tline>

	<@content class="biglist">
		<ul>
			<#list months as m, count>
				<#assign disable=(count == 0)/>
				<#outputformat "plainText">
				  <#assign ref><#if !disable>${relPath(game.path + "/releases/${year?c}/${m}/index.html")}</#if></#assign>
				</#outputformat>
				<@bigitem link="${ref}" meta="${count}" disabled=disable>${monthNames[m-1]}</@bigitem>
			</#list>
		</ul>
	</@content>

<#include "../../_footer.ftl">