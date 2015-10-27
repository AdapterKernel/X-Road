(function(MEMBER_SEARCH_DIALOG, $, undefined) {

    var oMemberSearch;

    function initMemberSearchTable(
            securityServerCode, subsystemsOnly, onSuccess) {

        var opts = defaultTableOpts();
        opts.bServerSide = true;
        opts.bDestroy = true;
        opts.bScrollCollapse = true;
        opts.bScrollInfinite = true;
        opts.sScrollY = "100px";
        opts.sDom = "<'dataTables_header'f<'clearer'>>tp";
        opts.aoColumns = [
            { "mData": "name" },
            { "mData": "member_code" },
            { "mData": "member_class" },
            { "mData": "subsystem_code" },
            { "mData": "xroad_instance" },
            { "mData": "type" },
        ];

        opts.sAjaxSource = "members/member_search";
        opts.fnDrawCallback = function() {
            if (!oMemberSearch.getFocus()) {
                $("#member_search_select").disable();
            }
        };

        opts.fnServerParams = function(aoData) {
            if (securityServerCode) {
                aoData.push({
                    "name": "securityServerCode",
                    "value": securityServerCode
                });
            }

            if (subsystemsOnly) {
                aoData.push({
                    "name": "subsystemsOnly",
                    "value": "true"
                });
            }
        }

        opts.aaSorting = [[1, 'asc']];

        oMemberSearch = $("#member_search").dataTable(opts);
        oMemberSearch.fnSetFilteringDelay(600);

        oMemberSearch.on("click", "tbody tr", function(ev) {
            if (oMemberSearch.setFocus(0, this)) {
                $("#member_search_select").enable();
            }
        });

        oMemberSearch.unbind("dblclick")
                .on("dblclick", "tbody tr", function(ev) {
            $("#member_search_dialog").dialog("close");
            onSuccess(oMemberSearch.fnGetData(this));
        });
    }

    MEMBER_SEARCH_DIALOG.open =
            function(securityServerCode, subsystemsOnly, onSuccess) {

        $("#member_search_dialog").initDialog({
            modal: true,
            title: _("members.search"),
            height: 400,
            minHeight: 400,
            width: 800,
            buttons: [
                { id: "member_search_select",
                  text: _("common.select"),
                  click: function() {
                      $(this).dialog("close");
                      onSuccess(oMemberSearch.getFocusData());
                  }
                },
                { text: _("common.cancel"),
                  click: function() {
                      $(this).dialog("close");
                  }
                }
            ],
            open: function() {
                $("#member_search_select").disable();

                initMemberSearchTable(securityServerCode, subsystemsOnly, onSuccess);
            }
        });
    }

}(window.MEMBER_SEARCH_DIALOG = window.MEMBER_SEARCH_DIALOG || {}, jQuery));
