<#include "../_header.ftl">

	<section class="header" style='background-image: url("${static}/images/games/${game.name}.png")'>
		<h1>
			Maps / ${game.name}
		</h1>
	</section>
	<article class="biglist">
		<ul>
		<#list game.gametypes as k, v>
			<li>
				<span class="meta">${v.maps}</span>
				<a href="${relUrl(game.path, v.path + "/index.html")}">${v.name}</a>
			</li>
		</#list>
		</ul>
	</article>

<#include "../_footer.ftl">