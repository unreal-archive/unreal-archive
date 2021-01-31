<#assign game=map.page.letter.gametype.game>
<#assign gametype=map.page.letter.gametype>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if map.map.leadImage?has_content>
    <#assign headerbg=urlEncode(map.map.leadImage)>
</#if>

<#assign ogDescription="${map.map.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${map.map.name}">
<#assign schemaItemAuthor="${map.map.author}">
<#assign schemaItemDate="${map.map.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${relPath(sectionPath + "/index.html")}">Maps</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
		/ ${map.map.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=map.map.attachments/>
		</div>

		<div class="info">

			<#assign themes>
				<#if map.map.themes?size gt 0>
					<#list map.map.themes as theme, weight>
						<div class="themes">
							<#if weight lt 0.2>
								<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
								<#list 0..3 as n>
									<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
								</#list>
							<#elseif weight lt 0.4>
								<#list 0..1 as n>
									<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
								</#list>
								<#list 0..2 as n>
									<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
								</#list>
							<#elseif weight lt 0.6>
								<#list 0..2 as n>
									<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
								</#list>
								<#list 0..1 as n>
									<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
								</#list>
							<#elseif weight lt 0.8>
								<#list 0..3 as n>
									<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
								</#list>
								<img src="${staticPath()}/images/icons/circle-dotted.svg" alt="${weight * 100}%"/>
							<#else>
								<#list 0..4 as n>
									<img src="${staticPath()}/images/icons/circle.svg" alt="${weight * 100}%"/>
								</#list>
							</#if>
							<span>${theme}</span>
						</div>
					</#list>
				<#else>
					Unknown
				</#if>
			</#assign>

			<#assign
			labels=[
				  "Name",
					"Game Type",
					"Title",
					"Author",
					"Player Count",
					"AI/Bot Support",
					"Release (est)",
					"Description",
					"Themes",
					"File Size",
					"File Name",
					"SHA1 Hash"
			]

			values=[
					'${map.map.name}',
					'<a href="${relPath(gametype.path + "/index.html")}">${map.map.gametype}</a>'?no_esc,
					'${map.map.title}',
					'${map.map.author}',
					'${map.map.playerCount}',
					'${map.map.bots?string("Yes", "No")}',
					'${dateFmtShort(map.map.releaseDate)}',
					'${map.map.description?replace("||", "<br/><br/>")?no_esc}',
      		'${themes}',
      		'${fileSize(map.map.fileSize)}',
					'${map.map.originalFilename}',
					'${map.map.hash}'
			]

			styles={"10": "nomobile"}
			>

			<@meta title="Map Information" labels=labels values=values styles=styles/>

			<#if map.variations?size gt 0>
				<section class="variations">
					<h2><img src="${staticPath()}/images/icons/variant.svg" alt="Variations"/>Variations</h2>
					<table>
						<thead>
						<tr>
							<th>Name</th>
							<th>Release Date (est)</th>
							<th>File Name</th>
							<th>File Size</th>
						</tr>
						</thead>
						<tbody>
							<#list map.variations as v>
							<tr>
								<td><a href="${relPath(v.path + ".html")}">${v.map.name}</a></td>
								<td>${v.map.releaseDate}</td>
								<td>${v.map.originalFilename}</td>
								<td>${fileSize(v.map.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<@files files=map.map.files alsoIn=map.alsoIn otherFiles=map.map.otherFiles/>

			<@downloads downloads=map.map.downloads/>

			<@dependencies deps=map.map.dependencies/>

			<@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Map] ${map.map.name}" hash="${map.map.hash}" name="${map.map.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">