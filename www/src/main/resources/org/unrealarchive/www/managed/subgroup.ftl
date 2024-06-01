<#assign ogDescription="Content for ${subgroup.parent.game.name}">
<#assign ogImage="${staticPath()}/images/games/${subgroup.parent.game.name}.png">

<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading bg=[ogImage]>
	<span class="crumbs">
			<a href="${relPath(subgroup.parent.game.path + "/index.html")}">${subgroup.parent.game.name}</a>
			/ <a href="${relPath(subgroup.parent.path + "/index.html")}">${subgroup.parent.name}</a>
			/</span> ${subgroup.name}
</@heading>

<@content class="biglist">
	<ul>
		<#list subgroup.content as content>
			<li
				<#if content.managed.titleImage?? && content.managed.titleImage?length gt 0>
					style='background-image: url("${relPath(content.path + "/" + content.managed.titleImage)}");'
				<#else>
					style='background-image: url("${staticPath()}/images/none-managed.png"");'
				</#if>
			>
				<a href="${relPath(content.path + "/index.html")}" title="${content.managed.name}">
					${content.managed.name}
				</a>
			</li>
		</#list>
	</ul>
</@content>

<#include "../_footer.ftl">
