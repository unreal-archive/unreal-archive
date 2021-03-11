<#assign headerbg>${staticPath()}/images/games/All.png</#assign>
<#list author.contents as content>
	<#if content.leadImage?has_content>
		<#assign headerbg=urlEncode(content.leadImage)>
		<#break>
	</#if>
</#list>

<#assign ogDescription="Unreal series content by ${author.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

<@heading bg=[ogImage]>
	<a href="${relPath(sectionPath + "/index.html")}">Authors</a>
	/ ${author.author}
</@heading>

<@content class="biglist taller">

	<ul>
		<#list author.contents as c>
			<#assign bg="">
			<#if c.leadImage?has_content>
				<#assign bg=urlEncode(c.leadImage)>
			</#if>

			<li style='background-image: url("${bg}")'>
				<span class="meta">${c.friendlyType}</span>
				<a href="${relPath(c.slugPath(siteRoot) + ".html")}">${c.name}</a>
			</li>
		</#list>
	</ul>

</@content>

<#include "../../_footer.ftl">
