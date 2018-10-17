<#include "_header.ftl">

	<section class="header">
		<h1>
			Hello World
		</h1>
	</section>
	<article>
		<ul>
		<#list games as game>
			<li>${game}</li>
		</#list>
		</ul>
	</article>

<#include "_footer.ftl">