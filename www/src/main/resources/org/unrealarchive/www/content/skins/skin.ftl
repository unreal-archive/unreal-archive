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
		<div class="side">
        <@links links=skin.item.links/>

        <@screenshots attachments=skin.item.attachments/>
		</div>

		<div class="info">

			<#assign author><@authorLink skin.item.authorName /></#assign>
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
					'${skin.item.name}',
					'${author}',
					'${dateFmtShort(skin.item.releaseDate)}',
					'${fileSize(skin.item.fileSize)}',
					'${skin.item.originalFilename}',
					'${skin.item.hash}'
			]

      styles={"5": "nomobile"}
      >

			<@meta title="Skin Information" labels=labels values=values styles=styles/>

			<@variations variations=skin.variations/>

			<@contents title="Skins">
				<#assign skinsList><#list skin.item.skins?sort as s><div>${s}</div></#list></#assign>
				<#assign faceList><#list skin.item.faces?sort as s><div>${s}</div></#list></#assign>
				<#assign
					labels=["Team Skins", "Included Skins", "Faces"]
					values=[
	          '${skin.item.teamSkins?string("Yes", "No")}',
					  '${skinsList}', '${faceList}'
					]
				>
				<@labellist labels=labels values=values/>
			</@contents>

			<@files files=skin.item.files alsoIn=skin.alsoIn otherFiles=skin.item.otherFiles/>

			<@downloads downloads=skin.item.downloads/>

			<@dependencies deps=skin.item.dependencies game=skin.item.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Skin] ${skin.item.name}" hash="${skin.item.hash}" name="${skin.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">