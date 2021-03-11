<#assign headerbg>${staticPath()}/images/games/All.png</#assign>
<#if author.leadImage??>
	<#assign headerbg=urlEncode(author.leadImage)>
</#if>

<#assign ogDescription="Unreal series content created by ${author.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

<@heading bg=[ogImage]>
	<a href="${relPath(sectionPath + "/index.html")}">Authors</a>
	/ ${author.author}
</@heading>

<@content class="biglist taller">

	<#list author.contents as group, contents>
		<h2>${group}</h2>
		<ul>
			<#list contents as c>
				<#assign bg="">
				<#if c.leadImage?has_content>
					<#assign bg=urlEncode(c.leadImage)>
				</#if>

				<li style='background-image: url("${bg}")'>
					<a href="${relPath(c.slugPath(siteRoot) + ".html")}">${c.name}</a>
				</li>
			</#list>
		</ul>
	</#list>

</@content>

<#include "../../_footer.ftl">
