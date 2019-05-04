/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, Slick */

var inteligenciaSEOEditKeyword = function (event, id, category, volume, isAdminOnly) {
    var target = $(event.currentTarget);
    var content = $('#edit-keyword');
    // Hide the modal if visible
    $('.modal').modal('hide');
    // Build the new modal
    $(content).find('#searchEditId').val(target.attr('data-id'));
    $(content).find('#searchEditCategory').val(category);
    $(content).find('#searchEditVolume').val(volume);
    if (isAdminOnly === false) {
        $(content).find('input[name="onlyAdmin"][value="false"]').attr('checked', 'checked');
    } else {
        $(content).find('input[name="onlyAdmin"][value="true"]').attr('checked', 'checked');
    }
    content.modal();
    return false;
};

var startEditVolume = function (event) {
    var target = event.target;
    var value = target.value;
    var searchId = target.getAttribute('data-search-id');
    target.readOnly = false;
    target.onkeyup = function (keyboard) {
        switch (keyboard.keyCode) {
            case 13:
                target.readOnly = true;
                // Try to update the volume
                setVolume(target, searchId, target.value, function () {
                    target.value = value;
                }, function() {
                    target.blur();
                });
                break;
            case 27:
                target.readOnly = true;
                target.value = value;
                break;
        }
    };
    return false;
};

