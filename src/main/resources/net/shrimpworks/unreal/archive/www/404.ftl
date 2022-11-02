<!-- since the 404 page only applies to online hosted content, we can force the homepage to the root -->
<#assign homeOverride="/index.html" />

<#include "_header.ftl">
<#include "macros.ftl">

<@content class="intro readable">
	<h1>
		Not Found 😟
	</h1>
	<p>
		The page you are looking for was not found or was moved.
	</p>
</@content>

<#include "_footer.ftl">