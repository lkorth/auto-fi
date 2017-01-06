// check all checkboxes on the page
var inputs = document.querySelectorAll("input[type='checkbox']");
for (var i = 0; i < inputs.length; i++) {
    inputs[i].checked = true;
}

var links = document.getElementsByTagName("a");
var buttons = document.getElementsByTagName("button");
var inputButtons = document.querySelectorAll("input[type='button']");
var submitButtons = document.querySelectorAll("input[type='submit']");
var imageButtons = document.querySelectorAll("input[type='image']");

var selectedElements = [];
selectedElements = selectedElements.concat(findActionElements(submitButtons));
selectedElements = selectedElements.concat(findActionElements(inputButtons));
selectedElements = selectedElements.concat(findActionElements(buttons));
selectedElements = selectedElements.concat(findActionElements(links));
selectedElements = selectedElements.concat(findActionElements(imageButtons));

AutoFi.log("Matched " + selectedElements.length + " elements");

if (selectedElements.length > 0) {
    AutoFi.log("Clicking " + selectedElements[0]);
    selectedElements[0].click();
}

function findActionElements(elements) {
    var collectedElements = [];

    collectedElements = collectedElements.concat(findElementsByText(elements, /\baccept\b/i));
    collectedElements = collectedElements.concat(findElementsByText(elements, /\bconnect\b/i));
    collectedElements = collectedElements.concat(findElementsByText(elements, /\bcontinue\b/i));
    collectedElements = collectedElements.concat(findElementsByText(elements, /\blogin\b/i));
    collectedElements = collectedElements.concat(findElementsByText(elements, /\blogon\b/i));
    collectedElements = collectedElements.concat(findElementsByText(elements, /\bok\b/i));
    collectedElements = collectedElements.concat(findElementsByText(elements, /\bget connected\b/i));

    return collectedElements;
}

function findElementsByText(elements, text) {
    var collectedElements = [];
    for (var i = 0; i < elements.length; i++) {
        if (elements[i].title && elements[i].title.match(text)) {
            collectedElements.push(elements[i]);
        } else if (elements[i].value && elements[i].value.match(text)) {
            collectedElements.push(elements[i])
        } else if (elements[i].innerHTML && elements[i].innerHTML.match(text)) {
            collectedElements.push(elements[i]);
        } else if (elements[i].alt && elements[i].alt.match(text)) {
            collectedElements.push(elements[i]);
        }
    }

    return collectedElements;
}
