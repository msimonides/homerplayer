$('.faq-toc a').click(highlightOnEvent);
$('.faq a.self').click(highlightOnEvent);

function highlightOnEvent(event) {
	var targetHref = this.getAttribute('href');
	var targetId = targetHref.substr(targetHref.indexOf('#') + 1);
	highlight(targetId);
}

function highlight(id) {
	var answer = $('#' + id + " + dd");
	answer.addClass('highlight');
	setTimeout(function() {
		answer.removeClass('highlight');
	}, 500);

	$('dt.selected').removeClass('selected');
	$('#' + id).addClass('selected');
}

$(document).ready(function() {
	var hash = document.location.hash;
	if (hash && hash.substr)
		highlight(hash.substr(1));
});
