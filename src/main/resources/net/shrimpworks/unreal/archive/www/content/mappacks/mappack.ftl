<#assign game=pack.page.gametype.game>
<#assign gametype=pack.page.gametype>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if pack.pack.leadImage?has_content>
    <#assign headerbg=urlEncode(pack.pack.leadImage)>
</#if>

<#assign ogDescription="${pack.pack.autoDescription}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[ogImage]>
		<a href="${relPath(sectionPath + "/index.html")}">Map Packs</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ <a href="${relPath(gametype.path + "/index.html")}">${gametype.name}</a>
		/ ${pack.pack.name}
	</@heading>

	<@content class="info">

		<div class="screenshots">
			<@screenshots attachments=pack.pack.attachments/>
		</div>

		<div class="info">

			<#assign themes>
				<#if pack.pack.themes?size gt 0>
					<#list pack.pack.themes as theme, weight>
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
				"Maps",
				"Author",
				"Release (est)",
        "Themes",
				"File Size",
				"File Name",
				"SHA1 Hash"
			]

			values=[
				'${pack.pack.name}',
				'<a href="${relPath(gametype.path + "/index.html")}">${pack.pack.gametype}</a>'?no_esc,
				'${pack.pack.maps?size}',
				'${pack.pack.author}',
				'${dateFmtShort(pack.pack.releaseDate)}',
        '${themes}',
				'${fileSize(pack.pack.fileSize)}',
				'${pack.pack.originalFilename}',
				'${pack.pack.hash}'
			]

      styles={"8": "nomobile"}
			>

			<@meta title="Map Pack Information" labels=labels values=values styles=styles/>

			<#if pack.variations?size gt 0>
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
							<#list pack.variations as v>
							<tr>
								<td><a href="${relPath(v.path + ".html")}">${v.pack.name}</a></td>
								<td>${v.pack.releaseDate}</td>
								<td>${v.pack.originalFilename}</td>
								<td>${fileSize(v.pack.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<section class="maps">
				<h2><img src="${staticPath()}/images/icons/list.svg" alt="Maps"/>Maps</h2>
				<table>
					<thead>
					<tr>
						<th>Name</th>
						<th class="nomobile">Title</th>
						<th>Author</th>
					</tr>
					</thead>
					<tbody>
						<#list pack.pack.maps as m>
						<tr>
							<td>${m.name}</td>
							<td class="nomobile">${m.title}</td>
							<td>${m.author}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</section>

			<@files files=pack.pack.files alsoIn=pack.alsoIn otherFiles=pack.pack.otherFiles/>

			<@downloads downloads=pack.pack.downloads/>

      <@dependencies deps=pack.pack.dependencies/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Map Pack] ${pack.pack.name}" hash="${pack.pack.hash}" name="${pack.pack.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">