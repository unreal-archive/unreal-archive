<#include "../../_header.ftl">

	<section class="header">
		<h1>
			File / ${file.name}
		</h1>
	</section>

	<article class="file">
		<div class="info">

			<section class="meta">
				<h2>File</h2>
				<div class="label-value">
					<label>Name</label><span>${file.name}</span>
				</div>
				<div class="label-value">
					<label>File Size</label><span>${fileSize(file.fileSize)}</span>
				</div>
				<div class="label-value">
					<label>Hash</label><span>${file.hash}</span>
				</div>
			</section>

			<section class="packages">
				<h2>In Packages</h2>
				<table>
					<thead>
					<tr>
						<th>Type</th>
						<th>Game</th>
						<th>Name</th>
						<th>Author</th>
						<th>Hash</th>
					</tr>
					</thead>
					<tbody>
						<#list packages as c>
						<tr>
							<td>${c.contentType}</td>
							<td>${c.game}</td>
							<td>
								<#if c.contentType == "MAP">
									${c.name}
								<#else>
									${c.name}
								</#if>
							</td>
							<td>${c.author}</td>
							<td>${c.hash}</td>
						</tr>
						</#list>
					</tbody>
				</table>
			</section>

		</div>

	</article>

<#include "../../_footer.ftl">