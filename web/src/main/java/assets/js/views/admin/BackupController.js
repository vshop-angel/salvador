var getCurrentSelection = function (id) {
    var array = [];
    var form = document.getElementById(id);
    var inputs = form.querySelectorAll('input[type="checkbox"]');
    for (var index = 0; index < inputs.length; ++index) {
        var input = inputs[index];
        if (input.checked) {
            array.push(input.value);
        }
    }
    return array;
};

var populateForm = function (form, selection) {
    var old = form.querySelectorAll('input[type="hidden"]');
    for (var i = 0; i < old.length; ++i) {
        form.removeChild(old[i]);
    }
    for (var j = 0; j < selection.length; ++j) {
        var input = document.createElement('input');
        input.name = 'names[]';
        input.value = selection[j];
        input.type = 'hidden';
        form.appendChild(input);
    }
};

var selectionCannotBeEmptyError = function () {
    alert("Your selection is empty, why did you click the button?");
    return false;
};

var tooManyItemsSelectedError = function () {
    alert("Only one item can be selected");
    return false;
};

var confirmWipe = function (event, id) {
    try {
        var selection = getCurrentSelection(id);
        if (selection.length === 0) {
            return selectionCannotBeEmptyError();
        }
        // Populate the form with inputs
        populateForm(event.target, selection);
        // Ask the user before they do it
        return confirm("Are you sure?\nAll the information will be lost");
    } catch (ex) {
        console.log(ex);
        return false;
    }
};

var confirmRestore = function (event, id) {
    try {
        var selection = getCurrentSelection(id);
        // Populate the form with inputs
        populateForm(event.target, selection);
        if (selection.length === 0) {
            return selectionCannotBeEmptyError();
        } else if (selection.length !== 1) {
            return tooManyItemsSelectedError();
        } else {
            // Ask the user before they do it
            return confirm("You will restore the selected backup, are you sure?");
        }
    } catch (ex) {
        console.log(ex);
        return false;
    }
};

var reload = function () {
    window.location.reload();
};

var showSuccess = function (cell) {
    var i = document.createElement('i');
    i.setAttribute('class', 'fa fa-check green');
    cell.innerHTML = '';
    cell.appendChild(i);
};

var checkBackupStatus = function (name, status) {
    if (status === 'Finished') {
        return;
    }
    var xhr = new XMLHttpRequest();
    xhr.open('GET', '/admin/backup/status?name=' + name, true);
    xhr.onreadystatechange = function () {
        if (xhr.readyState !== 4)
            return;
        if (xhr.status === 200) {
            var json = JSON.parse(xhr.responseText);
            var row = document.getElementById(name);
            var checkCell = row.cells[0];
            var sizeCell = row.cells[3];
            // Update the size cell
            sizeCell.innerHTML = json.size;
            if (json.status === 'Finished') {
                showSuccess(checkCell);
                setTimeout(reload, 1500);
            } else {
                setTimeout(function () {
                    checkBackupStatus(name, json.status);
                }, 1000);
            }
        }
    };
    xhr.send();
};
