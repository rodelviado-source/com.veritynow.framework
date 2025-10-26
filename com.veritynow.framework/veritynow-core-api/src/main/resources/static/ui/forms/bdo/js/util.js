(function() {
"use strict";

/**
 * Shorthand helper function to getElementById
 * @param id
 * @returns {Element}
 */
var d = function (id) {
    return document.getElementById(id);
};

var ClassHelper = (function() {
    return {
        addClass: function(ele, name) {
            var classes = ele.className.length !== 0 ? ele.className.split(" ") : [];
            var index = classes.indexOf(name);
            if (index === -1) {
                classes.push(name);
                ele.className = classes.join(" ");
            }
        },

        removeClass: function(ele, name) {
            var classes = ele.className.length !== 0 ? ele.className.split(" ") : [];
            var index = classes.indexOf(name);
            if (index !== -1) {
                classes.splice(index, 1);
            }
            ele.className = classes.join(" ");
        }
    };
})();

var Button = {};

FormViewer.on('ready', function(data) {
    // Grab buttons
    Button.zoomIn = d('btnZoomIn');
    Button.zoomOut = d('btnZoomOut');

    if (Button.zoomIn) {
        Button.zoomIn.onclick = function(e) { FormViewer.zoomIn(); e.preventDefault(); };
    }
    if (Button.zoomOut) {
        Button.zoomOut.onclick = function(e) { FormViewer.zoomOut(); e.preventDefault(); };
    }

    document.title = data.title ? data.title : data.fileName;
    var pageLabels = data.pageLabels;
    var btnPage = d('btnPage');
    if (btnPage != null) {
        btnPage.innerHTML = pageLabels.length ? pageLabels[data.page - 1] : data.page;
        btnPage.title = data.page + " of " + data.pagecount;

        FormViewer.on('pagechange', function(data) {
            d('btnPage').innerHTML = pageLabels.length ? pageLabels[data.page - 1] : data.page;
            d('btnPage').title = data.page + " of " + data.pagecount;
        });
    }

    if (idrform.app) {
        idrform.app.execFunc = idrform.app.execMenuItem;
        idrform.app.execMenuItem = function (str) {
            switch (str.toUpperCase()) {
                case "FIRSTPAGE":
                    idrform.app.activeDocs[0].pageNum = 0;
                    FormViewer.goToPage(1);
                    break;
                case "LASTPAGE":
                    idrform.app.activeDocs[0].pageNum = FormViewer.config.pagecount - 1;
                    FormViewer.goToPage(FormViewer.config.pagecount);
                    break;
                case "NEXTPAGE":
                    idrform.app.activeDocs[0].pageNum++;
                    FormViewer.next();
                    break;
                case "PREVPAGE":
                    idrform.app.activeDocs[0].pageNum--;
                    FormViewer.prev();
                    break;
                default:
                    idrform.app.execFunc(str);
                    break;
            }
        }
    }

    document.addEventListener('keydown', function (e) {
        if (e.target != null) {
            switch (e.target.constructor) {
                case HTMLInputElement:
                case HTMLTextAreaElement:
                case HTMLVideoElement:
                case HTMLAudioElement:
                case HTMLSelectElement:
                    return;
                default:
                    break;
            }
        }
        switch (e.keyCode) {
            case 33: // Page Up
                FormViewer.prev();
                e.preventDefault();
                break;
            case 34: // Page Down
                FormViewer.next();
                e.preventDefault();
                break;
            case 37: // Left Arrow
                data.isR2L ? FormViewer.next() : FormViewer.prev();
                e.preventDefault();
                break;
            case 39: // Right Arrow
                data.isR2L ? FormViewer.prev() : FormViewer.next();
                e.preventDefault();
                break;
            case 36: // Home
                FormViewer.goToPage(1);
                e.preventDefault();
                break;
            case 35: // End
                FormViewer.goToPage(data.pagecount);
                e.preventDefault();
                break;
        }
    });
});

window.addEventListener("beforeprint", function(event) {
    FormViewer.setZoom(FormViewer.ZOOM_AUTO);
});

})();
