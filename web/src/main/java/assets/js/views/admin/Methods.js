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

var confirmRemoveDeadKeywords = function () {
    return confirm("El sistema va a buscar las keywords que nunca hayan tenido actividad y eliminarlas para así optimizar los análisis.\n\n¿Esto es irreversible y perderá los datos de todas aquellas keywords que sean elegidas por el algoritmo, está seguro?");
};

var getDecimalSeparator = function () {
    var string = (1.1).toLocaleString();
    return string.substr(1, 1);
};

var loadFile = function (input) {
    var reader = new FileReader();
    reader.onload = function (event) {
        var target = event.target;
        var text = target.result;
        $('#bulk-search').val(text);
    };
    reader.readAsText(input.files[0], 'UTF-8');
};

var NumericInputMask = function (target, decimalPlaces) {
    decimalPlaces = Number(decimalPlaces);

    var NUMPAD_DECIMAL_POINT = 110;
    var KEYBOARD_COMMA = 188;
    var KEYBOARD_POINT = 190;
    var handleDecimalPoint = function (event, numeric, separator) {
        var value = target.value;
        if (numeric === 0) {
            return;
        }
        if (value.indexOf(separator) === -1) {
            target.value = value + separator;
        }
    };
    $(target).keydown(function (event) {
        var separator = getDecimalSeparator();
        if (event.ctrlKey || event.altKey)
            return true;
        if (event.which < 48) {
            return true;
        } else if ((event.which >= 96 && event.which <= 105) || (event.which >= 48 && event.which <= 57)) {
            var value = target.value;
            var position = value.indexOf(separator);
            if ((position !== -1) && (position === value.length - decimalPlaces - 1)) {
                event.preventDefault();
            }
        } else {
            switch (separator) {
                case '.':
                    if (event.which === NUMPAD_DECIMAL_POINT || event.which === KEYBOARD_POINT)
                        handleDecimalPoint(event, decimalPlaces, separator);
                    break;
                case ',':
                    if (event.which === NUMPAD_DECIMAL_POINT || event.which === KEYBOARD_COMMA)
                        handleDecimalPoint(event, decimalPlaces, separator);
                    break;
                default:
                    throw new Error('I cannot determine your browser\'s decimal separator');
            }
            event.preventDefault();
        }
    });
};

var toQueryString = function (obj) {
    var params = [];
    for (var key in obj) {
        if (obj.hasOwnProperty(key)) {
            params.push(key + '=' + obj[key])
        }
    }
    return params.join('&');
};

var setValue = function (input, searchId, value, endpoint, error, ok) {
    var variables = $('#csp-vars');
    var request = {
        id: searchId,
        _xsrf: variables.attr('data-xsrf'),
        value: value
    };
    var xhr = new XMLHttpRequest();
    var groupId = variables.attr('data-group-id');
    xhr.open('POST', '/google/' + groupId + '/search/' + searchId + '/set-' + endpoint, true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState !== 4)
            return;
        if (xhr.status !== 200) {
            error();
        } else {
            const grid = serposcope.googleGroupControllerGrid;
            // Update the grid data
            grid.setFieldValue(searchId, endpoint, value);
            ok();
        }
    };
    xhr.setRequestHeader('content-type', 'application/x-www-form-urlencoded');
    xhr.send(toQueryString(request));
};

var setVolume = function (input, searchId, value, error, ok) {
    setValue(input, searchId, value, 'volume', error, ok)
};

var setTag = function (input, searchId, value, error, ok) {
    setValue(input, searchId, value, 'tag', error, ok)
};

var setCategory = function (input, searchId, value, error, ok) {
    setValue(input, searchId, value, 'category', error, ok)
};

var setCompetition = function (input, searchId, value, error, ok) {
    setValue(input, searchId, value, 'competition', error, ok)
};

var setCPC = function (input, searchId, value, error, ok) {
    setValue(input, searchId, value, 'cpc', error, ok)
};

