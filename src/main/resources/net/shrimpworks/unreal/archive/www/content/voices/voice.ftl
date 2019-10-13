<#assign game=voice.page.letter.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>

<#list voice.voice.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

<#assign ogDescription="${voice.voice.name}, a custom player voice pack for ${game.game.bigName}, created by ${voice.voice.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${relPath(sectionPath + "/index.html")}">Voices</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ ${voice.voice.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=voice.voice.attachments/>
		</div>

		<div class="info">

			<#assign voicesList><#list voice.voice.voices as v><div>${v}</div><#else>Unknown</#list></#assign>

			<#assign
			labels=[
					"Name",
					"Author",
					"Release (est.)",
					"Included Voices",
					"File Size",
					"File Name",
					"Hash"
			]

			values=[
					'${voice.voice.name}',
					'${voice.voice.author}',
					'${dateFmtShort(voice.voice.releaseDate)}',
					'${voicesList}',
					'${fileSize(voice.voice.fileSize)}',
					'${voice.voice.originalFilename}',
					'${voice.voice.hash}'
			]>

			<@meta title="Voice Information" labels=labels values=values/>

			<#if voice.variations?size gt 0>
				<section class="variations">
					<h2><img src="${staticPath()}/images/icons/black/px22/variant.png" alt="Variations"/>Variations</h2>
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
							<#list voice.variations as v>
							<tr>
								<td><a href="${relPath(v.path + ".html")}">${v.voice.name}</a></td>
								<td>${v.voice.releaseDate}</td>
								<td>${v.voice.originalFilename}</td>
								<td>${fileSize(v.voice.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<@files files=voice.voice.files alsoIn=voice.alsoIn otherFiles=voice.voice.otherFiles/>

			<@downloads downloads=voice.voice.downloads/>

		</div>

	</@content>

<#include "../../_footer.ftl">