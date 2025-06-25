<#assign game=model.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if model.item.leadImage?has_content>
    <#assign headerbg=urlEncode(model.item.leadImage)>
</#if>

<#assign ogDescription="${model.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${model.item.name}">
<#assign schemaItemAuthor="${model.item.authorName}">
<#assign schemaItemDate="${model.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Models &amp; Characters</a>
			/</span> ${model.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@problems problems=model.item.problemLinks/>

        <@links links=model.item.links/>

        <@screenshots attachments=model.item.attachments/>
		</div>

		<div class="info">
			<#assign author><@authorLink model.item /></#assign>
			<#assign
			labels=[
					"Name",
					"Author",
					"Release (est.)",
					"File Size",
					"File Name",
					"SHA1 Hash"
			]

			values=[
					'${model.item.name}',
					'${author}',
					'${dateFmtShort(model.item.releaseDate)}',
					'${fileSize(model.item.fileSize)}',
					'${model.item.originalFilename}',
					'${model.item.hash}'
			]

      styles={"5": "nomobile"}
      >

			<@meta title="Model Information" labels=labels values=values styles=styles/>

			<@variations variations=model.variations/>

			<#if model.item.models?size gt 0 || model.item.skins?size gt 0>
			<@contents title="Models and Skins">
				<#assign modelList><#list model.item.models?sort as m><div>${m}</div><#else></#list></#assign>
				<#assign skinsList><#list model.item.skins?sort as s><div>${s}</div><#else></#list></#assign>
				<#assign
				  labels=["Included Models", "Included Skins"]
					values=['${modelList}', '${skinsList}']
      	>
				<@labellist labels=labels values=values/>
      </@contents>
			</#if>

			<@downloads downloads=model.item.downloads/>

			<@files game=game files=model.item.files alsoIn=model.alsoIn otherFiles=model.item.otherFiles/>

      <@dependencies game=game deps=model.item.dependencies/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Model] ${model.item.name}" hash="${model.item.hash}" name="${model.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">