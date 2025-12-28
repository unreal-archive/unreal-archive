<#assign game=skin.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if skin.item.leadImage?has_content>
    <#assign headerbg=urlEncode(skin.item.leadImage)>
</#if>

<#assign ogDescription="${skin.item.autoDescription}">
<#assign ogImage=headerbg>
<#assign ogId="${skin.item.id}">

<#assign schemaItemName="${skin.item.name}">
<#assign schemaItemAuthor="${skin.item.authorName}">
<#assign schemaItemDate="${skin.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(game.root + "/index.html")}">${game.name}</a>
			/ <a href="${relPath(game.path + "/index.html")}">Skins</a>
			/</span> ${skin.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@problems problems=skin.item.problemLinks/>

        <@links links=skin.item.links/>

        <@screenshots attachments=skin.item.attachments/>
		</div>

		<div class="info">

			<#assign author><@authorLink skin.item /></#assign>
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

			<@downloads downloads=skin.item.downloads/>

			<@files game=game files=skin.item.files alsoIn=skin.alsoIn otherFiles=skin.item.otherFiles/>

			<@dependencies game=game deps=skin.item.dependencies/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Skin] ${skin.item.name}" hash="${skin.item.hash}" name="${skin.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">