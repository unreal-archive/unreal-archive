<#assign game=voice.page.letter.game>

<#assign headerbg>${staticPath(static)}/images/games/${game.name}.png</#assign>

<#list voice.voice.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

<#assign ogDescription="${voice.voice.name}, a custom player voice packs for ${game.name}, created by ${voice.voice.author}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${siteRoot}/index.html">Voices</a>
		/ <a href="${relUrl(siteRoot, game.path)}/index.html">${game.name}</a>
		/ ${voice.voice.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=voice.voice.attachments/>
		</div>

		<div class="info">

			<section class="meta">
				<h2>Voice Information</h2>
				<div class="label-value">
					<label>Name</label><span>${voice.voice.name}</span>
				</div>
				<div class="label-value">
					<label>Author</label><span>${voice.voice.author}</span>
				</div>
				<div class="label-value">
					<label>Release (est.)</label><span>${voice.voice.releaseDate}</span>
				</div>
				<#if voice.voice.voices?size gt 0>
					<div class="label-value">
						<label>Included Voices</label><span>
							<#list voice.voice.voices as v>
								<div>${v}</div>
							</#list>
						</span>
					</div>
				</#if>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(voice.voice.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>File Name</label><span>${voice.voice.originalFilename}</span>
				</div>
				<div class="label-value nomobile">
					<label>Hash</label><span>${voice.voice.hash}</span>
				</div>
			</section>

			<#if voice.variations?size gt 0>
				<section class="variations">
					<h2>Variations</h2>
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
								<td><a href="${relUrl(siteRoot, v.path + ".html")}">${v.voice.name}</a></td>
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