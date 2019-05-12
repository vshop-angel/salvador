/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, canonLoc, Papa */

serposcope.googleGroupController = function () {

    var onResize = function () {
        $('.tab-content').css("min-height", serposcope.theme.availableHeight() - 150);
        serposcope.googleGroupControllerGrid.resize();
    };

    var configureModalFocus = function () {
        $('#new-target').on('shown.bs.modal', function () {
            $('#targetName').focus();
        });
        $('#new-target-bulk').on('shown.bs.modal', function () {
            $('#bulk-target').focus();
        });
        $('#new-search').on('shown.bs.modal', function () {
            $('#searchName').focus();
        });
        $('#new-search-bulk').on('shown.bs.modal', function () {
            $('#bulk-search').focus();
        });
    };

    var showNewSearchModal = function () {
        $('.modal').modal('hide');
        $('#new-search input[data-numeric-input]').each(function (index, input) {
            var numeric = input.getAttribute('data-numeric-input');
            if (numeric === null)
                return;
            new NumericInputMask(input, Number(numeric));
        });
        $('#new-search').modal();
        return false;
    };

    var showNewBulkSearchModal = function () {
        $('.modal').modal('hide');
        $('#new-search-bulk').modal();
        return false;
    };

    var showNewTargetModal = function () {
        $('.modal').modal('hide');
        $('#new-target').modal();
        return false;
    };

    var showNewReportModal = function () {
        $('.modal').modal('hide');
        console.log($('#new-report')[0]);
        $('#new-report').modal();
        return false;
    };

    var showNewBulkTargetModal = function () {
        $('.modal').modal('hide');
        $('#new-target-bulk').modal();
        return false;
    };

    var showNewEventModal = function (elt) {
        $('#modal-add-event').modal();
        return false;
    };

    var deleteReport = function (elt) {
        var id = $(elt.currentTarget).attr("data-id");
        var name = $("#report-" + id + " .report-name").html();
        var href = $(elt.currentTarget).attr("href");

        if (!confirm("Delete report \"" + name + "\"?")) {
            return false;
        }
        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('<input>', {
            'name': 'id[]',
            'value': id,
            'type': 'hidden'
        })).appendTo(document.body).submit();

        return false;
    };

    var deleteReports = function (elt) {
        if (!confirm("Delete reports?")) {
            return false;
        }

        $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('.chk-report'))
            .appendTo(document.body).submit();

        return false;
    };

    var deleteTarget = function (elt) {
        var id = $(elt.currentTarget).attr("data-id");
        var name = $("#target-" + id + " .target-name").html();
        var href = $(elt.currentTarget).attr("href");

        if (!confirm("Delete website \"" + name + "\" ?\nAll history will be erased.")) {
            return false;
        }

        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('<input>', {
            'name': 'id[]',
            'value': id,
            'type': 'hidden'
        })).appendTo(document.body).submit();

        return false;
    };

    var deleteTargets = function (elt) {
        if (!confirm("Delete targets ?\nAll history will be erased.")) {
            return false;
        }

        $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('.chk-target'))
            .appendTo(document.body).submit();

        return false;
    };

    var refreshVolumes = function (elt) {
        if (!confirm("Refresh search volumes?")) {
            return false;
        }

        var $form = $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        }));

        var ids = serposcope.googleGroupControllerGrid.getSelection();
        for (var i = 0; i < ids.length; i++) {
            $form.append($('<input>', {
                'name': 'id[]',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $form.appendTo(document.body).submit();
        return false;
    };

    var executeButtonAction = function (elt) {
        var $form = $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        }));

        var ids = serposcope.googleGroupControllerGrid.getSelection();
        for (var i = 0; i < ids.length; i++) {
            $form.append($('<input>', {
                'name': 'id[]',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $form.appendTo(document.body).submit();
        return false;
    };

    var deleteDeadKeywords = function (elt) {
        if (!confirmRemoveDeadKeywords()) {
            return false;
        }
        executeButtonAction(elt);
    };

    var deleteSearches = function (elt) {
        if (!confirm("Eliminar keywords\n\n¿Esto es irreversible, está seguro?")) {
            return false;
        }
        executeButtonAction(elt);
    };

    var bulkTargetSubmit = function () {
        var patterns = [];
        if ($('#bulk-target').val() == "") {
            alert("no target specified");
            return false;
        }

        var lines = $('#bulk-target').val().split(/\r?\n/);
        for (var i = 0; i < lines.length; i++) {
            lines[i] = lines[i].replace(/(^\s+)|(\s+$)/g, "");
            if (lines[i].length == 0) {
                continue;
            }

            patterns.push(lines[i]);
        }

        if (patterns.length == 0) {
            alert("no target specified");
            return false;
        }

        var form = $('<form>', {
            'action': $("#bulk-target-import").attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).append($('<input>', {
            'name': 'target-radio',
            'value': $('#new-target-bulk .target-radio:checked').val(),
            'type': 'hidden'
        }));

        var inputs = [];
        for (var i = 0; i < patterns.length; i++) {
            inputs.push($('<input>', {'name': 'name[]', 'value': patterns[i], 'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'pattern[]', 'value': patterns[i], 'type': 'hidden'})[0]);
        }
        form.append(inputs);
        form.appendTo(document.body).submit();
        return false;
    };

    var bulkSearchSubmit = function () {
        var cspVars = $('#csp-vars');
        var content = $('#bulk-search')
            .val()
            .trim();
        var KEYWORD_COLUMN = 0;
        var COUNTRY_COLUMN = 1;
        var DATACENTER_COLUMN = 2;
        var DEVICE_COLUMN = 3;
        var LOCAL_COLUMN = 4;
        var CUSTOM_COLUMN = 5;
        var COMPETITION_COLUMN = 6;
        var VOLUME_COLUMN = 7;
        var CATEGORY_COLUMN = 8;
        var TAG_COLUMN = 9;
        var CPC_COLUMN = 10;
        var VISIBILITY_COLUMN = 11;

        var identity = function (value) {
            return value;
        };

        var integerValidator = function (value) {
            var number = Number(value);
            if (isNaN(number)) {
                return null;
            } else if (number !== Math.round(number)) {
                return null;
            }
            return number;
        };

        var floatValidator = function (value) {
            var fixed = value.replace(getDecimalSeparator(), '.');
            var number = Number(fixed);
            if (isNaN(number)) {
                return null;
            }
            return number;
        };

        var booleanValidator = function (value) {
            return value.toLowerCase() === 'true';
        };

        var deviceValidator = function (value) {
            switch (value.toLowerCase()) {
                case "desktop":
                    return 0;
                case "mobile":
                    return 1;
                default:
                    return null;
            }
        };

        var collectedData = {
            keyword: {data: [], column: KEYWORD_COLUMN, validate: identity},
            country: {data: [], column: COUNTRY_COLUMN, validate: identity},
            datacenter: {data: [], column: DATACENTER_COLUMN, validate: identity},
            device: {data: [], column: DEVICE_COLUMN, validate: deviceValidator},
            local: {data: [], column: LOCAL_COLUMN, validate: identity},
            competition: {data: [], column: COMPETITION_COLUMN, validate: integerValidator},
            category: {data: [], column: CATEGORY_COLUMN, validate: identity},
            invisible: {data: [], column: VISIBILITY_COLUMN, validate: booleanValidator},
            volume: {data: [], column: VOLUME_COLUMN, validate: integerValidator},
            cpc: {data: [], column: CPC_COLUMN, validate: floatValidator},
            tag: {data: [], column: TAG_COLUMN, validate: identity},
            custom: {data: [], column: CUSTOM_COLUMN, validate: identity}
        };

        if (content.length === 0) {
            alert("no search specified");
            return false;
        }

        var lines = content.split(/\r?\n/);
        var index = 0;
        for (var j = 0; j < lines.length; j++) {
            lines[j] = lines[j].trim();
            if (lines[j].length === 0) {
                continue;
            }
            var parsed = Papa.parse(lines[j], {delimiter: ';'});
            var data = parsed.data;
            if (data.length !== 1) {
                alert("error at line " + j + " : " + lines[j]);
                return;
            }
            var params = data[0];
            for (var key in collectedData) {
                var item = collectedData[key];
                if (!collectedData.hasOwnProperty(key))
                    continue;
                var array = item.data;
                var raw = params[item.column];
                var value = item.validate(raw || cspVars.attr('data-default-' + key));
                if (value === null) {
                    switch (item.validate) {
                        case identity:
                            alert('Tipo incorrecto para `' + key + '\': `' + raw + '\' what the fuck?');
                            break;
                        case floatValidator:
                            alert('Tipo incorrecto para `' + key + '\': `' + raw + '\', se esperaba número');
                            break;
                        case integerValidator:
                            alert('Tipo incorrecto para `' + key + '\': `' + raw + '\', se esperaba entero');
                            break;
                        case deviceValidator:
                            alert('Tipo desconocido de dispositivo, error muy inesperado');
                            break;
                    }
                    return false;
                }
                array[index] = value;
            }
            index += 1;
        }

        var form = $('<form>', {
            'action': $("#bulk-search-import").attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        }));

        var inputs = [];
        for (var name in collectedData) {
            var array = collectedData[name].data;
            for (var k = 0; k < array.length; ++k) {
                inputs.push($('<input>', {'name': name + '[]', 'value': array[k], 'type': 'hidden'})[0]);
            }
        }
        form.append(inputs);
        form
            .appendTo(document.body)
            .submit()
        ;
        return false;
    };

    var deleteGroup = function (elt) {
        var name = $(elt.currentTarget).attr("data-name");
        var href = $(elt.currentTarget).attr("href");

        if (!confirm("Delete group \"" + name + "\" ?\nAll history will be erased.")) {
            return false;
        }

        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();

        return false;
    };

    var toggleEvent = function (elt) {
        $('#event-description-' + $(elt.currentTarget).attr('data-id')).toggleClass("hidden");
    };

    var deleteEvent = function (elt) {
        var day = $(elt.currentTarget).attr("data-day");
        var href = $(elt.currentTarget).attr("href");

        if (!confirm("Delete event \"" + day + "\" ?")) {
            return false;
        }

        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': 'day',
            'value': day,
            'type': 'hidden'
        })).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();

        return false;
    };

    var renameGroup = function (elt) {
        var href = $(elt.currentTarget).attr("href");
        var name = prompt("new group name");

        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': 'name',
            'value': name,
            'type': 'hidden'
        })).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();

        return false;
    };

    var renameTarget = function (elt) {
        var href = $(elt.currentTarget).attr("href");
        var id = $(elt.currentTarget).attr("data-id");
        var name = prompt("new name");

        $('<form>', {
            'action': href,
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': 'name',
            'value': name,
            'type': 'hidden'
        })).append($('<input>', {
            'name': 'id',
            'value': id,
            'type': 'hidden'
        })).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        })).appendTo(document.body).submit();

        return false;
    };

    var onRadioTargetChange = function () {
        $("#pattern").attr('placeholder', $(this).attr("data-help"));
    };

    var searchChecked = false;
    var checksearch = function () {
        $('.chk-search').prop('checked', searchChecked = !searchChecked ? 'checked' : '');
        return false;
    };

    var targetChecked = false;
    var checkTarget = function () {
        $('.chk-target').prop('checked', targetChecked = !targetChecked ? 'checked' : '');
        return false;
    };

    var exportSearches = function (elt) {
        var $form = $('<form>', {
            'action': $(elt.currentTarget).attr("data-action"),
            'method': 'post',
            'target': '_top'
        }).append($('<input>', {
            'name': '_xsrf',
            'value': $('#_xsrf').attr("data-value"),
            'type': 'hidden'
        }));

        var ids = serposcope.googleGroupControllerGrid.getSelection();
        for (var i = 0; i < ids.length; i++) {
            $form.append($('<input>', {
                'name': 'id[]',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $form.appendTo(document.body).submit();
        return false;
    };

    var loadAsyncCanonical = function () {
        $.ajax({
            url: '/assets/js/canonical-location.js',
            dataType: 'script',
            cache: true, // otherwise will get fresh copy every page load
            success: function () {
                configureSearchLocal();
            }
        });
    };

    var configureSearchLocal = function () {
        $('.search-local').typeahead({
            source: canonLoc,
            minLength: 2,
            items: 100,
            matcher: function (arg) {
                var item = arg;
                var array = this.query.split(" ");
                for (var i = 0; i < array.length; i++) {
                    if (item.indexOf(array[i].toLowerCase()) == -1) {
                        return false;
                    }
                }
                return true;
            },
            highlighter: function (item) {
                return item;
            }
        });
    };

    var renderScoreHistory = function () {
        $('.score-history-inline').sparkline("html", {tagValuesAttribute: "data-values"});
    };

    var configureTabs = function () {
        $('.nav-tabs a').on('shown.bs.tab', function (e) {
            window.location.hash = e.target.hash;
            window.scrollTo(0, 0);
            if (e.target.hash == "#tab-searches") {
                serposcope.googleGroupControllerGrid.resize();
            }
        });

        var url = document.location.toString();
        if (url.match('#')) {
            $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
        }
    };

    var view = function () {
        $(window).bind("load resize", onResize);
        $('input[name="day"]').daterangepicker({
            singleDatePicker: true,
            locale: {
                format: 'YYYY-MM-DD'
            }
        });
        configureModalFocus();
        $('.target-radio').change(onRadioTargetChange);
        $("#pattern").attr('placeholder', $('#target-domain').attr("data-help"));
        $('.btn-rename').click(renameGroup);
        $('.btn-rename-target').click(renameTarget);
        $('.toggle-event').click(toggleEvent);
        $('.btn-add-event').click(showNewEventModal);
        $('.btn-delete-event').click(deleteEvent);

        $('.btn-delete-group').click(deleteGroup);
        $('.btn-add-target').click(showNewTargetModal);
        $('.btn-add-report').click(showNewReportModal);
        $('.btn-add-target-bulk').click(showNewBulkTargetModal);
        $('.btn-add-search').click(showNewSearchModal);
        $('.btn-add-search-bulk').click(showNewBulkSearchModal);
        $('#bulk-search-import').click(bulkSearchSubmit);
        $('#bulk-target-import').click(bulkTargetSubmit);

        $('.btn-delete-target').click(deleteTarget);
        $('.btn-delete-report').click(deleteReport);

        $('#btn-chk-search').click(checksearch);
        $('#btn-chk-target').click(checkTarget);
        $('#btn-export-searches').click(exportSearches);
        $('#btn-delete-searches').click(deleteSearches);
        $('#btn-delete-dead-keywords').click(deleteDeadKeywords);
        $('#btn-refresh-volumes').click(refreshVolumes);
        $('#btn-delete-targets').click(deleteTargets);
        $('#btn-delete-reports').click(deleteReports);
        $('#table-target').stupidtable();
        renderScoreHistory();
        configureTabs();
        serposcope.googleGroupControllerGrid.render();
        loadAsyncCanonical();
    };

    var oPublic = {
        view: view
    };

    return oPublic;
}();
