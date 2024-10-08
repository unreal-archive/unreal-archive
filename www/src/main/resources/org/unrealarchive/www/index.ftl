<#include "_header.ftl">
<#include "macros.ftl">

<@heading bg=["${staticPath()}/images/games/All.png"]>Contents</@heading>
<@content class="biglist bigger">
	<ul>
		<#list games as g, cnt>
			<@bigitem link="${slug(g.name)}/index.html" meta="${cnt}" bg="${staticPath()}/images/games/t_${g.name}.png">${g.name}</@bigitem>
		</#list>
	</ul>
</@content>

<@content class="biglist">
	<ul>
		<@bigitem link="wikis/index.html" bg="${staticPath()}/images/contents/t_wikis.png">Wikis</@bigitem>
		<@bigitem link="authors/index.html" bg="${staticPath()}/images/games/All.png">Browse by Author</@bigitem>
		<@bigitem link="misc/index.html" bg="${staticPath()}/images/gametypes/Unreal/t_Single Player.png">Miscellaneous</@bigitem>
	</ul>
</@content>

<@content class="intro readable">
	<h1>
		About
	</h1>
	<p>
		Welcome to the Unreal Archive, an initiative to preserve and maintain availability
		of the rich and vast history of user-created content for the Unreal and Unreal
		Tournament series of games.
	</p>
	<p>
		We've collected and indexed as much content as possible, and will continue to
		grow the Archive as new content is produced.
	</p>
	<p>
		You're also encouraged to contribute to the indexing, categorisation and
		refinement of the Archive content via the
		<a href="general/documents/unreal-archive/documentation/content-contribution-guide/index.html">content</a> or
		<a href="general/documents/unreal-archive/documentation/website-and-code-contribution-guide/index.html">website</a>
		contribution guides. The Archive is a completely	open source initiative.
	</p>
	<p>
		You're also welcome and encouraged to download entire local mirrors of the
		Archive content and website, as spreading it around helps to ensure the
		longevity of the content and reduce the chances of it being lost to time.
		See the <a href="general/documents/unreal-archive/documentation/archive-mirroring-guide/index.html">mirroring guide</a>
		for more information.
	</p>
</@content>

<#include "_footer.ftl">
