<#assign extraCss="wiki.css"/>
<#if wiki.title?? && wiki.title?length gt 0>
	<#assign headerbg>${relPath(wikiPath + "/" + wiki.title)}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/contents/wikis.png</#assign>
</#if>

<#assign ogDescription="heh">
<#assign ogImage=headerbg>

<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading bg=[headerbg]>
	<span class="crumbs">
			<a href="${relPath(wikiPath + "/../index.html")}">Wikis</a>
			/ <a href="${relPath(wikiPath + "/index.html")}">${wiki.name}</a>
			/</span> ${page.name}
</@heading>

<@content class="document wiki">
	<section class="readable">
		${text?no_esc}
	</section>

	<#if categoryPages?? && categoryPages?size gt 0>
		<section class="readable categoryPages">
			<div class="info">This category has the following ${categoryPages?size} pages and sub-categories.</div>
			<ul>
				<#list categoryPages?sort_by("name") as pg>
					<li><a href="./${pg.name?replace(" ", "_")}.html">${pg.name}</a></li>
				</#list>
			</ul>
		</section>
	</#if>
	<#if page.parse.categories?? && page.parse.categories?size gt 0>
		<section class="readable categories">
			<h4><img src="${staticPath()}/images/icons/list.svg" alt="Categories"/>Page Categories</h4>
			<ul>
				<#list page.parse.categories as cat>
					<li><a href="./${relPath(wikiPath + "/Category:" + cat.name + ".html")}">${cat.name}</a></li>
				</#list>
			</ul>
		</section>
	</#if>
	<section class="readable updates">
		<h4><img src="${staticPath()}/images/icons/info.svg" alt="Info"/> Page Information</h4>
		<div class="label-value">
			<#if hasDiscussion>
				<label>&nbsp;</label><span><a href="./Talk:${page.name?replace(" ", "_")}.html">Discussion</a></span>
			</#if>
			<label>Capture Time</label><span>${page.timestamp}</span>
			<#if page.revision??>
				<label>Updated Time</label><span>${page.revision.timestamp}</span>
				<label>Updated by</label><span>
					<#if hasUserPage>
						<a href="./${relPath(wikiPath + "/User:" + page.revision.user?replace(" ", "_") + ".html")}">${page.revision.user}</a>
					<#else>
						${page.revision.user}
					</#if>
				</span>
				<label>Comment</label><span>${page.revision.comment}</span>
      </#if>
			<label>Original URL</label><span><a href="${wiki.url}/${page.name}">${wiki.url}/${page.name}</a></span>
			<label>Licence</label><span><a href="${wiki.licence.url}">${wiki.licence.name}</a></span>
		</div>
	</section>
</@content>

<#include "../_footer.ftl">