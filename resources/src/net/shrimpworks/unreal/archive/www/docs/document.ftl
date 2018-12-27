<#include "../_header.ftl">
<#include "../content/macros.ftl">

	<#assign headerbg>${static!"static"}/images/none-document.png</#assign>

	<#if document.document.titleImage??>
		<#assign headerbg>${document.document.titleImage}</#assign>
	</#if>

	<@heading bg=[headerbg]>
		${title}
	</@heading>

	<@content class="document">
		<div class="meta">
			<div class="label-value">
				<label>Author</label><span>${document.document.author}</span>
			</div>
			<div class="label-value">
				<label>Created Date</label><span>${document.document.createdDate}</span>
			</div>
			<div class="label-value">
				<label>Last Updated</label><span>${document.document.updatedDate}</span>
			</div>
			<div class="label-value">
				<label>Summary</label><span>${document.document.description}</span>
			</div>
		</div>
		<div class="content readable">
			${page}
		</div>
	</@content>

<#include "../_footer.ftl">