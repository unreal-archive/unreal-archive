<#assign extraCss="wiki.css"/>
<#assign ogDescription="Mirrors of Unreal and Unreal Torunament and Wikis, containing Engine, Modding, and Game information">
<#assign ogImage="${staticPath()}/images/wikis.png">

<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading bg=[ogImage]>
	Wikis
</@heading>

<@content class="biglist bigger">
	<ul>
      <#list wikis as wiki>
				<li style='background-image: url("${slug(wiki.name)}/${wiki.title}"),url("${ogImage}")'>
					<a href="${slug(wiki.name)}/index.html">${wiki.name}</a>
				</li>
      </#list>
	</ul>
</@content>

<#include "../_footer.ftl">