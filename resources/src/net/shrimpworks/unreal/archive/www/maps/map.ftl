<#include "_header.ftl">

	<#list map.map.attachments as a>
		<#if a.type == "IMAGE">
			<#assign headerbg=a.url?url_path?replace('https%3A', 'https:')>
			<#break>
		</#if>
	</#list>

	<section class="header" <#if headerbg??>style="background-image: url('${headerbg}')"</#if>>
		<h1>
			${map.page.letter.gametype.game.name} / ${map.page.letter.gametype.name} / ${map.map.name}
		</h1>
	</section>
	<article>
		map info
	</article>

<#include "_footer.ftl">