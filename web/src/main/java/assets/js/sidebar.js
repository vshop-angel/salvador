/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope */

serposcope.sidebar = function () {

    var groupSuggest = function (query, cb) {
        $.getJSON('/groups/suggest?query=' + encodeURIComponent(query)).success(cb);
    };

    var groupSelected = function (group) {
        switch (group.module) {
            case 0:
            case "GOOGLE":
                window.location = "/google/" + group.id;
                break;
            case 1:
            case "TWITTER":
                window.location = "/twitter/" + group.id;
                break;
            case 2:
            case "GITHUB":
                window.location = "/github/" + group.id;
                break;
        }
    };

    var groupHighlighted = function (group) {
        var icon = "fa-google";
        switch (group.module) {
            case 0:
            case "GOOGLE":
                icon = "fa-google";
                break;
            case 1:
            case "TWITTER":
                icon = "fa-twitter-square";
                break;
            case 2:
            case "GITHUB":
                icon = "fa-github-square";
                break;
        }
        return "<i class=\"fab " + icon + " fa-sm fa-fw\"></i> " + group.name;
    };

    var oPublic = {
        groupSelected: groupSelected,
        groupHighlighted: groupHighlighted,
        groupSuggest: groupSuggest
    };

    return oPublic;

}();