/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, Slick */
serposcope.googleGroupControllerGrid = function () {

    var grid = null;
    var dataView = null;

    var filter = new KeywordFilter();

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

    var filterGrid = function(item) {
        return matchesAtLeastOneFilter(filter, item);
    };

    var render = function () {
        groupId = $('#csp-vars').attr('data-group-id');
        $('#filter-apply').click(function () {
            applyFilter(filter, dataView);
        });
        $('#filter-reset').click(function () {
            resetFilter(filter, dataView);
        });
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
            forceFitColumns: true,
            forceSyncScrolling: true
        };

        var toString = function (value) {
            return '\'' + value + '\'';
        };


        var visibilityTableOption = function (row, col, unk, colDef, rowData) {
            var onchange = 'setKeywordVisibility(this)';
            if (!rowData.isAdminOnly) {
                return '<input type="checkbox" checked onclick="return ' + onchange + '" data-search-id="' + rowData.id + '">'
            } else {
                return '<input type="checkbox" onclick="return ' + onchange + '" data-search-id="' + rowData.id + '">'
            }
        };

        var searchesTableOptions = function (row, col, unk, colDef, rowData) {
            var args = [rowData.id, toString(rowData.category), rowData.volume, rowData.isAdminOnly].join(',');
            return '<span class="search-options">' +
                '<a onclick="return inteligenciaSEOEditKeyword(event, ' + args + ')" data-id="' + rowData.id +
                '" id="btn-edit-keyword" class="edit" title="Edit"><i class="glyphicon glyphicon-edit"></i></a>' +
                '</span>';
        };

        var checkboxSelector = new Slick.CheckboxSelectColumn({cssClass: "slick-cell-checkboxsel"});
        var columns = [checkboxSelector.getColumnDefinition(), {
            id: "visibility",
            width: 22,
            name: '<i class="fa fa-eye"></i>',
            cssClass: 'slick-cell-checkboxsel',
            formatter: visibilityTableOption
        }, {
            id: "keyword",
            field: "keyword",
            minWidth: 200,
            sortable: true,
            name: 'Palabra Clave',
            formatter: formatKeyword
        }, {
            id: "volume", field: "volume", minWidth: 50, sortable: true, name: 'Volumen', formatter: formatVolume
        }, {
            id: "category",
            field: "category",
            minWidth: 100,
            sortable: true,
            name: 'Categoría',
            formatter: formatCategory,
        }, {
            id: "competition",
            field: "competition",
            minWidth: 100,
            sortable: true,
            name: 'Competencia',
            formatter: formatCompetition,
        }, {
            id: "cpc", field: "cpc", minWidth: 80, sortable: true, name: 'CPC', formatter: formatCPC,
        }, {
            id: "tag", field: "tag", minWidth: 80, sortable: true, name: 'Etiqueta', formatter: formatTag,
        }, {
            id: "device", field: "device", minWidth: 100, sortable: true, name: 'Dispositivo', formatter: formatDevice
        }, {
            id: "country", field: "country", minWidth: 60, sortable: true, name: 'País', formatter: formatCountry
        }, {
            id: "datacenter", field: "datacenter", minWidth: 100, sortable: true, name: 'Datacenter'/*, formatter: formatDatacenter,*/
        }, {
            id: "local", field: "local", minWidth: 200, sortable: true, name: 'Local'/*, formatter: formatLocal,*/
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
            var A = Number(a[args.sortCol.field]);
            var B = Number(b[args.sortCol.field]);
            if (!isNaN(A) && !isNaN(B)) {
                return A - B;
            } else if (isNaN(A) && !isNaN(B)) {
                return 1;
            } else if (isNaN(B) && !isNaN(A)) {
                return -1;
            } else {
                return a[args.sortCol.field] > b[args.sortCol.field] ? 1 : -1;
            }
        };
        dataView.sort(comparer, args.sortAsc);
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
        return createEditableInputCell(Number(rowData.volume), rowData.id, 'setVolume', 0);
    };

    var formatCPC = function (row, col, unk, colDef, rowData) {
        var value = Number(rowData.cpc);
        if (isNaN(value) || value === 0) {
            value = '-';
        } else {
            value = value.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
        }
        return createEditableInputCell(value, rowData.id, 'setCPC', 2);
    };

    var formatTag = function (row, col, unk, colDef, rowData) {
        // var searchId = rowData.id;
        var value = rowData.tag || '';
        return createEditableInputCell(value, rowData.id, 'setTag');
    };

    var formatCategory = function (row, col, unk, colDef, rowData) {
        // var searchId = rowData.id;
        var value = rowData.category || '';
        return createEditableInputCell(value, rowData.id, 'setCategory');
    };

    var formatCompetition = function (row, col, unk, colDef, rowData) {
        var value = Number(rowData.competition);
        if (isNaN(value) || value === 0)
            value = '-';
        return createEditableInputCell(value, rowData.id, 'setCompetition', 0)
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
            competition: parseInt(Math.random() * 5) == 0 ? 'hl=fr' : ''
        };
    };

    var setFakeData = function () {
        var searches = 10000;
        for (var i = 0; i < searches; i++) {
            data.push(genFakeSearch(i));
        }
    };

    var setFieldValue = function (searchId, field, value) {
         var item = null;
        var seek = Number(searchId);
        for (var i = 0; i < data.length && item === null; ++i) {
            if (data[i].id === seek) {
                item = data[i];
            }
        }
        // We should probably not ignore this error
        if (item === null)
            return;
        item[field] = value;
    };

    var setSearchVisibility = function (searchId, invisible) {
        setFieldValue(searchId, 'isAdminOnly', !invisible);
    };

    return {
        resize: resize,
        render: render,
        setSearchVisibility: setSearchVisibility,
        setFieldValue: setFieldValue,
        getSelection: getSelection
    };
}();