serposcope.googleGroupControllerGrid = function () {

    var grid = null;
    var dataView = null;

    var filter = {
        keyword: '',
        country: '',
        device: '',
        local: '',
        datacenter: '',
        custom: '',
        category: -1
    };

    // provided by API
    var data = [];
    var groupId = 1;
    // end    

    var resize = function () {
        $('#group-searches-grid').css("height", serposcope.theme.availableHeight() - 250);
        if (grid != null) {
            grid.resizeCanvas();
        }
    };

    var render = function () {
        groupId = $('#csp-vars').attr('data-group-id');
        $('#filter-apply').click(applyFilter);
        $('#filter-reset').click(resetFilter);
        fetchData();
    };

    var fetchData = function () {
        $.getJSON('/google/' + $('#csp-vars').data('group-id') + '/search/list')
            .done(function (json) {
                $(".ajax-loader").remove();
                data = json;
                renderGrid();
            }).fail(function (err) {
            $(".ajax-loader").remove();
            $("#group-searches-grid").html("error");
        });
    };

    var renderGrid = function () {
        var options = {
            explicitInitialization: true,
            enableColumnReorder: false,
            enableTextSelectionOnCells: true,
            forceFitColumns: true
        };

        var toString = function (value) {
            return '\'' + value + '\'';
        };

        var visibilityTableOption = function (row, col, unk, colDef, rowData) {
            if (!rowData.isAdminOnly) {
                return '<input type="checkbox" checked onchange="setKeywordVisibility(this, ' + rowData.id + ')">'
            } else {
                return '<input type="checkbox" onchange="setKeywordVisibility(this, ' + rowData.id + ')">'
            }
        };

        var searchesTableOptions = function (row, col, unk, colDef, rowData) {
            var args = [rowData.id, toString(rowData.category), rowData.volume, rowData.isAdminOnly].join(',');
            return '<span class="search-options">' +
                '<a onclick="return inteligenciaSEOEditKeyword(event, ' + args + ')" data-id="' + rowData.id + '" id="btn-edit-keyword" class="edit" title="Edit"><i class="glyphicon glyphicon-edit"></i></a>' +
                '</span>';
        };

        var checkboxSelector = new Slick.CheckboxSelectColumn({cssClass: "slick-cell-checkboxsel"});
        var columns = [checkboxSelector.getColumnDefinition(), {
            id: "options",
            width: 22,
            name: '<i class="fa fa-cogs"></i>',
            formatter: searchesTableOptions
        }, {
            id: "visibility",
            width: 22,
            name: '<i class="fa fa-eye"></i>',
            formatter: visibilityTableOption,
            cssClass: 'slick-cell-checkboxsel'
        }, {
            id: "keyword", field: "keyword", minWidth: 200, sortable: true, name: 'Keyword', formatter: formatKeyword
        }, {
            id: "device", field: "device", minWidth: 100, sortable: true, name: 'Device', formatter: formatDevice
        }, {
            id: "country", field: "country", minWidth: 60, sortable: true, name: 'Country', formatter: formatCountry
        }, {
            id: "datacenter", field: "datacenter", minWidth: 100, sortable: true, name: 'Datacenter'/*, formatter: formatDatacenter,*/
        }, {
            id: "local", field: "local", minWidth: 200, sortable: true, name: 'Local'/*, formatter: formatLocal,*/
        }, {
            id: "custom", field: "custom", minWidth: 200, sortable: true, name: 'Custom'/*, formatter: formatCustom*/
        }, {
            id: "category", field: "category", minWidth: 150, sortable: true, name: 'Category'/*, formatter: formatCustom*/
        }, {
            id: "volume", field: "volume", minWidth: 50, sortable: true, name: 'Volume', formatter: formatVolume
        }];

        dataView = new Slick.Data.DataView();
        grid = new Slick.Grid("#group-searches-grid", dataView, columns, options);
        grid.registerPlugin(checkboxSelector);
        grid.setSelectionModel(new Slick.RowSelectionModel({selectActiveRow: false}));
        grid.onSort.subscribe(gridSort);

        dataView.onRowCountChanged.subscribe(function (e, args) {
            grid.updateRowCount();
            grid.render();
        });
        dataView.onRowsChanged.subscribe(function (e, args) {
            grid.invalidateRows(args.rows);
            grid.render();
        });

        grid.init();

        dataView.beginUpdate();
        dataView.setItems(data);
        dataView.setFilter(filterGrid);
        dataView.syncGridSelection(grid, false);
        dataView.endUpdate();
    };

    var gridSort = function (e, args) {
        var comparer = function (a, b) {
            return a[args.sortCol.field] > b[args.sortCol.field] ? 1 : -1;
        };
        dataView.sort(comparer, args.sortAsc);
    };

    var applyFilter = function () {
        var category = $('#filter-category').val() || '-1';

        filter.keyword = $('#filter-keyword').val().toLowerCase();
        filter.country = $('#filter-country').val().toLowerCase();
        filter.device = $('#filter-device').val();
        filter.local = $('#filter-local').val().toLowerCase();
        filter.datacenter = $('#filter-datacenter').val().toLowerCase();
        filter.custom = $('#filter-custom').val().toLowerCase();
        filter.category = category.toLowerCase();

        dataView.refresh();
    };

    var resetFilter = function () {
        $('#filter-keyword').val('');
        $('#filter-country').val('');
        $('#filter-device').val('');
        $('#filter-local').val('');
        $('#filter-datacenter').val('');
        $('#filter-custom').val('');
        $('#filter-category').val(-1);
        applyFilter();
    };

    var filterGrid = function (item) {
        if (filter.keyword !== '' && item.keyword.toLowerCase().indexOf(filter.keyword) === -1) {
            return false;
        }

        if (filter.device !== '' && item.device != filter.device) {
            return false;
        }

        if (filter.country !== '' && item.country.toLowerCase() != filter.country) {
            return false;
        }

        if (filter.local !== '' && item.local.toLowerCase().indexOf(filter.local) === -1) {
            return false;
        }

        if (filter.datacenter !== '' && item.datacenter != filter.datacenter) {
            return false;
        }

        if (filter.custom !== '' && item.custom.toLowerCase().indexOf(filter.custom) === -1) {
            return false;
        }

        return !(filter.category != -1 && item.category.toLowerCase().indexOf(filter.category) === -1);
    };

    var formatKeyword = function (row, col, unk, colDef, rowData) {
        return "&nbsp;<a href=\"/google/" + groupId + "/search/" + rowData.id + "\" >" + rowData.keyword + "</a>";
    };

    var formatDevice = function (row, col, unk, colDef, rowData) {
        if (rowData.device === 'M') {
            return "<i data-toggle=\"tooltip\" title=\"mobile\" class=\"fa fa-mobile fa-fw\" ></i>";
        } else {
            return "<i data-toggle=\"tooltip\" title=\"desktop\" class=\"fa fa-desktop fa-fw\" ></i>";
        }
    };

    var formatCountry = function (row, col, unk, colDef, rowData) {
        if (rowData.country === '__') {
            return "__ (no country)";
        } else {
            return rowData.country;
        }
    };

    var formatVolume = function (row, col, unk, colDef, rowData) {
        var searchId = rowData.id;
        return '<input class="volume" value="' + Number(rowData.volume) + '" data-search-id="' + searchId + '"' +
            ' title="Double click to edit. Then Return to submit or Escape to cancel"' +
            ' ondblclick="startEditVolume(event)" readonly/>';
    };

    var getSelection = function () {
        return grid.getSelectedRows().map(dataView.getItem).map(function (x) {
            return x.id;
        });
    };

    var genFakeSearch = function (i) {
        return {
            id: i,
            keyword: "search#" + parseInt(Math.random() * 100000),
            country: parseInt(Math.random() * 5) == 0 ? '__' : 'fr',
            device: parseInt(Math.random() * 5) == 0 ? 'M' : 'D',
            local: parseInt(Math.random() * 5) == 0 ? 'Paris' : '',
            datacenter: parseInt(Math.random() * 5) == 0 ? '1.2.3.4' : '',
            custom: parseInt(Math.random() * 5) == 0 ? 'hl=fr' : ''
        };
    };

    var setFakeData = function () {
        var searches = 10000;
        for (var i = 0; i < searches; i++) {
            data.push(genFakeSearch(i));
        }
    };

    var oPublic = {
        resize: resize,
        render: render,
        getSelection: getSelection
    };

    return oPublic;

}();
