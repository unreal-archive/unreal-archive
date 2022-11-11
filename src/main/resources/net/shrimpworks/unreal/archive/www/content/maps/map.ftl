<#assign game=map.page.letter.group.game>
<#assign gametype=map.page.letter.group>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if map.item.leadImage?has_content>
    <#assign headerbg=urlEncode(map.item.leadImage)>
</#if>

<#assign ogDescription="${map.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${map.item.name}">
<#assign schemaItemAuthor="${map.item.author}">
<#assign schemaItemDate="${map.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Maps</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
			/</span> ${map.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@links links=map.item.links/>

        <@screenshots attachments=map.item.attachments/>
		</div>

		<div class="info">

			<#assign themes>
				<#if map.item.themes?size gt 0>
					<#list map.item.themes as theme, weight>
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
				<#else></#if>
			</#assign>

			<#assign author><span title="${map.item.author}"><@authorLink map.item.authorName /></span></#assign>
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
					'${map.item.name}',
					'<a href="${relPath(gametype.path + "/index.html")}">${map.item.gametype}</a>'?no_esc,
					'${map.item.title}',
					'${author}',
					'${map.item.playerCount}',
					'${map.item.bots?string("Yes", "No")}',
					'${dateFmtShort(map.item.releaseDate)}',
					'${map.item.description?replace("||", "<br/><br/>")?no_esc}',
      		'${themes}',
      		'${fileSize(map.item.fileSize)}',
					'${map.item.originalFilename}',
					'${map.item.hash}'
			]

			styles={"11": "nomobile"}
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
								<td><a href="${relPath(v.path + ".html")}">${v.item.name}</a></td>
								<td>${v.item.releaseDate}</td>
								<td>${v.item.originalFilename}</td>
								<td>${fileSize(v.item.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<@files files=map.item.files alsoIn=map.alsoIn otherFiles=map.item.otherFiles/>

			<@downloads downloads=map.item.downloads/>

			<@dependencies deps=map.item.dependencies game=map.item.game/>

			<@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Map] ${map.item.name}" hash="${map.item.hash}" name="${map.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">