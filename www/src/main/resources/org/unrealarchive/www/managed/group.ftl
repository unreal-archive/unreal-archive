<#include "../_header.ftl">
<#include "../macros.ftl">

	<@heading bg=["${staticPath()}/images/contents/patches.png"]>
		<#list groupPath as p>
		  <#if p?is_first && p?has_next><span class="crumbs"><#elseif !p?is_first && p?is_last></span></#if>
			<a href="${relPath(p.path + "/index.html")}">${p.name}</a>
			<#if p?has_next>/</#if>
		</#list>
	</@heading>

	<#if group.groups?size gt 0>
		<@content class="biglist">
			<ul>
			<#list group.groups as k, g>
				<li style='background-image: url("${staticPath()}/images/games/${g.name}.png")'>
					<span class="meta">${g.count}</span>
					<a href="${relPath(g.path + "/index.html")}">${g.name}</a>
				</li>
			</#list>
			</ul>
		</@content>
  </#if>

	<#if group.content?size gt 0>
		<@content class="list">
			<table class="content">
				<thead>
				<tr>
					<th class="nomobile">&nbsp;</th>
					<th>Title</th>
					<th>Author</th>
					<th>Release Date</th>
				</tr>
				</thead>
				<tbody>
					<#list group.content as c>
						<tr>
							<td class="title-image nomobile" rowspan="2">
								<a href="${relPath(c.path + "/index.html")}">
									<#if c.managed.titleImage??>
										<img src="${relPath(c.path + "/" + c.managed.titleImage)}"/>
									<#else>
										<img src="${staticPath()}/images/none-managed.png"/>
									</#if>
								</a>
							</td>
							<td nowrap="nowrap"><a href="${relPath(c.path + "/index.html")}">${c.managed.title}</a></td>
							<td>${c.managed.author}</td>
							<td>${c.managed.releaseDate!"-"}</td>
						</tr>
						<tr>
							<td colspan="3">${c.managed.description}</td>
						</tr>
					</#list>
				</tbody>
			</table>
		</@content>
	</#if>

<#include "../_footer.ftl">