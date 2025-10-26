/* FormViewer - v1.2.0 | Copyright 2022 IDRsolutions */
!function(){"use strict";var e={LAYOUT_CONTINUOUS:"continuous",SELECT_SELECT:"select",SELECT_PAN:"pan",ZOOM_SPECIFIC:"specific",ZOOM_ACTUALSIZE:"actualsize",ZOOM_FITWIDTH:"fitwidth",ZOOM_FITHEIGHT:"fitheight",ZOOM_FITPAGE:"fitpage",ZOOM_AUTO:"auto"},t=1,o=0,n,i,r,a,s=!0,u,c=[],l,f,m=[],d=10,p={},g=!1,v,h="",O=!1;e.setup=function(d){d||(d=FormViewer.config),g=!0,a=/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent),u=d.bounds,o=d.pagecount,d.url&&(h=d.url),O=!!d.isR2L,i=H("formviewer"),O&&R.addClass(i,"isR2L"),L.setup();var p=document.createElement("style");p.setAttribute("type","text/css"),document.head.appendChild(p),f=p.sheet,(t<1||t>o)&&(t=1);for(var T=0;T<o;T++)if(u[T][0]!=u[0][0]||u[T][1]!=u[0][1]){s=!1;break}switch(v){case FormViewer.LAYOUT_CONTINUOUS:break;default:v=FormViewer.LAYOUT_CONTINUOUS}var E=[e.LAYOUT_CONTINUOUS];for(v===FormViewer.LAYOUT_CONTINUOUS&&(r=b),window.addEventListener("resize",(function(){L.updateZoom()})),l=H("overlay"),S.setup(),null==(n=H("contentContainer"))&&(n=H("mainXFAForm")),n.style.transform="translateZ(0)",n.style.padding="5px",T=1;T<=o;T++){var A=H("page"+T);A.setAttribute("style","width: "+u[T-1][0]+"px; height: "+u[T-1][1]+"px;"),m[T]=A,c[T]=F(A,T)}r.setup(t),R.addClass(i,"layout-"+r.toString()),L.updateZoomToDefault(),r.goToPage(t);var C={selectMode:S.currentSelectMode,isMobile:a,layout:r.toString(),availableLayouts:E,isFirstPage:1===t,isLastPage:r.isLastPage(t)};for(var I in d)d.hasOwnProperty(I)&&(C[I]=d[I]);C.page=t,e.fire("ready",C)};var F=function(t,o){var n={isVisible:function(){return!0},isLoaded:function(){return!0},hide:function(){},unload:function(){e.fire("pageunload",{page:o})},load:function(){e.fire("pageload",{page:o})}};return n},T=function(n){t!=n&&(t=n,e.fire("pagechange",{page:t,pagecount:o,isFirstPage:1===t,isLastPage:r.isLastPage(n)}))},b=function(){var n={},r=0,a=0,s=[];n.setup=function(){i.addEventListener("scroll",c);for(var e=0;e<o;e++)u[e][0]>r&&(r=u[e][0]),u[e][1]>a&&(a=u[e][1])},n.unload=function(){i.removeEventListener("scroll",c)};var c=function(){l()},l=function(){var e,t;if(m[1].getBoundingClientRect().top>0)T(1);else for(e=1;e<=o;e++){var n=m[e].getBoundingClientRect();t=n.top;var i=n.bottom-n.top;if(t<=.25*i&&t>.5*-i){T(e);break}}f()},f=function(){s=[t];var e,n,r=i.clientHeight,a=function(e){return(n=m[e].getBoundingClientRect()).bottom>0&&n.top<r};for(e=t-1;e>=1&&a(e);e--)s.push(e);for(e=t+1;e<=o&&a(e);e++)s.push(e)};return n.goToPage=function(e,t){var o=0;if(t){var n=t.split(" ");switch(n[0]){case"XYZ":o=Number(n[2]);break;case"FitH":o=Number(n[1]);break;case"FitR":o=Number(n[4]);break;case"FitBH":o=Number(n[1]);break}(isNaN(o)||o<0||o>u[e-1][1])&&(o=0),0!==o&&(o=u[e-1][1]-o)}var r=L.getZoom();i.scrollTop=m[e].offsetTop-5+o*r,T(e),f()},n.getVisiblePages=function(){return s},n.updateLayout=function(){},n.isLastPage=function(e){return e===o},n.getZoomBounds=function(){return{width:r,height:a}},n.getAutoZoom=function(e){return n.getZoomBounds().width>i.clientWidth-d?e:1},n.next=function(){e.goToPage(t+1)},n.prev=function(){e.goToPage(t-1)},n.toString=function(){return FormViewer.LAYOUT_CONTINUOUS},n}(),E=function(e){try{e.getSelection?e.getSelection().empty?e.getSelection().empty():e.getSelection().removeAllRanges&&e.getSelection().removeAllRanges():e.document.selection&&e.document.selection.empty()}catch(e){}},A=function(e){try{E(e)}catch(e){}},S=function(){var t={},o,n,r=!1,a;t.setup=function(){switch(a){case FormViewer.SELECT_PAN:case FormViewer.SELECT_SELECT:break;default:a=FormViewer.SELECT_SELECT}this.currentSelectMode=a,this.currentSelectMode==e.SELECT_SELECT?t.enableTextSelection():t.enablePanning()},t.enableTextSelection=function(){this.currentSelectMode=e.SELECT_SELECT,R.removeClass(l,"panning"),l.removeEventListener("mousedown",s),document.removeEventListener("mouseup",u),l.removeEventListener("mousemove",c)};var s=function(e){return e=e||window.event,R.addClass(l,"mousedown"),o=e.clientX,n=e.clientY,r=!0,!1},u=function(){R.removeClass(l,"mousedown"),r=!1},c=function(e){if(r)return e=e||window.event,i.scrollLeft=i.scrollLeft+o-e.clientX,i.scrollTop=i.scrollTop+n-e.clientY,o=e.clientX,n=e.clientY,!1};return t.enablePanning=function(){this.currentSelectMode=e.SELECT_PAN,E(window),R.addClass(l,"panning"),l.addEventListener("mousedown",s),document.addEventListener("mouseup",u),l.addEventListener("mousemove",c)},t.setDefaultMode=function(e){a=e},t}();e.setSelectMode=function(t){if(g){if(a)return;t==e.SELECT_SELECT?S.enableTextSelection():S.enablePanning(),e.fire("selectchange",{type:t})}else S.setDefaultMode(t)};var L=(C=e.ZOOM_AUTO,P=[.25,.5,.75,1,1.25,1.5,2,2.5,3,3.5,4],w=[e.ZOOM_AUTO,e.ZOOM_FITPAGE,e.ZOOM_FITHEIGHT,e.ZOOM_FITWIDTH,e.ZOOM_ACTUALSIZE],y=0,M=1,x=function(e,t,o,n,i){var r;return"-webkit-transform: "+(r=i?"translate3d("+t+"px, "+o+"px, 0) scale("+n+")":"translateX("+t+"px) translateY("+o+"px) scale("+n+")")+";\n-ms-transform: "+r+";\ntransform: "+r+";"},U=function(t){var n=!1,a=!1;(M=D(t))>=4?(M=4,a=!0):M<=.25&&(M=.25,n=!0);var s=i.scrollTop/i.scrollHeight;r.updateLayout();for(var l=r.getVisiblePages(),f=1;f<=o;f++)-1===l.indexOf(f)&&c[f].hide();I&&Z.deleteRule(I);var d=x(null,0,0,M,!1);I=Z.insertRule(".page-inner { \n"+d+"\n}",Z.cssRules.length);for(var p=0;p<o;p++)m[p+1].style.width=Math.floor(u[p][0]*M)+"px",m[p+1].style.height=Math.floor(u[p][1]*M)+"px";i.scrollTop=i.scrollHeight*s,++y%2==1&&U(),e.fire("zoomchange",{zoomType:C,zoomValue:M,isMinZoom:n,isMaxZoom:a})},V=function(){for(var e=M,t=P[P.length-1],o=0,n;o<P.length;o++)if(P[o]>e){t=P[o];break}for(o=0;o<w.length;o++){var i=D(w[o]);if(i>e&&i<=t){if(n&&i===t)continue;n=w[o],t=i}}return n||t},k=function(){for(var e=M,t=P[0],o=P.length-1,n;o>=0;o--)if(P[o]<e){t=P[o];break}for(o=0;o<w.length;o++){var i=D(w[o]);if(i<e&&i>=t){if(n&&i===t)continue;n=w[o],t=i}}return n||t},D=function(t){var o=r.getZoomBounds(),n=(i.clientWidth-d)/o.width,a=(i.clientHeight-d)/o.height,s=parseFloat(t);switch(isNaN(s)||(M=s,t=e.ZOOM_SPECIFIC),t||(t=C),t){case e.ZOOM_AUTO:M=r.getAutoZoom(n,a);break;case e.ZOOM_FITWIDTH:M=n;break;case e.ZOOM_FITHEIGHT:M=a;break;case e.ZOOM_FITPAGE:M=Math.min(n,a);break;case e.ZOOM_ACTUALSIZE:M=1;break}return C=t,M},{setup:function(){var e=document.createElement("style");e.setAttribute("type","text/css"),document.head.appendChild(e),Z=e.sheet},updateZoom:U,updateZoomToDefault:function(){U(N)},zoomIn:function(){U(V())},zoomOut:function(){U(k())},getZoom:function(){return M},setDefault:function(e){N=e}}),C,I,P,w,y,Z,M,N,_,x,U,V,k,D;e.zoomIn=function(){L.zoomIn()},e.zoomOut=function(){L.zoomOut()},e.setZoom=function(e){g?L.updateZoom(e):L.setDefault(e)},e.goToPage=function(e,n){g?e>=1&&e<=o&&r.goToPage(Number(e),n):t=e},e.next=function(){r.next()},e.prev=function(){r.prev()},e.setLayout=function(o){g?(r.unload(),R.removeClass(i,"layout-"+r.toString()),(r=b).setup(t),R.addClass(i,"layout-"+r.toString()),L.updateZoom(FormViewer.ZOOM_AUTO),r.goToPage(t),e.fire("layoutchange",{layout:o})):v=o},e.updateLayout=function(){L.updateZoom()};var H=function(e){return document.getElementById(e)};e.on=function(e,t){p[e]||(p[e]=[]),-1===p[e].indexOf(t)&&p[e].push(t)},e.off=function(e,t){if(p[e]){var o=p[e].indexOf(t);-1!==o&&p[e].splice(o,1)}},e.fire=function(e,t){p[e]&&p[e].forEach((function(e){e(t)}))};var R={addClass:function(e,t){var o=0!==e.className.length?e.className.split(" "):[],n;-1===o.indexOf(t)&&(o.push(t),e.className=o.join(" "))},removeClass:function(){for(var e=arguments[0],t=0!==e.className.length?e.className.split(" "):[],o=1;o<arguments.length;o++){var n=t.indexOf(arguments[o]);-1!==n&&t.splice(n,1)}e.className=t.join(" ")}};e.handleSubmitButton=function(e,t,{exclude:o=!1,IncludeNoValueFields:n=!1,ExportFormat:i=!1,GetMethod:r=!1,SubmitCoordinates:a=!1,XFDF:s=!1,IncludeAppendSaves:u=!1,IncludeAnnotations:c=!1,SubmitPDF:l=!1,CanonicalFormat:f=!1,ExclNonUserAnnots:m=!1,ExclFKey:d=!1,EmbedForm:p=!1}={}){if(r)return fetch(e,{method:"GET"}).then((e=>{e.ok?alert("Successfully sent request"):alert(`Error while sending request: ${e.status}`)})).catch((e=>alert(`Failed to send request to: ${cURL}\n${e}`))),void 0;let g;if(i)g="HTML";else{if(l)return idrform.doc._submitFormAsPDF(e),void 0;g=s?"XFDF":"FDF"}o&&(t=idrform.doc._getFieldsHTML(["input","textarea","select"]).map((e=>e.dataset.fieldName)).filter((e=>!t||!t.includes(e)))),idrform.doc.submitForm({cURL:e,cSubmitAs:g,aFields:t,bEmpty:n,bCanonical:f,bAnnotations:c,bExclNonUserAnnots:m,bExclFKey:d,bEmbedForm:p,bIncrChanges:u})},e.handleFormSubmission=function(e,t){FormVuAPI&&(e||(e=window.prompt("Please enter the URL to submit to"))?z(e,t):console.log("No URL provided for FormSubmission"))};var z=function(e,t){if(FormVuAPI){var o={url:e,success:function(){alert("Form submitted successfully")},failure:function(){alert("Form failed to submit, please try again")}},n="string"==typeof t?t.toLowerCase():"";if(e.startsWith("mailto:"))return o.format=n,FormVuAPI.submitFormAsMail(o),void 0;switch(n){case"json":"function"==typeof FormVuAPI.submitFormAsJSON&&FormVuAPI.submitFormAsJSON(o);break;case"pdf":"function"==typeof FormVuAPI.submitFormAsPDF&&FormVuAPI.submitFormAsPDF(o);break;case"formdata":default:"function"==typeof FormVuAPI.submitFormAsFormData&&FormVuAPI.submitFormAsFormData(o);break}}};"function"==typeof define&&define.amd?define(["formviewer"],[],(function(){return e})):"object"==typeof module&&module.exports?module.exports=e:window.FormViewer=e}();


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