var setKeywordVisibility = function (checkbox) {
    var id = checkbox.getAttribute('data-search-id');
    var variables = $('#csp-vars');
    var request = {
        id: id,
        visible: checkbox.checked,
        _xsrf: variables.attr('data-xsrf')
    };
    var xhr = new XMLHttpRequest();
    xhr.open('POST', "/google/" + variables.attr('data-group-id') + "/search/set-visibility", true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState !== 4)
            return;
        if (xhr.status === 200) {
            const grid = serposcope.googleGroupControllerGrid;
            // Update the grid data
            grid.setSearchVisibility(id, !checkbox.checked);
            // Update the checkbox itself (remember that we ignored the change)
            checkbox.checked = !checkbox.checked;
        } else {
            showError('Ha ocurrido un problema, no se pudo actualizar la visibilidad de la Keyword');
        }
        checkbox.disabled = false;
    };
    xhr.setRequestHeader('content-type', 'application/x-www-form-urlencoded');
    // Just before sending the query
    checkbox.disabled = true;
    // Now simply send the request
    xhr.send(toQueryString(request));
    // Now return false to stop this from going ...
    return false
};

var EditableInput = {
    reset: function (target) {
        var value = target.getAttribute('data-original-value');
        target.readOnly = true;
        target.value = value;
        // Make it normal again
        $(target).removeClass('working');
        // Remove focus
        target.blur();
    },
    setVolume: function (input, searchId, value, error, ok) {
        setVolume(input, searchId, value, error, ok);
    },
    setCompetition: function (input, searchId, value, error, ok) {
        setCompetition(input, searchId, value, error, ok);
    },
    setCPC: function (input, searchId, value, error, ok) {
        setCPC(input, searchId, Number(value.replace(getDecimalSeparator(), '.')), error, ok);
    },
    setTag: function (input, searchId, value, error, ok) {
        setTag(input, searchId, value, error, ok);
    },
    setCategory: function (input, searchId, value, error, ok) {
        setCategory(input, searchId, value, error, ok);
    },
    submit: function (target) {
        var id = target.getAttribute('data-search-id');
        var fn = target.getAttribute('data-submit-fn');
        target.readOnly = true;
        // Make it show some progress
        $(target).addClass('working');
        EditableInput[fn](target, id, target.value, function () {
            EditableInput.reset(target);
        }, function () {
            $(target).attr('data-original-value', target.value);
            $(target).removeClass('working');
            // Now "reset" it
            EditableInput.reset(target);
        });
    },
    onMouseEnter: function (event) {
    },
    onMouseLeave: function (event) {
    },
    onFocus: function (event) {
        var target = event.target;
        if (target.readOnly) {
            $(target).addClass('fake-no-focus');
        }
    },
    onBlur: function (event) {
        var target = event.target;
        // Reset it now
        EditableInput.reset(target);
        // Remove the no-focus fake class
        $(target).removeClass('fake-no-focus');
    },
    onPaste: function (event) {
        var target = event.target;
        var data = event.clipboardData || window.clipboardData;
        var text = data.getData('Text');
        var numeric = target.getAttribute('data-numeric-input');
        if (numeric != null) {
            var decimalPlaces = Number(numeric);
            var value = Number(text.replace(getDecimalSeparator(), '.'));
            if (!isNaN(value)) {
                target.value = value.toLocaleString(undefined, {
                    minimumFractionDigits: decimalPlaces,
                    maximumFractionDigits: decimalPlaces
                });
            }
            event.preventDefault();
        }
    },
    onDblClick: function (event) {
        var target = event.target;
        target.readOnly = false;
        target.onkeyup = function (keyboard) {
            switch (keyboard.keyCode) {
                case 13:
                    EditableInput.submit(target);
                    break;
                case 27:
                    EditableInput.reset(target);
                    break;
            }
        };
        $(target).removeClass('fake-no-focus');
        var numeric = target.getAttribute('data-numeric-input');
        if (numeric !== null) {

            new NumericInputMask(target, numeric);
        }
        return false;
    }
};

