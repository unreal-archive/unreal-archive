<#assign ogDescription="Content for ${game.name}">
<#assign ogImage="${staticPath()}/images/games/${game.name}.png">

<#include "_header.ftl">
<#include "macros.ftl">

<@heading bg=[ogImage]>${game.name}</@heading>

<@content class="biglist bigger">
	<ul>
		<#if managed?size gt 0>
			<#list managed as m, c>
        <@bigitem link="${slug(m)}/index.html" meta="${c}" bg="${staticPath()}/images/contents/t_patches.png">${m}</@bigitem>
			</#list>
		</#if>

		<#if count.Documents gt 0>
			<@bigitem link="documents/index.html" meta="${count.Documents}" bg="${staticPath()}/images/contents/t_documents.png">Guides &amp; Reference</@bigitem>
		</#if>

		<#if count.GameTypes gt 0>
      <@bigitem link="gametypes/index.html" meta="${count.GameTypes}" bg="${staticPath()}/images/contents/t_mods.png">Game Types &amp; Mods</@bigitem>
		</#if>

		<#if count.Map??>
			<@bigitem link="maps/index.html" meta="${count.Map}" bg="${staticPath()}/images/contents/t_maps.png">Maps</@bigitem>
		</#if>
		<#if count.MapPack??>
			<@bigitem link="mappacks/index.html" meta="${count.MapPack}" bg="${staticPath()}/images/contents/t_mappacks.png">Map Packs</@bigitem>
		</#if>
		<#if count.Mutator??>
			<@bigitem link="mutators/index.html" meta="${count.Mutator}" bg="${staticPath()}/images/contents/t_mutators.png">Mutators</@bigitem>
		</#if>
		<#if count.Model??>
      <@bigitem link="models/index.html" meta="${count.Model}" bg="${staticPath()}/images/contents/t_models.png">Models &amp; Characters</@bigitem>
		</#if>
		<#if count.Skin??>
			<@bigitem link="skins/index.html" meta="${count.Skin}" bg="${staticPath()}/images/contents/t_skins.png">Skins</@bigitem>
		</#if>
		<#if count.Voice??>
			<@bigitem link="voices/index.html" meta="${count.Voice}" bg="${staticPath()}/images/contents/t_voices.png">Voices</@bigitem>
		</#if>
		<#if count.Announcer??>
      <@bigitem link="announcers/index.html" meta="${count.Announcer}" bg="${staticPath()}/images/contents/t_announcers.png">Announcers</@bigitem>
    </#if>
	</ul>
</@content>

<#include "_footer.ftl">
