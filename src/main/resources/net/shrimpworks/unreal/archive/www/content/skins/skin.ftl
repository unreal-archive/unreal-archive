<#assign game=skin.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if skin.item.leadImage?has_content>
    <#assign headerbg=urlEncode(skin.item.leadImage)>
</#if>

<#assign ogDescription="${skin.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${skin.item.name}">
<#assign schemaItemAuthor="${skin.item.author}">
<#assign schemaItemDate="${skin.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Skins</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/</span> ${skin.item.name}
	</@heading>

	<@content class="info">
		<div class="screenshots">
			<@screenshots attachments=skin.item.attachments/>
		</div>

		<div class="info">

			<#assign skinsList><#list skin.item.skins as s><div>${s}</div></#list></#assign>
			<#assign faceList><#list skin.item.faces as s><div>${s}</div></#list></#assign>

			<#assign author><@authorLink skin.item.authorName /></#assign>
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
					'${skin.item.name}',
					'${author}',
					'${dateFmtShort(skin.item.releaseDate)}',
					'${skin.item.teamSkins?string("Yes", "No")}',
					'${skinsList}',
					'${faceList}',
					'${fileSize(skin.item.fileSize)}',
					'${skin.item.originalFilename}',
					'${skin.item.hash}'
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


			<@files files=skin.item.files alsoIn=skin.alsoIn otherFiles=skin.item.otherFiles/>

			<@downloads downloads=skin.item.downloads/>

			<@dependencies deps=skin.item.dependencies game=skin.item.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Skin] ${skin.item.name}" hash="${skin.item.hash}" name="${skin.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">