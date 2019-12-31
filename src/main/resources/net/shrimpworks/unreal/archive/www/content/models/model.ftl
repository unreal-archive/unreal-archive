<#assign game=model.page.letter.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>

<#list model.model.attachments as a>
	<#if a.type == "IMAGE">
		<#assign headerbg=urlEncode(a.url)>
		<#break>
	</#if>
</#list>

<#assign ogDescription="${model.model.autoDescription()}">
<#assign ogImage=headerbg>

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${relPath(sectionPath + "/index.html")}">Models</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ ${model.model.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=model.model.attachments/>
		</div>

		<div class="info">

			<#assign modelList><#list model.model.models as m><div>${m}</div><#else>Unknown</#list></#assign>
			<#assign skinsList><#list model.model.skins as s><div>${s}</div><#else>Unknown</#list></#assign>

			<#assign
			labels=[
					"Name",
					"Author",
					"Release (est.)",
					"Included Models",
					"Included Skins",
					"File Size",
					"File Name",
					"Hash"
			]

			values=[
					'${model.model.name}',
					'${model.model.author}',
					'${dateFmtShort(model.model.releaseDate)}',
					'${modelList}',
					'${skinsList}',
					'${fileSize(model.model.fileSize)}',
					'${model.model.originalFilename}',
					'${model.model.hash}'
			]

      styles={"7": "nomobile"}
      >

			<@meta title="Model Information" labels=labels values=values styles=styles/>

			<#if model.variations?size gt 0>
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
							<#list model.variations as v>
							<tr>
								<td><a href="${relPath(v.path + ".html")}">${v.model.name}</a></td>
								<td>${v.model.releaseDate}</td>
								<td>${v.model.originalFilename}</td>
								<td>${fileSize(v.model.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>

			<@files files=model.model.files alsoIn=model.alsoIn otherFiles=model.model.otherFiles/>

			<@downloads downloads=model.model.downloads/>

		</div>

	</@content>

<#include "../../_footer.ftl">