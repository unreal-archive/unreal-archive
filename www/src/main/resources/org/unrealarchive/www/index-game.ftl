<#assign ogDescription="Content for ${game.name}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "_header.ftl">
<#include "macros.ftl">

<@heading bg=[ogImage]>${game.name}</@heading>

<@content class="biglist">
	<ul>
		<#if managed?size gt 0>
			<#list managed as m, c>
				<li style='background-image: url("${staticPath()}/images/contents/t_patches.png")'>
					<span class="meta">${c}</span>
					<a href="${slug(m)}/index.html">${m}</a>
				</li>
			</#list>
		</#if>

		<#if count.Documents gt 0><li style='background-image: url("${staticPath()}/images/contents/t_documents.png")'>
			<span class="meta">${count.Documents}</span>
			<a href="documents/index.html">Guides &amp; Reference</a>
		</li></#if>

		<#if count.GameTypes gt 0><li style='background-image: url("${staticPath()}/images/contents/t_mods.png")'>
			<span class="meta">${count.GameTypes}</span>
			<a href="gametypes/index.html">Game Types &amp; Mods</a>
		</li></#if>

		<#if count.Map??><li style='background-image: url("${staticPath()}/images/contents/t_maps.png")'>
			<span class="meta">${count.Map}</span>
			<a href="maps/index.html">Maps</a>
		</li></#if>
		<#if count.MapPack??><li style='background-image: url("${staticPath()}/images/contents/t_mappacks.png")'>
			<span class="meta">${count.MapPack}</span>
			<a href="mappacks/index.html">Map Packs</a>
		</li></#if>
		<#if count.Mutator??><li style='background-image: url("${staticPath()}/images/contents/t_mutators.png")'>
			<span class="meta">${count.Mutator!"0"}</span>
			<a href="mutators/index.html">Mutators</a>
		</li></#if>
		<#if count.Model??><li style='background-image: url("${staticPath()}/images/contents/t_models.png")'>
			<span class="meta">${count.Model!"0"}</span>
			<a href="models/index.html">Models &amp; Characters</a>
		</li></#if>
		<#if count.Skin??><li style='background-image: url("${staticPath()}/images/contents/t_skins.png")'>
			<span class="meta">${count.Skin!"0"}</span>
			<a href="skins/index.html">Skins</a>
		</li></#if>
		<#if count.Voice??><li style='background-image: url("${staticPath()}/images/contents/t_voices.png")'>
			<span class="meta">${count.Voice!"0"}</span>
			<a href="voices/index.html">Voices</a>
		</li></#if>
      <#if count.Announcer??><li style='background-image: url("${staticPath()}/images/contents/t_announcers.png")'>
			<span class="meta">${count.Announcer!"0"}</span>
			<a href="announcers/index.html">Announcers</a>
		</li></#if>
	</ul>
</@content>

<#include "_footer.ftl">
