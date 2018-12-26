<#include "_header.ftl">

<section class="intro readable">
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
		<a href="documents/general/meta/contribution-guide/index.html">contribution guide</a>.
		The Archive is a completely	open source initiative.
	</p>
	<p>
		You're also welcome and encouraged to download entire local mirrors of the
		Archive content and website, as spreading it around helps to ensure the
		longevity of the content and reduce the changes of it being permanently lost.
		See the <a href="documents/general/meta/archive-mirroring-guide/index.html">mirroring guide</a>
		for more information.
	</p>
</section>

<article class="biglist">
	<ul>
		<li>
			<span class="meta">${count.Documents}</span>
			<a href="documents/index.html">Guides &amp; Articles</a>
		</li>
		<li>
			<span class="meta">${count.Updates}</span>
			<a href="patches-updates/index.html">Patches & Updates</a>
		</li>
		<li style='background-image: url("${static}/images/contents/maps.png")'>
			<span class="meta">${count.Map}</span>
			<a href="maps/index.html">Maps</a>
		</li>
		<li style='background-image: url("${static}/images/contents/mappacks.png")'>
			<span class="meta">${count.MapPack}</span>
			<a href="mappacks/index.html">Map Packs</a>
		</li>
		<li>
			<span class="meta">coming soon</span>
			<a href="#">Game Types</a>
		</li>
		<li>
			<span class="meta">coming soon</span>
			<a href="#">Mutators</a>
		</li>
		<li style='background-image: url("${static}/images/contents/models.png")'>
			<span class="meta">${count.Model!"0"}</span>
			<a href="models/index.html">Models</a>
		</li>
		<li style='background-image: url("${static}/images/contents/skins.png")'>
			<span class="meta">${count.Skin!"0"}</span>
			<a href="skins/index.html">Skins</a>
		</li>
		<li>
			<span class="meta">coming soon</span>
			<a href="#">Voices</a>
		</li>
	</ul>
</article>

<#include "_footer.ftl">