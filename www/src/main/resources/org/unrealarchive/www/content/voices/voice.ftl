<#assign game=voice.page.letter.group.game>

<#assign headerbg>${staticPath()}/images/games/${game.name}.png</#assign>
<#if voice.item.leadImage?has_content>
    <#assign headerbg=urlEncode(voice.item.leadImage)>
</#if>

<#assign ogDescription="${voice.item.autoDescription}">
<#assign ogImage=headerbg>

<#assign schemaItemName="${voice.item.name}">
<#assign schemaItemAuthor="${voice.item.author}">
<#assign schemaItemDate="${voice.item.releaseDate}-01">

<#include "../../_header.ftl">
<#include "../../macros.ftl">

	<@heading bg=[headerbg]>
		<span class="crumbs">
			<a href="${relPath(sectionPath + "/index.html")}">Voices</a>
			/ <a href="${relPath(game.path + "/index.html")}">${game.name}</a>
			/</span> ${voice.item.name}
	</@heading>

	<@content class="info">
		<div class="side">
        <@problems problems=voice.item.problemLinks/>

        <@links links=voice.item.links/>

        <@screenshots attachments=voice.item.attachments/>
		</div>

		<div class="info">

			<#assign author><@authorLink voice.item.authorName /></#assign>
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
					'${voice.item.name}',
					'${author}',
					'${dateFmtShort(voice.item.releaseDate)}',
					'${fileSize(voice.item.fileSize)}',
					'${voice.item.originalFilename}',
					'${voice.item.hash}'
			]

      styles={"5": "nomobile"}
      >

			<@meta title="Voice Information" labels=labels values=values styles=styles/>

			<@variations variations=voice.variations/>

			<#if voice.item.voices?size gt 0>
			<@contents title="Voices">
				<#assign voicesList><#list voice.item.voices?sort as v><div>${v}</div><#else>Unknown</#list></#assign>
				<#assign labels=["Included Voices"] values=['${voicesList}']>
				<@labellist labels=labels values=values/>
			</@contents>
			</#if>

			<@files files=voice.item.files alsoIn=voice.alsoIn otherFiles=voice.item.otherFiles/>

			<@downloads downloads=voice.item.downloads/>

      <@dependencies deps=voice.item.dependencies game=voice.item.game/>

      <@ghIssue text="Report a problem" repoUrl="${dataProjectUrl}" title="[Voice] ${voice.item.name}" hash="${voice.item.hash}" name="${voice.item.name}"/>

		</div>

	</@content>

<#include "../../_footer.ftl">