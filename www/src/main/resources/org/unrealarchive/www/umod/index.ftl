<#assign extraCss="submit.css"/>
<#include "../_header.ftl">
<#include "../macros.ftl">

<@content class="readable">

	<h1>
		UMOD Converter
	</h1>

	<div id="form">
		<div id="upload-block">
			<div id="upload-controls" class="display-block">
				<h2>Choose Files to Submit</h2>

				<div id="upload" class="display-block single">
					<input type="file" id="files" accept=".zip,.rar,.7z,.ace,.gz,.bz2,.tar,.tgz,.exe,.umod,.ut2mod,.ut4mod" multiple style="display:none">
					<button id="select-files"><@icon "file-plus"/>Add Files</button>
					<button id="upload-files"><@icon "upload"/>Convert!</button>
				</div>
			</div>

			<div id="progress-controls">
				<button id="abort">Cancel Upload</button>
				<progress id="progress" value="0" max="100" style="width: 100%"></progress>
			</div>

			<div id="files-list">
				<h2>Selected Files</h2>
			</div>
		</div>

		<div id="info-block">
			<div id="words" class="display-block">
				<h2>Information</h2>
				<p>
					This feature allows you to upload a UMOD package (Unreal Tournament 99 to 2004)
					and have it converted to a regular ZIP file you can unzip without needing
					additional tools or registry changes.
				</p>
				<p>
					To upload files, you may select one or more <b>.zip, .rar, .7z,
						.ace, .gz, .bz2, .tar, .tgz, .exe, .umod, ut2mod or .ut4mod</b>
					files to upload in a batch.
				</p>
				<p>
					Files you submit will be added to a queue and processed as in
					order of submission, so on occasion <b>you may need to wait a
					while</b> before your files are processed.
				</p>
			</div>

			<div id="log">
				<h2>Processing Status</h2>
				<div id="log-lines"></div>
			</div>
		</div>
	</div>

</@content>

