<#include "macros.ftl">
<#compress>
<footer class="page">
	<div>
		<div>Unreal Archive ${version}. Website last generated: <code>${timestamp?string["yyyy-MM-dd HH:mm:ss '('zzz')'"]}</code></div>
		<div><i>${siteName}</i> claims no ownership or copyright over the content
			listed or hosted here. All content is the property of its respective
			authors. You download and use the content listed and hosted here at your
			own risk, <i>${siteName}</i> makes no guarantees as to the functionality,
			suitability, integrity, or safety of the content listed here.</div>
		<div><i>${siteName}</i> does not use cookies or employ any visitor tracking analytics.</div>
		<div style="text-align: right">
			<a href="https://github.com/unreal-archive">
				<@icon "github"/>Contribute on GitHub
			</a>
		</div>
	</div>
</footer>

</div>

<script src="${staticPath()}/scripts/lb.js"></script>
<#if features?? && features.collections>
	<script src="${staticPath()}/scripts/collections.js"></script>
</#if>

</body>
</html>
</#compress>