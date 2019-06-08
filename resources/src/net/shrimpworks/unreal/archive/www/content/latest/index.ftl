<#assign ogDescription="Last added Files for Unreal, Unreal Tournament, and Unreal Tournament 2004">
<#assign ogImage="${staticPath()}/images/logo-96.png">

<#include "../../_header.ftl">
<#include "../../macros.ftl">


	<@heading bg=[ogImage]>
		Latest Content Additions
	</@heading>


	<@content class="list">
		<#list dates as d>
			<section class="latest">
				<h2>${d.date}</h2>
				<table>
					<thead>
					<tr>
						<th>Type</th>
						<th>Game</th>
						<th>Name</th>
						<th>Author</th>
					</tr>
					</thead>
					<tbody>
          <#list packages as c>
						<tr>
							<td>${c.friendlyContentType()}</td>
							<td>${c.game}</td>
							<td>
								<a href="${relPath(c.slugPath(siteRoot) + ".html")}">${c.name}</a>
							</td>
							<td>${c.author}</td>
						</tr>
          </#list>
					</tbody>
				</table>
			</section>
		</#list>
	</@content>