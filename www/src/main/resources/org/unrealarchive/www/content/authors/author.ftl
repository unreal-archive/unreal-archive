<#assign headerbg>${staticPath()}/images/games/All.png</#assign>
<#if author.leadImage??>
	<#assign headerbg=urlEncode(author.leadImage)>
</#if>

<#assign ogDescription="Unreal series content created by ${author.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

<@heading bg=[ogImage]>
	<span class="crumbs">
		<a href="${relPath(authorsPath + "/index.html")}">Authors</a>
		/</span> ${author.author}
</@heading>

<@content class="biglist taller">

	<#list author.contents as group, contents>
		<h2>${contents?size} ${group}<#if contents?size gt 1>s</#if></h2>
		<ul>
			<#list contents as c>
				<#assign bgi=""/>
				<#if c.leadImage?has_content>
					<#if c.leadImage?contains("://")>
						<#assign bgi=urlEncode(c.leadImage)/>
					<#else>
            <#assign bgi=rootPath(c.leadImage)/>
					</#if>
				</#if>
				<#outputformat "plainText">
					<#assign g><img src="${staticPath()}/images/games/icons/${c.game}.png" alt="${c.game}" title="${c.game}"/></#assign>
				</#outputformat>

				<@bigitem link="${relPath(c.pagePath(siteRoot))}" meta="${g}" bg="${bgi}">${c.name}</@bigitem>
			</#list>
		</ul>
	</#list>

</@content>

<#include "../../_footer.ftl">
