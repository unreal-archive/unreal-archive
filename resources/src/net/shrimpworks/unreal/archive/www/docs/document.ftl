<#include "../_header.ftl">

	<#assign headerbg>${static!"static"}/images/none-document.png</#assign>

	<#if document.document.titleImage??>
		<#assign headerbg>${document.document.titleImage}</#assign>
	</#if>

	<section class="header" style="background-image: url('${headerbg}')">
		<h1>
			${title}
		</h1>
	</section>
	<article class="document">
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
			${content}
		</div>
	</article>

<#include "../_footer.ftl">