/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, Slick */

serposcope.googleTargetControllerGrid = function () {

    var UNRANKED = 32767;
    var COL_WIDTH = 21;

    var COL_ID = 0;
    var COL_SEARCH = 1;
    var COL_SEARCH_KEYWORD = 0;
    var COL_SEARCH_COUNTRY = 1;
    var COL_SEARCH_DEVICE = 2;
    var COL_SEARCH_LOCAL = 3;
    var COL_SEARCH_DATACENTER = 4;
    var COL_SEARCH_CUSTOM = 5;
    var COL_BEST = 2;
    var COL_BEST_RANK = 0;
    var COL_BEST_DAY = 1;
    var COL_BEST_URL = 2;
    var COL_EVENTS = 3;
    var COL_EVENTS_TITLE = 0;
    var COL_EVENTS_DESCRIPTION = 1;
    var COL_RANK = 3;
    var COL_RANK_CURRENT = 0;
    var COL_RANK_PREVIOUS = 1;
    var COL_RANK_URL = 2;
    var COL_SEARCH_SETTINGS = 4;

    var grid = null;
    var dataView = null;
    var filter = new KeywordFilter();

    // provided by API
    var days = [];
    var data = [];
    var groupId = 1;

    var resize = function (height) {
        $('#google-target-table-container').css("min-height", (height) + "px");
        if (grid != null) {
            grid.resizeCanvas();
        }
    };

    var render = function () {
        if (document.getElementById("google-target-table-container") == null)
            return;
        $('#filter-apply').click(function () {
            applyFilter(filter, dataView);
        });
        $('#filter-reset').click(function () {
            resetFilter(filter, dataView);
        });
        fetchData();
    };

    var filterGrid = function (item) {
        var search = item[COL_SEARCH];
        if (search === 0)
            return true;
        var settings = item[COL_SEARCH_SETTINGS];
        var mapping = {
            keyword: search[COL_SEARCH_KEYWORD],
            device: search[COL_SEARCH_DEVICE],
            country: search[COL_SEARCH_COUNTRY],
            local: search[COL_SEARCH_LOCAL],
            datacenter: search[COL_SEARCH_DATACENTER],
            competition: settings.competition,
            tag: settings ? settings.tag : -1,
            category: settings ? settings.category : -1,
            volume: settings ? settings.volume : Number('nan')
        };
        return matchesAtLeastOneFilter(filter, mapping);
    };

    var fetchData = function () {
        var cspVars = $('#csp-vars');
        var groupId = cspVars.attr('data-group-id');
        var startDate = cspVars.data('start-date');
        var endDate = cspVars.data('end-date');
        if (startDate == "" || endDate == "") {
            $("#google-target-table-container").html("no data");
            return;
        }
        var targetId = cspVars.data('target-id');
        var url = "/google/" + groupId + "/target/" + targetId + "/ranks?startDate=" + startDate + "&endDate=" + endDate;
        $.getJSON(url)
            .done(function (json) {
                $(".ajax-loader").remove();
                data = json[0];
                days = json[1];
                renderGrid();
            }).fail(function (err) {
            $(".ajax-loader").remove();
            console.log("error", err);
            $("#google-target-table-container").html("error");
        });
    };

    var renderGrid = function () {
        var options = {
            explicitInitialization: true,
            enableColumnReorder: false,
            enableTextSelectionOnCells: true,
            forceFitColumns: false,
            forceSyncScrolling: true,
            autoExpandColumns: true
        };

        var columns = [{
            id: "visibility",
            name: '<i class="fa fa-eye"></i>',
            width: COL_WIDTH,
            sortable: false,
            formatter: formatVisibilityCell,
            toolTip: 'Visible a todos los usuarios?'
        }, {
            id: "search",
            name: 'Búsquedas',
            field: "id",
            width: 250,
            sortable: true,
            formatter: formatSearchCell
        }, {
            id: "volume",
            name: 'Volumen',
            field: "volume",
            width: 120,
            sortable: true,
            formatter: formatVolumeCell
        }];

        var wideColumns = [{
            id: "category",
            name: 'Categoría',
            field: "category",
            width: 150,
            sortable: true,
            formatter: formatCategory
        }, {
            id: "competition",
            name: 'Competencia',
            field: "competition",
            width: 130,
            sortable: true,
            formatter: formatCompetition
        }, {
            id: "cpc",
            name: 'CPC',
            field: "cpc",
            width: 70,
            sortable: true,
            formatter: formatCPC
        }, {
            id: "tag",
            name: 'Etiqueta',
            field: "tag",
            width: 150,
            sortable: true,
            formatter: formatTag
        }];
        var day = null;
        if (window.innerWidth > 1280) {
            for (var k = 0; k < wideColumns.length; ++k) {
                columns.push(wideColumns[k]);
            }
            for (var i = 0; i < days.length; i++) {
                day = days[i];
                columns.push({
                    id: day,
                    name: "<span data-toggle='tooltip' title='" + day + "' >" + day.split("-")[2] + "</span>",
                    field: i,
                    sortable: true,
                    width: COL_WIDTH,
                    formatter: formatGridCell
                });
            }
            columns.push({
                id: "best",
                name: '<i class="fa fa-trophy" data-toggle="tooltip" title="Best"></i>',
                field: "best",
                width: COL_WIDTH,
                formatter: formatBestCell,
                sortable: true,
            });
        } else {
            day = days[days.length - 1];
            columns.push({
                id: day,
                name: "<span data-toggle='tooltip' title='" + day + "' >" + day.split("-")[2] + "</span>",
                field: days.length - 1,
                sortable: true,
                width: COL_WIDTH,
                formatter: formatGridCell
            });
        }

        dataView = new Slick.Data.DataView();
        dataView.getItemMetadata = function (row) {
            if (row !== 0) {
                return null;
            }
            return {cssClasses: 'sub-header'};
        };

        grid = new Slick.Grid("#google-target-table-container", dataView, columns, options);

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
        dataView.setItems(data, 0);
        dataView.setFilter(filterGrid);
        dataView.endUpdate();
    };

    var compareAsNumbers = function (a, b) {
        var A = Number(a);
        var B = Number(b);
        if (isNaN(A)) {
            return isNaN(B) ? 0 : 1;
        } else if (isNaN(B)) {
            return -1;
        } else {
            return A - B;
        }
    };

    var compareAsStrings = function (A, B) {
        if (A === null || A === '') {
            return (B === null || B === '') ? 0 : 1;
        } else if (B === null || B === '') {
            return -1;
        } else if (A === B) {
            return 0;
        } else {
            return A > B ? 1 : -1;
        }
    };

    var compareVolumes = function (a, b) {
        return compareAsNumbers(a[COL_SEARCH_SETTINGS].volume, b[COL_SEARCH_SETTINGS].volume);
    };

    var compareTags = function (a, b) {
        return compareAsStrings(a[COL_SEARCH_SETTINGS].tag, b[COL_SEARCH_SETTINGS].tag);
    };

    var compareCategories = function (a, b) {
        return compareAsStrings(a[COL_SEARCH_SETTINGS].category, b[COL_SEARCH_SETTINGS].category);
    };

    var compareCpcs = function (a, b) {
        return compareAsNumbers(a[COL_SEARCH_SETTINGS].cpc, b[COL_SEARCH_SETTINGS].cpc);
    };

    var compareCompetitions = function (a, b) {
        return compareAsNumbers(a[COL_SEARCH_SETTINGS].competition, b[COL_SEARCH_SETTINGS].competition);
    };

    var gridSort = function (e, args) {
        var comparer = function (a, b) {
            var column = args.sortCol;
            if (a[COL_ID] === -1) {
                return args.sortAsc ? -1 : 1;
            }
            if (b[COL_ID] === -1) {
                return args.sortAsc ? 1 : -1;
            }
            switch (column.field) {
                case "id":
                    return a[COL_SEARCH][COL_SEARCH_KEYWORD] > b[COL_SEARCH][COL_SEARCH_KEYWORD] ? 1 : -1;
                case "volume":
                    return compareVolumes(a, b);
                case "tag":
                    return compareTags(a, b);
                case "cpc":
                    return compareCpcs(a, b);
                case "competition":
                    return compareCompetitions(a, b);
                case 'category':
                    return compareCategories(a, b);
                case "best":
                    return a[COL_BEST][COL_BEST_RANK] - b[COL_BEST][COL_BEST_RANK];
                default:
                    var aRank = a[COL_RANK][args.sortCol.field] === 0 ? UNRANKED : a[COL_RANK][args.sortCol.field][COL_RANK_CURRENT];
                    var bRank = b[COL_RANK][args.sortCol.field] === 0 ? UNRANKED : b[COL_RANK][args.sortCol.field][COL_RANK_CURRENT];
                    return aRank - bRank;
            }
        };
        dataView.sort(comparer, args.sortAsc);
    };

    var formatVisibilityCell = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return null;
        }
        var cell = rowData[COL_SEARCH_SETTINGS];
        if (!cell)
            return null;
        if (!cell.isAdminOnly) {
            return '<input type="checkbox" checked onclick="return false;">'
        } else {
            return '<input type="checkbox" onclick="return false;">'
        }
    };

    var formatCompetition = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return null;
        } else {
            var value = Number(rowData[COL_SEARCH_SETTINGS].competition);
            if (isNaN(value) || value === 0)
                value = '-';
            return '<div class="text-right">' + value + '</div>';
        }
    };

    var formatCPC = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return null;
        } else {
            var value = Number(rowData[COL_SEARCH_SETTINGS].cpc);
            if (isNaN(value) || value === 0)
                value = '-';
            value = value.toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2});
            return '<div class="text-right">' + value + '</div>';
        }
    };

    var formatCategory = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return null;
        } else {
            var value = rowData[COL_SEARCH_SETTINGS].category;
            if (value === null)
                value = '';
            return '<div style="text-align:left">' + value + '</div>';
        }
    };

    var formatTag = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return null;
        } else {
            var value = rowData[COL_SEARCH_SETTINGS].tag;
            if (value === null)
                value = '';
            return '<div style="text-align:left">' + value + '</div>';
        }
    };

    var formatVolumeCell = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return null;
        } else {
            var value = parseInt(rowData[COL_SEARCH_SETTINGS].volume);
            return '<div class="text-right">' + (isNaN(value) ? '-' : value) + '</div>';
        }
    };

    var formatSearchCell = function (row, col, unk, colDef, rowData) {
        var search = rowData[COL_SEARCH];
        if (search === 0) {
            return "<div class=\"text-left\">Agenda</div>";
        }
        var ret = "<div class=\"text-left\">";
        ret += "<i data-toggle=\"tooltip\" title=\"Country : " + search[COL_SEARCH_COUNTRY] + "\" class=\"fa fa-globe\" ></i>";
        if (search[COL_SEARCH_DEVICE] === "M") {
            ret += "<i data-toggle=\"tooltip\" title=\"mobile\" class=\"fa fa-mobile fa-fw\" ></i>";
        }
        if (search[COL_SEARCH_LOCAL] !== "") {
            ret += "<i data-toggle=\"tooltip\" title=\"" + search[COL_SEARCH_LOCAL] + "\" class=\"fa fa-map-marker fa-fw\" ></i>";
        }
        if (search[COL_SEARCH_DATACENTER] !== "") {
            ret += "<i data-toggle=\"tooltip\" title=\"Datacenter: " + search[COL_SEARCH_DATACENTER] + "\" class=\"fa fa-building fa-fw\" ></i>";
        }
        if (search[COL_SEARCH_CUSTOM] !== "") {
            ret += "<i data-toggle=\"tooltip\" title=\"" + search[COL_SEARCH_CUSTOM] + "\" class=\"fa fa-question-circle fa-fw\" ></i>";
        }
        ret += " <a href=\"/google/" + groupId + "/search/" + rowData[COL_ID] + "\" >" + search[COL_SEARCH_KEYWORD] + "</a>";
        ret += "</div>";
        return ret;
    };

    var formatGridCell = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return formatCalendarCell(row, null, unk, colDef, rowData);
        } else {
            return formatRankCell(row, null, unk, colDef, rowData);
        }
    };

    var formatCalendarCell = function (row, col, unk, colDef, rowData) {
        var event = rowData[COL_EVENTS][colDef.field];
        if (event === 0) {
            return null;
        }
        return '<div class="text-center pointer" rel="popover" data-toggle="tooltip" ' +
            'title="' + serposcope.utils.escapeHTMLQuotes(event[COL_EVENTS_TITLE]) + '" ' +
            'data-content="' + serposcope.utils.escapeHTMLQuotes(event[COL_EVENTS_DESCRIPTION]) + '" >' +
            '<i class="fa fa-calendar" ></i>' +
            '</div>';
    };

    var formatRankCell = function (row, col, unk, colDef, rowData) {
        var rank = rowData[COL_RANK][colDef.field];
        var diffText = "", diffClass = "";
        var bestClass = "", bestText = "";
        var rankText = "";
        if (rank === 0) {
            rankText = "-";
            diffText = "out";
            diffClass = "minus";
        } else {
            rankText = rank[COL_RANK_CURRENT];
            var rankDiff = rank[COL_RANK_PREVIOUS] - rank[COL_RANK_CURRENT];
            if (rank[COL_RANK_PREVIOUS] === UNRANKED && rank[COL_RANK_CURRENT] !== UNRANKED) {
                diffText = "in";
                diffClass = "plus";
            } else if (Number(rankDiff) === 0) {
                diffText = "=";
            } else if (rankDiff > 0) {
                diffText = "+" + rankDiff;
                diffClass = "plus";
            } else {
                diffText = rankDiff;
                diffClass = "minus";
            }

            if (rowData[COL_BEST] !== 0 && rowData[COL_BEST][COL_BEST_RANK] === rank[COL_RANK_CURRENT]) {
                bestClass = "best-cell";
                bestText = " (best)";
            }
        }

        var rankUrl = rank[COL_RANK_URL] == null ? "not provided" : serposcope.utils.escapeHTMLQuotes(rank[COL_RANK_URL]);
        return '<div class="pointer diff-' + diffClass + ' ' + bestClass + '" ' +
            'rel="popover" data-toggle="tooltip" ' +
            'data-tt="' + diffText + bestText + '" ' +
            'data-pt="' + days[col - 1] + '" ' +
            'data-content="' + rankUrl + '" ' +
            '>' + rankText + '</div>';
    };

    var formatBestCell = function (row, col, unk, colDef, rowData) {
        if (row === 0) {
            return "";
        }
        var best = rowData[COL_BEST];
        var rankText = (best[COL_BEST_RANK] === UNRANKED ? "-" : best[COL_BEST_RANK]);
        return '<div class="pointer best-cell" ' +
            'rel="popover" data-toggle="tooltip" ' +
            'title="' + best[COL_BEST_DAY] + '" ' +
            'data-content="' + serposcope.utils.escapeHTMLQuotes(best[COL_BEST_URL]) + '" ' +
            '>' + rankText + '</div>';
    };

    var oPublic = {
        resize: resize,
        render: render
    };

    return oPublic;

}();
