<#assign ogDescription="Latest added downloads for Unreal, Unreal Tournament, and Unreal Tournament 2004">
<#assign ogImage="${staticPath()}/images/games/All.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		Latest Content Additions
	</@heading>

	<@content class="list">
		<#list latest as date, content>
			<section class="latest">
				<h2>${dateFmt(date)}</h2>
				<table>
					<thead>
					<tr>
						<th>Type</th>
						<th>Name</th>
						<th class="nomobile">Author</th>
					</tr>
					</thead>
					<tbody>
          <#list content as c>
						<tr>
							<td>${c.game} ${c.friendlyContentType()}</td>
							<td>
								<a href="${relPath(c.slugPath(siteRoot) + ".html")}">${c.name}</a>
							</td>
							<td class="nomobile">${trunc(c.author, 30)}</td>
						</tr>
          </#list>
					</tbody>
				</table>
			</section>

			<#if date?counter == 10><#break></#if>
		</#list>
	</@content>

<#include "../../_footer.ftl">