<#assign game=model.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if model.item.leadImage?has_content>
    <#assign headerbg=urlEncode(model.item.leadImage)>
</#if>

<#assign ogDescription="${model.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${model.item.name}">
<#assign schemaItemAuthor="${model.item.author}">
<#assign schemaItemDate="${model.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Models</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/</span> ${model.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@links links=model.item.links/>

        <@screenshots attachments=model.item.attachments/>
		</div>

		<div class="info">

			<#assign modelList><#list model.item.models as m><div>${m}</div><#else></#list></#assign>
			<#assign skinsList><#list model.item.skins as s><div>${s}</div><#else></#list></#assign>

			<#assign author><@authorLink model.item.authorName /></#assign>
			<#assign
			labels=[
					"Name",
					"Author",
					"Release (est.)",
					"Included Models",
					"Included Skins",
					"File Size",
					"File Name",
					"SHA1 Hash"
			]

			values=[
					'${model.item.name}',
					'${author}',
					'${dateFmtShort(model.item.releaseDate)}',
					'${modelList}',
					'${skinsList}',
					'${fileSize(model.item.fileSize)}',
					'${model.item.originalFilename}',
					'${model.item.hash}'
			]

      styles={"7": "nomobile"}
      >

			<@meta title="Model Information" labels=labels values=values styles=styles/>

			<#if model.variations?size gt 0>
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
							<#list model.variations as v>
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

			<@files files=model.item.files alsoIn=model.alsoIn otherFiles=model.item.otherFiles/>

			<@downloads downloads=model.item.downloads/>

      <@dependencies deps=model.item.dependencies game=model.item.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Model] ${model.item.name}" hash="${model.item.hash}" name="${model.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">