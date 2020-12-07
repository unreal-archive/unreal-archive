<#assign game=pack.page.gametype.game>
<#assign gametype=pack.page.gametype>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>

<#list pack.pack.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

<#assign ogDescription="${pack.pack.autoDescription()}">
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
						<div class="theme-gauge">
							<div class="part p${theme?index}" style="width: ${weight * 100}%">${theme}</div>
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