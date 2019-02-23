/* 
 * Serposcope - SEO rank checker https://serposcope.serphacker.com/
 * 
 * Copyright (c) 2016 SERP Hacker
 * @author Pierre Nogues <support@serphacker.com>
 * @license https://opensource.org/licenses/MIT MIT License
 */

/* global serposcope, canonLoc, Papa */

serposcope.googleGroupController = function () {
    
    var onResize = function(){
        $('.tab-content').css("min-height", serposcope.theme.availableHeight() - 150);
        serposcope.googleGroupControllerGrid.resize();
    };
    
    var configureModalFocus = function() {
        $('#new-target').on('shown.bs.modal', function(){ $('#targetName').focus(); });
        $('#new-target-bulk').on('shown.bs.modal', function(){ $('#bulk-target').focus(); });
        $('#new-search').on('shown.bs.modal', function(){ $('#searchName').focus(); });
        $('#new-search-bulk').on('shown.bs.modal', function(){ $('#bulk-search').focus(); });
    };
    
    var showNewSearchModal = function(){
        $('.modal').modal('hide');
        $('#new-search').modal();
        return false;
    };
    
    var showNewBulkSearchModal = function(){
        $('.modal').modal('hide');
        $('#new-search-bulk').modal();
        return false;
    };
    
    var showNewTargetModal = function(){
        $('.modal').modal('hide');
        $('#new-target').modal();
        return false;
    };

    var showNewReportModal = function(){
        $('.modal').modal('hide');
        console.log($('#new-report')[0]);
        $('#new-report').modal();
        return false;
    };

    var showNewBulkTargetModal = function(){
        $('.modal').modal('hide');
        $('#new-target-bulk').modal();
        return false;
    };    
    
    var showNewEventModal = function(elt){
        $('#modal-add-event').modal();
        return false;
    };

    var deleteReport = function(elt){
        var id = $(elt.currentTarget).attr("data-id");
        var name = $("#report-" + id +" .report-name").html();
        var href= $(elt.currentTarget).attr("href");

        if(!confirm("Delete report \"" + name + "\"?")){
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

    var deleteReports = function(elt){
        if(!confirm("Delete reports?")){
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

    var deleteTarget = function(elt){
        var id = $(elt.currentTarget).attr("data-id");
        var name = $("#target-" + id +" .target-name").html();
        var href= $(elt.currentTarget).attr("href");
        
        if(!confirm("Delete website \"" + name + "\" ?\nAll history will be erased.")){
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

    var deleteTargets = function(elt){
        if(!confirm("Delete targets ?\nAll history will be erased.")){
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
    
    var deleteSearches = function(elt){
        if(!confirm("Delete searches ?\nAll history will be erased.")){
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
        for(var i=0; i <ids.length; i++){
           $form.append($('<input>', {
                'name': 'id[]',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $form.appendTo(document.body).submit();
        return false;
    };
    
    var bulkTargetSubmit = function() {
        var patterns = [];
        if($('#bulk-target').val() == ""){
            alert("no target specified");
            return false;
        }
        
        var lines = $('#bulk-target').val().split(/\r?\n/);
        for(var i = 0; i< lines.length; i++){
            lines[i] = lines[i].replace(/(^\s+)|(\s+$)/g,"");
            if(lines[i].length == 0){
                continue;
            }
            
            patterns.push(lines[i]);
        }
        
        if(patterns.length == 0){
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
        for(var i = 0; i< patterns.length; i++){
            inputs.push($('<input>', {'name': 'name[]','value': patterns[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'pattern[]','value': patterns[i],'type': 'hidden'})[0]);
        }        
        form.append(inputs);
        form.appendTo(document.body).submit();
        return false;
    };
    
    var bulkSearchSubmit = function(){
        var keyword = [], country = [], datacenter = [], device = [], local = [], custom = [];
        if($('#bulk-search').val() == ""){
            alert("no search specified");
            return false;
        }
        
        var lines = $('#bulk-search').val().split(/\r?\n/);
        
        for(var i = 0; i< lines.length; i++){
            lines[i] = lines[i].replace(/(^\s+)|(\s+$)/g,"");
            if(lines[i].length == 0){
                continue;
            }
            
            var params = Papa.parse(lines[i]);
            if(params.data.length != 1){
                alert("error at line " + i + " : " + lines[i]);
                return;
            }
            params = params.data[0];
            keyword[i] = params[0];
            country[i] = params.length > 1 ? params[1] : $('#csp-vars').attr('data-default-country');
            datacenter[i] = params.length > 2 ? params[2]  : $('#csp-vars').attr('data-default-datacenter');
            if(params.length > 3){
                switch(params[3].toLowerCase()){
                    case "desktop":
                        device[i] = 0;
                        break;
                    case "mobile":
                        device[i] = 1;
                        break;  
                    default:
                        alert(params[3] + " is an invalid device type, valid values : desktop, mobile");
                        return false;
                }
            } else {
                device[i] = parseInt($('#csp-vars').attr('data-default-device'));
            }
            local[i] = params.length > 4 ? params[4]  : $('#csp-vars').attr('data-default-local');
            custom[i] = params.length > 5 ? params[5]  : $('#csp-vars').attr('data-default-custom');
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
        for(var i=0; i<keyword.length; i++){
            if(typeof(keyword[i]) == "undefined"){
                continue;
            }
            inputs.push($('<input>', {'name': 'keyword[]','value': keyword[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'country[]','value': country[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'datacenter[]','value': datacenter[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'device[]','value': device[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'local[]','value': local[i],'type': 'hidden'})[0]);
            inputs.push($('<input>', {'name': 'custom[]','value': custom[i],'type': 'hidden'})[0]);
        }
        form.append(inputs);
        form.appendTo(document.body).submit();
        
        return false;
    };
    
    var deleteGroup = function(elt) {
        var name = $(elt.currentTarget).attr("data-name");
        var href= $(elt.currentTarget).attr("href");
        
        if(!confirm("Delete group \"" + name + "\" ?\nAll history will be erased.")){
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
    
    var toggleEvent = function(elt) {
        $('#event-description-' + $(elt.currentTarget).attr('data-id')).toggleClass("hidden");
    };
    
    var deleteEvent = function(elt){
        var day = $(elt.currentTarget).attr("data-day");
        var href= $(elt.currentTarget).attr("href");
        
        if(!confirm("Delete event \"" + day + "\" ?")){
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
    
    var renameGroup = function(elt){
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
    
    var renameTarget = function(elt){
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
    
    var onRadioTargetChange = function(){
        $("#pattern").attr('placeholder', $(this).attr("data-help"));
    };
    
    var searchChecked = false;
    var checksearch = function(){
        $('.chk-search').prop('checked',searchChecked=!searchChecked ? 'checked' : '');
        return false;
    };
    
    var targetChecked = false;
    var checkTarget = function(){
        $('.chk-target').prop('checked',targetChecked=!targetChecked ? 'checked' : '');
        return false;
    };    
    
    var exportSearches = function(elt){
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
        for(var i=0; i <ids.length; i++){
           $form.append($('<input>', {
                'name': 'id[]',
                'value': ids[i],
                'type': 'hidden'
            }));
        }
        $form.appendTo(document.body).submit();
        return false;
    };
    
    var loadAsyncCanonical = function() {
        $.ajax({
            url: '/assets/js/canonical-location.js',
            dataType: 'script',
            cache: true, // otherwise will get fresh copy every page load
            success: function () {
                configureSearchLocal();
            }
        });
    };
    
    var configureSearchLocal = function(){
        $('.search-local').typeahead({
            source: canonLoc,
            minLength: 2,
            items: 100,
            matcher: function(arg){
                var item = arg;
                var array = this.query.split(" ");
                for(var i=0; i<array.length; i++){
                    if( item.indexOf(array[i].toLowerCase()) == -1){
                        return false;
                    }
                }
                return true;
            },
            highlighter: function (item) {return item;}
        });
    };
    
    var renderScoreHistory = function() {
        $('.score-history-inline').sparkline("html", {tagValuesAttribute: "data-values"});        
    };
    
    var configureTabs = function() {
        $('.nav-tabs a').on('shown.bs.tab', function (e) {
            window.location.hash = e.target.hash;
            window.scrollTo(0, 0);
            if(e.target.hash == "#tab-searches"){
                serposcope.googleGroupControllerGrid.resize();
            }
        });
        
        var url = document.location.toString();
        if (url.match('#')) {
            $('.nav-tabs a[href="#' + url.split('#')[1] + '"]').tab('show');
        } 
    };
    
    var view = function() {
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
