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
							<td>
								<img src="${staticPath()}/images/games/icons/${c.game}.png" alt="${c.game} icon" height="22"/>
								${c.game} ${c.friendlyType}
							</td>
							<td>
								<a href="${relPath(c.pagePath(siteRoot))}">${c.name}</a>
							</td>
							<td class="nomobile"><@authorLink content=c small=true/></td>
						</tr>
          </#list>
					</tbody>
				</table>
			</section>

			<#if date?counter == 10><#break></#if>
		</#list>
	</@content>

<#include "../../_footer.ftl">