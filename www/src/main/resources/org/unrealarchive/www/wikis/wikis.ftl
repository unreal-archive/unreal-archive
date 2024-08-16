<#assign extraCss="wiki.css"/>
<#assign ogDescription="Mirrors of Unreal and Unreal Torunament and Wikis, containing Engine, Modding, and Game information">
<#assign ogImage="${staticPath()}/images/contents/wikis.png">

<#include "../_header.ftl">
<#include "../macros.ftl">

<@heading bg=[ogImage]>
	Wikis
</@heading>

<@content class="biglist bigger">
	<p>
		These wiki snapshots are mirrored here for future reference, to help reduce the chances of the
		vast amounts of knowledge and information they contain becoming lost, inaccessible, or unusable
		in the future, as well as providing the ability to
		<a href="/general/documents/unreal-archive/archive-mirroring-guide/index.html">host them offline</a>.
	</p>
	<p>
		Since they are snapshots, they may inevitably become out of date over time, though the sources
		have stabilised over the last several years. Future snapshots may be possible.
	</p>
	<p>
		The formatting of some pages may not always be ideal, depending on the source formatting, however
		attempts have been made to clean up and present the individual pages as cohesively as possible.
	</p>
	<ul>
		<#list wikis as wiki>
			<@bigitem link="${slug(wiki.name)}/index.html" bg="${slug(wiki.name)}/${wiki.title}" bg2="${ogImage}">${wiki.name}</@bigitem>
		</#list>
	</ul>
</@content>

<#include "../_footer.ftl">