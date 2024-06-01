<#if document.document.titleImage??>
	<#assign headerbg>${document.document.titleImage}</#assign>
<#else>
	<#assign headerbg>${staticPath()}/images/contents/documents.png</#assign>
</#if>

<#assign group=document.subGroup.parent>
<#assign subgroup=document.subGroup>

<#assign ogDescription=document.document.description>
<#assign ogImage=headerbg>

<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(group.game.root + "/index.html")}">${group.game.name}</a>
			/ <a href="${relPath(group.game.path + "/index.html")}">Guides &amp; Reference</a>
			/ <a href="${relPath(group.path + "/index.html")}">${group.name}</a>
			/ <a href="${relPath(subgroup.path + "/index.html")}">${subgroup.name}</a>
			/</span> ${document.document.title}
	</@heading>

	<@content class="document split split7030">
		<div class="right">
			<div class="meta">
				<div class="label-value">
					<label>Author</label><span>${document.document.author}</span>
				</div>
				<div class="label-value">
					<label>Created Date</label><span>${document.document.createdDate}</span>
				</div>
				<div class="label-value">
					<label>Last Updated</label><span>${document.document.updatedDate}</span>
				</div>
				<div class="label-value">
					<label>Summary</label><span>${document.document.description}</span>
				</div>
			</div>
		</div>

		<div class="left">
			<section class="content readable">
				${page?no_esc}
			</section>
		</div>
	</@content>

<#include "../_footer.ftl">