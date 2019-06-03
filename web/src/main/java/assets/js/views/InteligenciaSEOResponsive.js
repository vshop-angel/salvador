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
                var children = options.children;
                for (var i = 0; i < children.length; ++i) {
                    element.appendChild(children[i]);
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
    if ($(table).hasClass('hidden-xs'))
        return;
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

var updateMainActiveTab = function () {
    var location = window.location;
    var base = location.pathname
        .split('/')
        .slice(0, 2)
        .join('/');
    var link = document.querySelector('a[href="' + base + '"]');
    if (link) {
        setActive($(link));
    }
};

var adjustSecondaryTabs = function () {
    var items = document.querySelectorAll('ul.nav.nav-tabs');
    for (var i = 0; i < items.length; ++i) {
        (function (tabBar) {
            var parent = tabBar.parentNode;
            if (tabBar.offsetWidth < tabBar.scrollWidth) {
                var prevArrow = createElement('button', {
                    className: 'tab-scroller prev',
                    children: [
                        createElement('i', {
                            className: 'fa fa-angle-left'
                        })
                    ]
                });
                var nextArrow = createElement('button', {
                    className: 'tab-scroller next',
                    children: [
                        createElement('i', {
                            className: 'fa fa-angle-right'
                        })
                    ]
                });
                var oldTabBar = tabBar;
                // Make a clone
                tabBar = tabBar.cloneNode(true);
                var goTo = function (item) {
                    var anchor = item.firstElementChild;
                    var location = window.location;
                    // Set the href
                    location.href = anchor.href;
                    // Find activa tab pane
                    $('.tab-pane.active').removeClass('active');
                    // Activate the actual tab
                    $(location.hash).addClass('active');
                };
                var setActiveTab = function (item) {
                    var last = tabBar.querySelector('.active');
                    $(last).removeClass('active');
                    $(item).addClass('active');
                    // No actually navigate
                    goTo(item);
                };
                // Reinstall handlers
                var listItems = tabBar.querySelectorAll('li');
                for (var m = 0; m < listItems.length; ++m) {
                    (function (item) {
                        item.onclick = function () {
                            // And always scroll to the item
                            scrollTo(item);
                        };
                    })(listItems[m]);
                }
                var scrollTo = function (item) {
                    var first = tabBar.firstElementChild;
                    // Move the scroll to the optimal position if possible
                    // or the maximum valid
                    tabBar.scrollLeft = item.offsetLeft - first.offsetLeft;
                    // Activate selected tab
                    setActiveTab(item);
                    // Now toggle buttons visibility
                    prevArrow.disabled = tabBar.scrollLeft <= 0;
                    nextArrow.disabled = tabBar.scrollLeft >= tabBar.scrollWidth - tabBar.offsetWidth;
                };
                prevArrow.addEventListener('click', function () {
                    var children = tabBar.children;
                    for (var k = 0; k < children.length; ++k) {
                        var child = children[k];
                        if (child.offsetLeft < tabBar.scrollLeft) {
                            scrollTo(child);
                            return;
                        }
                    }
                });
                nextArrow.addEventListener('click', function () {
                    var children = tabBar.children;
                    for (var k = 0; k < children.length; ++k) {
                        var child = children[k];
                        if (child.offsetLeft + child.offsetWidth === tabBar.scrollWidth) {
                            scrollTo(child);
                            return;
                        }
                    }
                });
                parent.replaceChild(createElement('div', {
                    className: 'tab-container',
                    children: [prevArrow, tabBar, nextArrow]
                }), oldTabBar);
                var currentlyActive = tabBar.querySelector('.active');
                if (currentlyActive) {
                    scrollTo(currentlyActive);
                }
            }
        })(items[i]);
    }
};

window.addEventListener('load', function () {
    updateMainActiveTab();
    if (window.innerWidth > 778)
        return;
    var tables = document.querySelectorAll('table');
    for (var index = 0; index < tables.length; ++index)
        transposeTable(tables[index]);
    adjustSecondaryTabs();
});
