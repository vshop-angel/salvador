const createElement = function (type, options) {
    var element = document.createElement(type);
    for (var key in options) {
        if (!options.hasOwnProperty(key))
            continue;
        switch (key) {
            case 'className':
                element.setAttribute('class', options[key]);
                break;
            case 'text':
                element.appendChild(document.createTextNode(options.text));
                break;
            case 'children':
                for (const child of options.children)
                    element.appendChild(child);
                break;
            case 'eventHandlers':
                for (const event of Object.getOwnPropertyNames(options.eventHandlers)) {
                    const handler = options.eventHandlers[event];
                    const internalHandler = async () => {
                        // While handling the event, disable the event
                        element.removeEventListener(event, internalHandler);
                        // Wait for the handler to finish
                        await handler();
                        // Now reinstall the handler
                        element.addEventListener(event, internalHandler);
                    };
                    element.addEventListener(event, internalHandler);
                }
                break;
            default:
                if (key.startsWith('data')) {
                    const normalized = key
                        .split(/(?=[A-Z])/)
                        .join('-')
                        .toLowerCase();
                    element.setAttribute(normalized, options[key]);
                } else {
                    element.setAttribute(key, options[key]);
                }
        }
    }
    return element;
};

var toArray = function (nodes) {
    var array = [];
    for (var index = 0; index < nodes.length; ++index) {
        array.push(nodes[index]);
    }
    return array;
};

var transposeTable = function (table) {
    var array = [];
    var parent = table.parentNode;
    // Ignore this specific table
    if ($(parent).hasClass('hb-body'))
        return;
    var headers = table.querySelectorAll('tr th');
    var rows = table.querySelectorAll('tbody tr');
    for (var row = 0; row < rows.length; ++row) {
        var items = [];
        var columns = rows[row].querySelectorAll('td');
        for (var col = 0; col < columns.length; ++col) {
            var column = columns[col];
            var header = headers[col];
            if (!header) {
                header = {childNodes: []};
            } else {
                header = header.cloneNode(true);
            }
            var label = createElement('div', {
                className: 'is-table-header',
                children: toArray(header.childNodes)
            });
            var value = createElement('div', {
                className: 'is-table-value',
                children: toArray(column.childNodes)
            });
            var item = createElement('div', {
                className: 'is-table-entry',
                children: [label, value]
            });
            if ($(rows[row]).hasClass('event-description')) {
                var last = array[array.length - 1];
                // Very peculiar case
                label.innerHTML = 'Descripción';
                last.appendChild(item);
            } else {
                items.push(item);
            }
        }
        array.push(createElement('div', {
            className: 'is-table-row',
            children: items
        }));
    }
    parent.appendChild(createElement('div', {
        className: 'responsive-table',
        children: array
    }), table);
};

var setActive = function (toSet, current) {
    var li = toSet.parent();
    // Set the one we want as active
    li.addClass('active-path');
};

window.addEventListener('load', function () {
    var location = window.location;
    var base = location.pathname
        .split('/')
        .slice(0, 2)
        .join('/');
    var link = document.querySelector('a[href="' + base + '"]');
    if (link) {
        setActive($(link));
    }
    if (window.innerWidth > 778)
        return;
    var tables = document.querySelectorAll('table');
    for (var index = 0; index < tables.length; ++index) {
        transposeTable(tables[index]);
    }
});
