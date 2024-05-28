<#include "_header.ftl">
<#include "macros.ftl">

<@heading bg=["${staticPath()}/images/games/All.png"]>Contents</@heading>
<@content class="biglist bigger">
	<ul>
		<#list games as g, cnt>
			<li style='background-image: url("${staticPath()}/images/games/t_${g.name}.png")'>
				<span class="meta">${cnt}</span>
				<a href="${slug(g.name)}/index.html">${g.name}</a>
			</li>
		</#list>
	</ul>
</@content>

<@content class="biglist">
	<h1>
		Resources
	</h1>
	<ul>
		<li style='background-image: url("${staticPath()}/images/contents/t_wikis.png")'>
			<a href="wikis/index.html">Wikis</a>
		</li>
		<li style='background-image: url("${staticPath()}/images/games/All.png")'>
			<a href="authors/index.html">Browse by Author</a>
		</li>
		<li style='background-image: url("${staticPath()}/images/gametypes/Unreal/t_Single Player.png")'>
			<a href="misc/index.html">Miscellaneous</a>
		</li>
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
		<a href="documents/general/content-contribution-guide/index.html">content</a> or
		<a href="documents/general/website-and-code-contribution-guide/index.html">website</a>
		contribution guides. The Archive is a completely	open source initiative.
	</p>
	<p>
		You're also welcome and encouraged to download entire local mirrors of the
		Archive content and website, as spreading it around helps to ensure the
		longevity of the content and reduce the chances of it being lost to time.
		See the <a href="documents/general/archive-mirroring-guide/index.html">mirroring guide</a>
		for more information.
	</p>
</@content>

<#include "_footer.ftl">
