<#assign game=skin.page.letter.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if skin.skin.leadImage?has_content>
    <#assign headerbg=urlEncode(skin.skin.leadImage)>
</#if>

<#assign ogDescription="${skin.skin.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${skin.skin.name}">
<#assign schemaItemAuthor="${skin.skin.author}">
<#assign schemaItemDate="${skin.skin.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<a href="${relPath(sectionPath + "/index.html")}">Skins</a>
		/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
		/ ${skin.skin.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=skin.skin.attachments/>
		</div>

		<div class="info">

			<#assign skinsList><#list skin.skin.skins as s><div>${s}</div></#list></#assign>
			<#assign faceList><#list skin.skin.faces as s><div>${s}</div></#list></#assign>

			<#assign author><@authorLink skin.skin.authorName /></#assign>
			<#assign
			labels=[
					"Name",
					"Author",
					"Release (est.)",
					"Team Skins",
					"Included Skins",
					"Faces",
					"File Size",
					"File Name",
					"SHA1 Hash"
			]

			values=[
					'${skin.skin.name}',
					'${author}',
					'${dateFmtShort(skin.skin.releaseDate)}',
					'${skin.skin.teamSkins?string("Yes", "No")}',
					'${skinsList}',
					'${faceList}',
					'${fileSize(skin.skin.fileSize)}',
					'${skin.skin.originalFilename}',
					'${skin.skin.hash}'
			]

      styles={"8": "nomobile"}
      >

			<@meta title="Skin Information" labels=labels values=values styles=styles/>

			<#if skin.variations?size gt 0>
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
							<#list skin.variations as v>
							<tr>
								<td><a href="${relPath(v.path + ".html")}">${v.skin.name}</a></td>
								<td>${v.skin.releaseDate}</td>
								<td>${v.skin.originalFilename}</td>
								<td>${fileSize(v.skin.fileSize)}</td>
							</tr>
							</#list>
						</tbody>
					</table>
				</section>
			</#if>


			<@files files=skin.skin.files alsoIn=skin.alsoIn otherFiles=skin.skin.otherFiles/>

			<@downloads downloads=skin.skin.downloads/>

			<@dependencies deps=skin.skin.dependencies/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Skin] ${skin.skin.name}" hash="${skin.skin.hash}" name="${skin.skin.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">