<script type="application/javascript">
	let url = "https://unrealarchive.org/umod-repack/";
	let maxUploadSizeMegabytes = 100;

	document.addEventListener("DOMContentLoaded", function() {

		let selectFilesButton = document.querySelector('#select-files');
		let fileSelector = document.querySelector('#files');
		let uploadFilesButton = document.querySelector('#upload-files');
		let abortButton = document.querySelector('#abort');

		let filesList = document.querySelector('#files-list');
	  let logView = document.querySelector('#log');
	  let logLines = document.querySelector('#log-lines');
		let infoBlurb = document.querySelector('#words');

		let uploadControls = document.querySelector('#upload-controls');
		let progressControls = document.querySelector('#progress-controls');
		let progressBar = document.querySelector('#progress');

		let currentRequest = null;

		selectFilesButton.addEventListener('click', () => {
			fileSelector.click();
		});

		uploadFilesButton.addEventListener('click', () => {
			upload();
		});

		abortButton.addEventListener('click', () => {
			toggleProgress(false);
			if (currentRequest) {
				currentRequest.abort();
				currentRequest = null;
			}
		});

	  fileSelector.addEventListener('change', e => {
		  let totalSize = 0;
		  resetFilesList("Selected Files");

		  for (let i = 0; i < e.target.files.length; i++) {
			  let f = e.target.files[i];
			  let name = document.createElement("div");
			  name.innerText = f.name;
			  name.classList.add("name");
			  let size = document.createElement("div");
			  size.innerText = (f.size / 1024 / 1024).toFixed(1) + " mb";
			  size.classList.add("size");
			  let row = document.createElement("div");
			  row.classList.add("file");
			  row.append(name, size);
			  filesList.append(row);
			  totalSize += f.size;
		  }

		  if (!filesList.classList.contains("display-block")) filesList.classList.add("display-block");

		  if (totalSize >= (maxUploadSizeMegabytes * 1024 * 1024)) {
			  alert("Caution!\n\n" +
			        "The total max size per upload is " + maxUploadSizeMegabytes + " MB. Reduce the total size of the upload or it may fail.");
		  }
	  });

		if (location.hash) {
			toggleProgress(true);
			abortButton.innerText = "Convert Another";
			pollJob(location.hash.substring(1), true);
		}

		function resetFilesList(title) {
			while (filesList.childNodes.length > 0) filesList.removeChild(filesList.childNodes[0]);
			let header = document.createElement("h2");
			header.innerText = title;
			filesList.append(header);
		}

		function resetLog() {
			while (logLines.childNodes.length > 0) logLines.removeChild(logLines.childNodes[0]);
		}

		function upload() {
			currentRequest = new XMLHttpRequest();
			let data = new FormData();

			let files = fileSelector.files;
			console.log("uploading", files);
			for (let i = 0; i < files.length; i++) {
				data.append('files', files[i]);
			}

			currentRequest.addEventListener('load', e => {
				abortButton.innerText = "Convert Another";

				history.pushState(null, document.title, '#' + e.target.response.toString());
				pollJob(e.target.response)
			});

			currentRequest.upload.addEventListener('progress', e => {
		  	progressBar.value = Math.round((e.loaded / e.total) * 100);
			});

			currentRequest.responseType = 'json';

			currentRequest.onerror = function(e) {
				alert("Connection error during upload. Please try again.");
				abortButton.innerText = "Retry upload";
			};

			// Send POST request to the server side script
			currentRequest.open('post', url + 'upload');

			toggleProgress(true);
			abortButton.innerText = "Cancel Upload";

			currentRequest.send(data);
		}

		function toggleProgress(progressing) {
			if (progressing) {
				progressBar.value = 0;
				if (!logView.classList.contains("display-block")) logView.classList.add("display-block");
				if (!progressControls.classList.contains("display-block")) progressControls.classList.add("display-block");
				if (uploadControls.classList.contains("display-block")) uploadControls.classList.remove("display-block");
				if (infoBlurb.classList.contains("display-block")) infoBlurb.classList.remove("display-block");
				resetLog();
			} else {
				if (logView.classList.contains("display-block")) logView.classList.remove("display-block");
				if (progressControls.classList.contains("display-block")) progressControls.classList.remove("display-block");
				if (!uploadControls.classList.contains("display-block")) uploadControls.classList.add("display-block");
				if (filesList.classList.contains("display-block")) filesList.classList.remove("display-block");
				if (!infoBlurb.classList.contains("display-block")) infoBlurb.classList.add("display-block");
				history.pushState(null, document.title, '#');
			}
		}

		function pollJob(jobId, catchup = false) {
			if (jobId !== location.hash.substring(1)) return; // stop polling if we're not looking at this job

			fetch(url + 'job/' + jobId.toString() + (catchup ? '?catchup=1' : ''))
				.then(result => {
					if (result.ok) {
						result.json().then(job => {
							job.log.forEach(l => pushLog(l));

							if (job.files) {
								showFiles(job);
							}

							if (!job.done) pollJob(jobId);
						});
					} else {
						pushLog(result.statusText);
					}
				})
		}

		function pushLog(event) {
			let time = document.createElement("div");
			time.classList.add('time');
			let log = document.createElement("div");
			log.classList.add('message');

			let logRow = document.createElement("div");
			logRow.classList.add('log');

			let dateStamp;

			if (event instanceof String) {
				dateStamp = new Date();
				log.innerText = event;
				logRow.classList.add("error")
			} else {
				dateStamp = new Date(event.time);
				log.innerHTML = linkify(event.message);
				logRow.classList.add(event.type.toLowerCase())
			}

			time.innerText = '[' + dateStamp.toLocaleTimeString() + ']';

			logRow.append(time, log);

			if (!logView.classList.contains("display-block")) logView.classList.add("display-block");
			logLines.append(logRow);
		}

		function showFiles(job) {
			// hijack the files list to present downloads
			resetFilesList("Downloads");

			if (!filesList.classList.contains("display-block")) filesList.classList.add("display-block");

			job.files.forEach(f => {
			  let link = document.createElement("a");
		  	link.innerText = f;
		  	link.classList.add("name");
			  link.setAttribute("href", url + 'download/' + job.id + '/' + f);
			  let row = document.createElement("div");
			  row.classList.add("file");
			  row.append(link);
			  filesList.append(row);
			});
		}

		function linkify(message) {
			let linkPattern = /(\b(https?|ftp):\/\/[-A-Z0-9+&@#\/%?=~_|!:,.;]*[-A-Z0-9+&@#\/%=~_|])/gim;
			return message.replace(linkPattern, '<a href="$1" target="_blank">$1</a>');
		}
	});

</script>

<#include "../_footer.ftl">