function generateIdElement(data) {
    var wrap = $("<div/>");
    var span = $("<span/>").addClass("xroad-id");
    var spanText = [];
    var spanTitle = [];

    for (var item in data) {
        if (data.hasOwnProperty(item) && data[item]) {
            spanText.push(data[item]);
            spanTitle.push(item + ": " + data[item]);
        }
    }

    span.text(spanText.join(' : '));
    span.attr("title", spanTitle.join("<br>"));

    return wrap.html(span).html();
}

function clientName(name) {
    return name ||
        "&lt;" + _("clients.client_acl_subjects_tab.client_not_found") + "&gt;";
}

function groupDesc(desc) {
    return desc ||
        "&lt;" + _("clients.client_acl_subjects_tab.group_not_found") + "&gt;";
}

$(document).ready(function() {
    var serverNameEl = $("#server-info #server-names h1");
    var serverName = serverNameEl.text().split('/');
    serverNameEl.text(serverName[0] + " : " + serverName[serverName.length - 1]);

    var serverInfo = [
        "Environment: " + serverName[0],
        "Security server: " + serverName[3],
        "Owner Name: " + serverNameEl.data("owner-name"),
        "Owner Class: " + serverName[1],
        "Owner Code: " + serverName[2]
    ];

    var status = $("#server-status").data().status;

    if (status) {
        serverInfo.unshift("Status: " + status);
    }

    $("#server-info").attr("title", serverInfo.join("<br>"));
});
