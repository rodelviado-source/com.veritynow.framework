
//global variables that can be used by ALL the functions on this page.
let is64;
let inputs;     // A list of all the inputs on the page
let tabOrder;   // A list of all the inputs with tab order, ordered by tab index
const states = ['On.png', 'Off.png', 'DownOn.png', 'DownOff.png', 'RollOn.png', 'RollOff.png'];
const states64 = ['imageOn', 'imageOff', 'imageDownOn', 'imageDownOff', 'imageRollOn', 'imageRollOff'];

function setImage(input, state) {
    if (inputs[input].getAttribute('images').charAt(state) === '1') {
        document.getElementById(inputs[input].getAttribute('id') + "_img").src = getSrc(input, state);
    }
}

function getSrc(input, state) {
    let src;
    if (is64) {
        src = inputs[input].getAttribute(states64[state]);
    } else {
        src = inputs[input].getAttribute('imageName') + states[state];
    }
    return src;
}

/**
 * Replace checkboxes and radiobuttons with their APImages
 * @param isBase64 Whether the APImages are encoded in base64
 */
function replaceChecks(isBase64) {

    is64 = isBase64;
    // Get all the input fields on the page
    inputs = [...document.getElementsByTagName('input')];
    // Create a sorted list of inputs for tab ordering
    tabOrder = [...document.querySelectorAll("[tabindex]")].filter(input => input.tabIndex !== -1)
        .sort((a, b) => a.tabIndex - b.tabIndex);

    //cycle through the input fields
    for (let i=0; i<inputs.length; i++) {
        if (!inputs[i].hasAttribute('images')) continue;

        //check if the input is a checkbox or radio button
        if (inputs[i].getAttribute('class') !== 'idr-hidden' && inputs[i].getAttribute('data-image-added') !== 'true'
            && (inputs[i].getAttribute('type') === 'checkbox' || inputs[i].getAttribute('type') === 'radio')) {

            //create a new image
            let img = document.createElement('img');

            //check if the checkbox is checked
            if (inputs[i].checked) {
                if (inputs[i].getAttribute('images').charAt(0) === '1')
                    img.src = getSrc(i, 0);
            } else {
                if (inputs[i].getAttribute('images').charAt(1) === '1')
                    img.src = getSrc(i, 1);
            }

            //set image ID
            img.id = inputs[i].getAttribute('id') + "_img";
            // Copy Tab index
            img.tabIndex = inputs[i].tabIndex;

            //set action associations
            let imageIndex = i;
            img.addEventListener("click", () => checkClick(imageIndex));
            img.addEventListener("mousedown", () => checkDown(imageIndex));
            img.addEventListener("mouseover", () => checkOver(imageIndex));
            img.addEventListener("mouseup", () => checkRelease(imageIndex));
            img.addEventListener("mouseout", () => checkRelease(imageIndex));
            img.addEventListener("focus", () => checkFocus(imageIndex))
            img.addEventListener("blur", () => checkBlur(imageIndex))

            img.style.position = "absolute";
            let style = window.getComputedStyle(inputs[i]);
            img.style.top = style.top;
            img.style.left = style.left;
            img.style.width = style.width;
            img.style.height = style.height;
            img.style.zIndex = style.zIndex;

            //place image in front of the checkbox
            inputs[i].parentNode.insertBefore(img, inputs[i]);
            inputs[i].setAttribute('data-image-added','true');
            inputs[i].setAttribute('data-image-index', i.toString());

            //hide the checkbox
            inputs[i].style.display='none';

            // Specific handling for checkbox
            if (inputs[i].type === 'checkbox') {
                img.addEventListener("keydown", event => {
                    // Need to capture keydown or it will scroll the page
                    if (event.code === "Space") {
                        event.preventDefault();
                        event.stopPropagation();
                        return false;
                    }
                });

                img.addEventListener("keyup", event => {
                    if (event.isComposing) return;
                    if (event.code === "Space") {
                        checkSpace(imageIndex);
                    }
                })
            } else if (inputs[i].type === "radio") {

                // Handle navigation
                img.addEventListener("keydown", event => {
                    if (["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.code)) {
                        event.preventDefault();
                        event.stopPropagation();
                        handleRadioArrow(event.code, i);
                        return false;
                    } else if (event.code === "Tab") {
                        event.preventDefault();
                        event.stopPropagation();
                        handleRadioTab(event.shiftKey, i);
                        return false;
                    }
                })
            }
        }
    }
}

/**
 * Handle when a radio button is navigated using the arrow keys
 * @param code {("ArrowUp"|"ArrowDown"|"ArrowLeft"|"ArrowRight")} The code for the key used to navigate
 * @param i {Number}The index of the radiobutton in the inputs array
 */
function handleRadioArrow(code, i) {
    const options = [...document.querySelectorAll(`input[data-field-name="${inputs[i].dataset.fieldName}"]`)];


    // Get the index of the currently selected checkbox
    const selected = inputs[i];
    let index = selected ? options.indexOf(selected) : 0;

    if (["ArrowLeft", "ArrowUp"].includes(code)) {
        // Get the previous index, wrapping around if necessary
        index = index === 0 ? options.length - 1 : index - 1;
    } else {
        // Get the next index, wrapping around if necessary
        index = (index + 1) % options.length;
    }

    const input = options[index];
    const imageIndex = parseInt(input.dataset.imageIndex);
    input.checked = true;
    focus(input);
    input.dispatchEvent(new Event("change"));

    deselectSiblingRadio(imageIndex);
    refreshApImage(imageIndex);
}

/**
 * Handle when a radiobutton tries to go to the next or previous form field with tab
 * @param back {Boolean} Whether to go to the previous element
 * @param i {Number} The index of the radiobutton in the inputs array
 */
function handleRadioTab(back, i) {
    let index = tabOrder.indexOf(inputs[i]);

    // A count is used to ensure that if there is only one radio button group in the list then it will not be an
    // infinite loop
    let count = 0;
    while (count++ < tabOrder.length
            && (tabOrder[index].dataset.fieldName === inputs[i].dataset.fieldName
            || tabOrder[index].readOnly || tabOrder[index].disabled)) {
        if (!back) {
            index = (index + 1) % tabOrder.length;
        } else {
            index = (index - 1);
            if (index < 0) index = tabOrder.length - 1;
        }
    }

    focus(tabOrder[index]);
}

/**
 * Focus the element at the given index of the Inputs array
 * <br>
 * This ensures that the AP Image is selected if available, and the input is selected otherwise
 * @param i {Number | Element}The index of the element in the inputs array or the input itself
 */
function focus(i) {
    const input = typeof i === "number" ? inputs[i] : i;
    let element;
    if (input.dataset.imageAdded === "true") element = document.getElementById(input.id + "_img");
    else element = input;

    element.focus({focusVisible: true});
}

/**
 * A utility to deselect all the siblings of the input at the given index
 * @param i {Number} The index of the input of who's siblings are to be disabled
 */
function deselectSiblingRadio(i) {
    if (inputs[i].getAttribute('name') !== null) {
        for (let index = 0; index < inputs.length; index++) {
            if (index !== i && inputs[index].getAttribute('name') === inputs[i].getAttribute('name')) {
                inputs[index].checked = false;
                setImage(index, 1);
            }
        }
    }
}

/**
 * Refresh the AP Image of the given input based on its value
 *
 * Intended to be used externally to update the ap image after a change
 * @param i {Number} The index of the checkbox/radiobutton
 */
function refreshApImage(i)  {
    if (!inputs[i].hasAttribute('images')) return;
    if (inputs[i].checked) {
        setImage(i, 0);
    } else {
        setImage(i, 1);
    }
}

/**
 * Handle clicking on a checkbox/radiobutton
 * <br>
 * This is the one of the mouse operations that actually changes the checkbox/radiobutton status
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkClick(i) {
    if (!inputs[i].hasAttribute('images')) return;

    if (inputs[i].checked) {
        if (inputs[i].getAttribute('type') === 'radio' && inputs[i].dataset.flagNotoggletooff === "true") {
            inputs[i].dispatchEvent(new Event('click'));
            return;
        } else {
            inputs[i].checked = false;
            setImage(i, 1);
        }
    } else {
        inputs[i].checked = true;

        setImage(i, 0);

        deselectSiblingRadio(i);
    }

    /*
     * Both checkboxes and radio buttons fire the change and input events
     * https://html.spec.whatwg.org/multipage/input.html#concept-input-apply
     */
    inputs[i].dispatchEvent(new Event('change'));
    inputs[i].dispatchEvent(new Event('input'));

    inputs[i].dispatchEvent(new Event('click'));
}

