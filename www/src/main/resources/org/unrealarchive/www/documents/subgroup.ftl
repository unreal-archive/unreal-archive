<#assign ogDescription="Documents and Reference for ${subgroup.parent.game.name}">
<#assign ogImage="${staticPath()}/images/games/documents.png">

<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=[ogImage]>
		<span class="crumbs">
			<a href="${relPath(subgroup.parent.game.root + "/index.html")}">${subgroup.parent.game.name}</a>
			/ <a href="${relPath(subgroup.parent.game.path + "/index.html")}">Guides &amp; Reference</a>
			/ <a href="${relPath(subgroup.parent.path + "/index.html")}">${subgroup.parent.name}</a>
			/</span> ${subgroup.name}
	</@heading>

	<@content class="list">
		<section>
		<table class="docs">
			<thead>
			<tr>
				<th class="nomobile">&nbsp;</th>
				<th>Title</th>
				<th>Author</th>
				<th class="nomobile">Created</th>
			</tr>
			</thead>
			<tbody>
				<#list subgroup.documents as d>
					<tr>
						<td class="title-image nomobile">
							<a href="${relPath(d.path + "/index.html")}">
								<#if d.document.titleImage??>
									<img src="${relPath(d.path + "/" + d.document.titleImage)}"/>
								<#else>
									<img src="${staticPath()}/images/none-document.png"/>
								</#if>
							</a>
						</td>
						<td>
							<div><a href="${relPath(d.path + "/index.html")}">${d.document.title}</a></div>
							<div>${d.document.description}</div>
						</td>
						<td nowrap="nowrap">${d.document.author}</td>
						<td nowrap="nowrap" class="nomobile">${d.document.createdDate}</td>
					</tr>
				</#list>
			</tbody>
		</table>
		</section>
	</@content>

<#include "../_footer.ftl">