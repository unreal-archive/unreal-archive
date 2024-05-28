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
				<#assign bg="">
				<#if c.leadImage?has_content>
					<#if c.leadImage?contains("://")>
						<#assign bg=urlEncode(c.leadImage)>
					<#else>
            <#assign bg=rootPath(c.leadImage)>
					</#if>
				</#if>

				<li style='background-image: url("${bg}")'>
					<span class="meta"><img src="${staticPath()}/images/games/icons/${c.game}.png" alt="${c.game}" title="${c.game}"/></span>
					<a href="${relPath(c.pagePath(siteRoot))}">${c.name}</a>
				</li>
			</#list>
		</ul>
	</#list>

</@content>

<#include "../../_footer.ftl">