/**
 * Handle when the space bar is pressed whilst a checkbox is targeted
 * <br>
 * This is only for checkboxes, so there's no radiobutton specific logic included
 * <br>
 * Changes the checkbox status and set the replacement image
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkSpace(i) {
    if (!inputs[i].hasAttribute('images')) return;
    if (inputs[i].checked) {
        inputs[i].checked = false;
        setImage(i, 1);
    } else {
        inputs[i].checked = true;
        setImage(i, 0);
    }

    /*
     * Both checkboxes and radio buttons fire the change and input events
     * https://html.spec.whatwg.org/multipage/input.html#concept-input-apply
     */
    inputs[i].dispatchEvent(new Event('change'));
    inputs[i].dispatchEvent(new Event('input'));

    inputs[i].dispatchEvent(new Event('keyup'));
}

/**
 * Handle when a checkbox/radiobutton is released (mouseup/mouseout)
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkRelease(i) {
    if (!inputs[i].hasAttribute('images')) return;
    if (inputs[i].checked) {
        setImage(i, 0);
    } else {
        setImage(i, 1);
    }
    inputs[i].dispatchEvent(new Event('mouseup'));
}

/**
 * Handle when a checkbox/radiobutton is pressed (mousedown)
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkDown(i) {
    if (!inputs[i].hasAttribute('images')) return;
    if (inputs[i].checked) {
        setImage(i, 2);
    } else {
        setImage(i, 3);
    }
    inputs[i].dispatchEvent(new Event('mousedown'));
}

/**
 * Handle when a mouse hovers over a checkbox/radiobutton
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkOver(i) {
    if (!inputs[i].hasAttribute('images')) return;
    if (inputs[i].checked) {
        setImage(i, 4);
    } else {
        setImage(i, 5);
    }
    inputs[i].dispatchEvent(new Event('mouseover'));
}

/**
 * Handle when the AP image is focused
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkFocus(i) {
    if (!inputs[i].hasAttribute('images')) return;

    inputs[i].dispatchEvent(new Event('focus'));
}

/**
 * Handle when the AP image loses focus
 * @param i {Number} The index of the checkbox/radiobutton
 */
function checkBlur(i) {
    if (!inputs[i].hasAttribute('images')) return;

    inputs[i].dispatchEvent(new Event('blur'));
}


/**
 * Handle when the AP image loses focus
 * @param id1 {string} id of element where to get the value to append (e.value)
 * @param id2 {string} id of the element where to append
 * @param  r  {boolean} if true reset id2's value if succesful
 * e2 value will be reset
 */
function appendToInput(id1, id2, reset) {

	let eTarget = document.getElementById(id1);
	let eSource = document.getElementById(id2);
	
	if (eTarget != null && eSource != null) {
		
		
		let s = eSource.value;
		let t = eTarget.value;
		s = (s!= null) ? s.trim() : "";
		t = (t!= null) ? t.trim() : "";
						
		t = (s.length <= 0) ? "" : t += ", " + s;
		
		
		let ns = new Set(t.split(/[, ]+/));
		ns.delete("");
		
		t = [...ns].join(", ");
		
		eTarget.value = t;
		if (reset)
			eSource.value = "";
	}	
}
	
   


