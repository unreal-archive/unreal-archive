<#include "_header.ftl">

	<section class="header">
		<h1>
			${game.name}
		</h1>
	</section>
	<article>
		<ul>
		<#list game.gametypes as k, v>
			<li><a href="${relUrl(game.path, v.path + "/index.html")}">${v.name} (${v.maps})</a></li>
		</#list>
		</ul>
	</article>

<#include "_footer.ftl">