var showError = function (message) {
    var html = '<div class="alert alert-danger alert-dismissible" role="alert">' +
        '<button type="button" class="close" data-dismiss="alert"><span aria-hidden="true">&times;</span><span class="sr-only">Cerrar</span></button>' +
        '<div class="alert-upper">' + message + '</div>' +
        '</div>';
    var alerts = document.querySelector('.alerts');
    alerts.innerHTML = html;
};

var createEditableInputCell = function (value, searchId, submitFnName, decimalPlaces) {
    var input = document.createElement('input');
    input.type = 'text';
    if (decimalPlaces !== undefined) {
        input.setAttribute('data-numeric-input', decimalPlaces);
        input.setAttribute('class', 'inline numeric');
    } else {
        input.setAttribute('class', 'inline');
    }
    input.setAttribute('data-search-id', searchId);
    input.setAttribute('data-submit-fn', submitFnName);
    input.setAttribute('data-original-value', value);
    // Events
    input.setAttribute('ondblclick', 'EditableInput.onDblClick(event)');
    input.setAttribute('onmouseenter', 'EditableInput.onMouseEnter(event)');
    input.setAttribute('onmouseleave', 'EditableInput.onMouseLeave(event)');
    input.setAttribute('onfocus', 'EditableInput.onFocus(event)');
    input.setAttribute('onblur', 'EditableInput.onBlur(event)');
    input.setAttribute('onpaste', 'EditableInput.onPaste(event)');
    input.setAttribute('value', value);
    input.setAttribute('readonly', null);
    input.setAttribute('title', 'Doble click para editar...');
    return input.outerHTML;
};

var matchesFilter = function (filters, row, key, unsetValue) {
    if (filters[key] === undefined || row[key] === undefined)
        throw new Error('unknown filter `' + key + '\' or the row object doesn\'t have such property');
    if (filters[key] === unsetValue)
        return true;
    if (row[key] === null)
        return false;
    var filter = filters[key].toLowerCase();
    var value = row[key].toLowerCase();
    // Now just compare the values
    return value.indexOf(filter) !== -1;
};

var applyFilter = function (filter, dataView) {
    filter.keyword = $('#filter-keyword').val();
    filter.country = $('#filter-country').val();
    filter.device = $('#filter-device').val();
    filter.local = $('#filter-local').val();
    filter.datacenter = $('#filter-datacenter').val();
    filter.custom = $('#filter-custom').val();
    filter.category = $('#filter-category').val() || -1;
    filter.tag = $('#filter-tag').val() || -1;

    dataView.refresh();
};

var resetFilter = function (filter, dataView) {
    $('#filter-keyword').val('');
    $('#filter-country').val('');
    $('#filter-device').val('');
    $('#filter-local').val('');
    $('#filter-datacenter').val('');
    $('#filter-custom').val('');
    $('#filter-category').val(-1);
    $('#filter-tag').val(-1);
    applyFilter(filter, dataView);
};

var matchesAtLeastOneFilter = function (filter, row) {
    if (!matchesFilter(filter, row, 'keyword', ''))
        return false;
    if (!matchesFilter(filter, row, 'device', ''))
        return false;
    if (!matchesFilter(filter, row, 'country', ''))
        return false;
    if (!matchesFilter(filter, row, 'datacenter', ''))
        return false;
    if (!matchesFilter(filter, row, 'local', ''))
        return false;
    if (!matchesFilter(filter, row, 'tag', -1))
        return false;
    return matchesFilter(filter, row, 'category', -1);
};

var KeywordFilter = function () {
    this.keyword = '';
    this.country = '';
    this.device = '';
    this.local = '';
    this.datacenter = '';
    this.competition = '';
    this.category = -1;
    this.tag = -1;
};
