var idrform = new IDRFORM
	, idrscript = {};

function IDRFORM() {
	class Event {
		#e; #t;	changeEx; commitKey; fieldFull; keyDown; modifier;	name;
		rc = !0; richChange; richChangeEx; richValue; selEnd; selStart;
		targetName;	type; willCommit;
		
		constructor(e, t) {
			this.#e = e, this.#t = t, this.changeEx = null, this.commitKey = null, this.fieldFull = null, this.keyDown = null, this.modifier = null, this.name = "", this.richChange = null, this.richChangeEx = null, this.richValue = null, this.selEnd = null, this.selStart = null, this.targetName = "", this.type = "Field", this.willCommit = null
		}
		get shift() {
			return this.#e.shiftKey
		}
		get source() {
			return new Field(this.#e.target)
		}
		get target() {
			return new Field(this.#e.target)
		}
		get value() {
			return this.#t ? this.#t.value : this.#e.target.value
		}
		set value(e) {
			if (this.#t) return this.#t.value = e, void 0;
			this.#e.target.value = e
		}
		get change() {
			return this.#e.target.value
		}
		set change(e) {
			this.#e.target.value = e
		}
	}
	const doc = new Doc
		, app = new App;
	let event;
	const AVAIL_CALCULATES = {}
		, AVAIL_VALIDATES = {};
	this.app = app, this.doc = doc, window.getField = function(e) {
		return doc.getField(e)
	};
	const AVAIL_SCRIPTS = {
		A: "click", K: "input", C: "", V: "", F: "", Fo: "focus"
		, Bl: "blur", D: "mousedown", U: "mouseup", E: "mouseenter"	, X: "mouseleave"
	};
	this._radioUnisonSiblings = {}, this._checkboxGroups = {}, this.init = function() {
		const e = document.getElementById("FDFXFA_FormType");
		e && (app.isAcroForm = "FDF" === e.textContent || "AcroForm" === e.textContent);
		const t = document.getElementById("FDFXFA_Processing");
		if (t && (t.style.display = "none"), idrscript.documentscript) try {
			window.eval(atob(idrscript.documentscript))
		} catch (e) {
			console.log(e)
		}
		Object.keys(idrscript)
			.filter((e => e.startsWith("page")))
			.forEach((e => idrform.exec(idrscript[e])));
		const o = document.querySelectorAll("input, select, textarea");
		for (const e of o) {
			const t = undefined;
			if (!e.dataset.fieldName) continue;
			const o = e.type
				, n = e.id
				, i = ["button", "radio", "checkbox"].includes(o);
			for (const [t, o] of Object.entries(AVAIL_SCRIPTS)) {
				const a = n + "_" + t;
				if (a in idrscript && (i || ("F" === t ? e.addEventListener("blur", (e => {
					idrform.exec(idrscript[a], e)
				})) : "C" === t ? AVAIL_CALCULATES[n] = atob(idrscript[a]) : "V" === t && (AVAIL_VALIDATES[n] = atob(idrscript[a]))), o.length > 0)) {
					e.addEventListener(o, (e => {
						idrform.exec(idrscript[a], e)
					}));
					const t = a + "_Next_";
					let n = 1;
					for (; idrscript[t + n];) e.addEventListener(o, (e => {
						idrform.exec(idrscript[t + n], e)
					})), n++
				}
			}
			if ("button" !== o && e.addEventListener("change", (e => {
				idrform.doc.calculateNow()
			})), "radio" === o) {
				if (e.dataset.hide && e.addEventListener("click", this._hideEvent), e.dataset.show && e.addEventListener("click", this._showEvent), e.dataset.flagRadiosinunison) {
					let t = this._radioUnisonSiblings[e.dataset.fieldName];
					t || (t = {}, this._radioUnisonSiblings[e.dataset.fieldName] = t);
					let o = t[e.value];
					o || (o = [], t[e.value] = o), o.push(e), e.addEventListener("change", (e => {
						this._doRadioUnison(e.currentTarget)
					}))
				}
			} else if ("checkbox" === o) {
				let t = this._checkboxGroups[e.dataset.fieldName];
				t || (t = {}, this._checkboxGroups[e.dataset.fieldName] = t);
				let o = t[e.value];
				o || (o = [], t[e.value] = o), o.push(e), e.addEventListener("change", (e => {
					this._doCheckboxGroup(e.currentTarget)
				}))
			} else if ("text" === o && "input" === e.dataset.editableCombo) {
				const t = undefined
					, o = e.parentElement.querySelector('select[data-editable-combo="select"]');
				o.addEventListener("change", (() => this._doEditableComboSelect(o, e))), e.addEventListener("change", (() => this._doEditableComboInput(e)))
			}
		}
		doc.calculateNow()
	}, this.exec = function(e, t) {
		this.doc.exec(atob(e), t), this.doc.calculateNow()
	}, this.execMenuItem = function(e) {
		this.app.execMenuItem(e)
	}, this.submitForm = function(e) {
		this.doc.submitForm(e)
	}, this._hideEvent = function(e) {
		if (e.target && e.target.dataset && e.target.dataset.hide)
			for (var t = e.target.dataset.hide.split(" "), o = 0; o < t.length; o++) idrform.doc.getField(t[o])
				.display = display.hidden
	}, this._showEvent = function(e) {
		if (e.target && e.target.dataset && e.target.dataset.show)
			for (var t = e.target.dataset.show.split(" "), o = 0; o < t.length; o++) idrform.doc.getField(t[o])
				.display = display.visible
	}, this._doRadioUnison = function(e) {
		this._updateRadioUnisonSiblings(e);
		for (const [t, o] of Object.entries(this._radioUnisonSiblings[e.dataset.fieldName])) t !== e.value && this._updateRadioUnisonSiblings(o[0])
	}, this._updateRadioUnisonSiblings = function(e) {
		const t = undefined;
		this._radioUnisonSiblings[e.dataset.fieldName][e.value].forEach((t => {
			t.checked = e.checked, "refreshApImage" in window && refreshApImage(parseInt(t.dataset.imageIndex))
		}))
	}, this._doCheckboxGroup = function(e) {
		const t = this._checkboxGroups[e.dataset.fieldName]
			, o = e.checked;
		for (const [n, i] of Object.entries(t))
			for (const t of i) t.checked = n === e.value && o, "refreshApImage" in window && refreshApImage(parseInt(t.dataset.imageIndex))
	}, this.getCheckboxGroup = function(e) {
		return this._checkboxGroups[e]
	}, this._doEditableComboInput = function(e) {
		delete e.dataset.realValue
	}, this._doEditableComboSelect = function(e, t) {
		const o = e.options[e.selectedIndex].text
			, n = e.value;
		t.value = o, e.selectedIndex = -1, t.dispatchEvent(new window.Event("change")), t.dataset.realValue = n, doc.calculateNow()
	}, this.getCompletedFormPDF = function() {
		return new Blob([Uint8Array.from(EcmaParser._insertFieldsToPDF(""))
			.buffer], {
			type: "application/pdf"
		})
	};
	const AnnotationType = {
		Caret: "Caret", Circle: "Circle", FileAttachment: "FileAttachment", FreeText: "FreeText"
		, Highlight: "Highlight", Ink: "Ink", Link: "Link", Line: "Line", Polygon: "Polygon"
		, PolyLine: "PolyLine", Sound: "Sound", Square: "Square", Squiggly: "Squiggly"
		, Stamp: "Stamp", StrikeOut: "StrikeOut", Text: "Text", Underline: "Underline"
	}
		, border = {
			s: "solid", d: "dashed", b: "beveled", i: "inset", u: "underline"
		}
		, cursor = {visible: 0, hidden: 1, delay: 2	}
		, display = {visible: 0, hidden: 1, noPrint: 2, noView: 3}
		, font = {
			Times: "Times-Roman", TimesB: "Times-Bold", TimesI: "Times-Italic"
			, TimesBI: "Times-BoldItalic", Helv: "Helvetica", HelvB: "Helvetica-Bold"
			, HelvI: "Helvetica-Oblique", HelvBI: "Helvetica-BoldOblique", Cour: "Courier"
			, CourB: "Courier-Bold", CourI: "Courier-Oblique", CourBI: "Courier-BoldOblique"
			, Symbol: "Symbol", ZapfD: "ZapfDingbats", KaGo: "HeiseiKakuGo-W5-UniJIS-UCS2-H"
			, KaMi: "HeiseiMin-W3-UniJIS-UCS2-H"
		}
		, highlight = {
			n: "none", i: "invert", p: "push", o: "outline"	}
		, position = {textOnly: 0, iconOnly: 1, iconTextV: 2, textIconV: 3
			, iconTextH: 4, textIconH: 5, overlay: 6}
		, style = {
			ch: "check", cr: "cross", di: "diamond", ci: "circle", st: "star", sq: "square"
		}
		, trans = {
			blindsH: "BlindsHorizontal"
			, blindsV: "BlindsVertical"
			, boxI: "BoxIn"
			, boxO: "BoxOut"
			, dissolve: "Dissolve"
			, glitterD: "GlitterDown"
			, glitterR: "GlitterRight"
			, glitterRD: "GlitterRightDown"
			, random: "Random"
			, replace: "Replace"
			, splitHI: "SplitHorizontalIn"
			, splitHO: "SplitHorizontalOut"
			, splitVI: "SplitVerticalIn"
			, splitVO: "SplitVerticalOut"
			, wipeD: "WipeDown"
			, wipeL: "WipeLeft"
			, wipeR: "WipeRight"
			, wipeU: "WipeUp"
		}
		, zoomType = {
			none: "NoVary"
			, fitP: "FitPage"
			, fitW: "FitWidth"
			, fitH: "fitHeight"
			, fitV: "fitVisibleWidth"
			, pref: "Preferred"
			, refW: "ReflowWidth"
		}
		, DS_GREATER_THAN = "Invalid value: must be greater than or equal to %s."
		, IDS_GT_AND_LT = "Invalid value: must be greater than or equal to %s and less than or equal to %s."
		, IDS_LESS_THAN = "Invalid value: must be less than or equal to %s."
		, IDS_INVALID_MONTH = "** Invalid **"
		, IDS_INVALID_DATE2 = "should match format"
		, IDS_INVALID_VALUE = "The value entered does not match the format of the field";

	function AFExecuteThisScript(e, t, o) {
		return console.log("method not defined contact - IDR SOLUTIONS"), event.rc
	}

	function AFImportAppearance(e, t, o, n) {
		return console.log("method not defined contact - IDR SOLUTIONS"), !0
	}

	function AFLayoutBorder(e, t, o, n, i) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFLayoutCreateStream(e) {
		return console.log("method not defined contact - IDR SOLUTIONS"), null
	}

	function AFLayoutDelete(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFLayoutNew(e, t, o) {
		return console.log("method not defined contact - IDR SOLUTIONS"), null
	}

	function AFLayoutText(e, t, o, n, i, a) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDDocEnumPDFields(e, t, o, n, i) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDDocGetPDFieldFromName(e, t) {
		return e.getField(t)
	}

	function AFPDDocLoadPDFields(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldFromCosObj(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldGetCosObj(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldGetDefaultTextAppearance(e, t) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldGetFlags(e, t) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldGetName(e) {
		return e.name
	}

	function AFPDFieldGetValue(e) {
		return e.value
	}

	function AFPDFieldIsAnnot(e) {
		return console.log("AFPDFieldIsAnnot not defined contact - IDR SOLUTIONS"), !1
	}

	function AFPDFieldIsTerminal(e) {
		return console.log("AFPDFieldIsTerminal not defined contact - IDR SOLUTIONS"), !0
	}

	function AFPDFieldIsValid(e) {
		return console.log("AFPDFieldIsValid not defined contact - IDR SOLUTIONS"), !0
	}

	function AFPDFieldReset(e) {
		console.log("AFPDFieldReset not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldSetDefaultTextAppearance(e, t) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldSetFlags(e, t, o) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDFieldSetOptions(e, t) {
		return console.log("method not defined contact - IDR SOLUTIONS"), "Good"
	}

	function AFPDFieldSetValue(e, t) {
		e.value = t
	}

	function AFPDFormFromPage(e, t) {
		return console.log("method not defined contact - IDR SOLUTIONS"), null
	}

	function AFPDWidgetGetAreaColors(e, t, o) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDWidgetGetBorder(e, t) {
		return console.log("method not defined contact - IDR SOLUTIONS"), !0
	}

	function AFPDWidgetGetRotation(e) {
		return console.log("method not defined contact - IDR SOLUTIONS"), null
	}

	function AFPDWidgetSetAreaColors(e, t, o) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPDWidgetSetBorder(e, t) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFSimple_Calculate(e, t) {
		let o = 0;
		switch (e) {
			case "AVG":
				let e = 0;
				for (const n of t) {
					const t = doc.getField(n);
					null != t && null != t.value && (e++, o += Number(t.value))
				}
				o /= e;
				break;
			case "MIN":
				o = doc.getField(t[0])
					.value;
				for (const e of t) {
					const t = doc.getField(e);
					null != t && null != t.value && (o = Math.min(o, t.value))
				}
				break;
			case "MAX":
				o = doc.getField(t[0])
					.value;
				for (const e of t) {
					const t = doc.getField(e);
					null != t && null != t.value && (o = Math.max(o, t.value))
				}
				break;
			case "PRD":
				o = 1;
				for (const e of t) {
					const t = doc.getField(e);
					null != t && null != t.value && (o *= t.value)
				}
				break;
			case "SUM":
				for (const e of t) {
					const t = doc.getField(e);
					null != t && null != t.value && (o += Number(t.value))
				}
				break
		}
		return o
	}

	function AFDate_KeystrokeEx(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFDate_Format(e) {
		var t = e
			, o = event.value
			, n, i;
		if (null != o && ("" + o)
			.length > 0 && null == util.scand(t, o)) {
			var a = "Invalid date/time: please ensure that the date/time exists. Field [" + event.target.name + "] should match the format " + t;
			alert(a), event.value = null
		}
	}

	function AFDate_FormatEx(e) {
		AFDate_Format(e)
	}

	function AFTime_Keystroke(e) {
		AFTime_Format(e)
	}

	function AFTime_Format(e) {
		var t = cFormat
			, o = event.value
			, n;
		if (null == util.scand(t, o)) {
			var i = "Invalid date/time: please ensure that the date/time exists. Field [" + event.target.name + "] should match the format " + t;
			alert(i), event.value = null
		}
	}

	function AFPercent_Keystroke(e, t) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFPercent_Format(e, t) {
		if ("number" == typeof e && "number" == typeof t) {
			if (e < 0 && (alert("Invalid nDec value in AFPercent_Format"), event.value = null), e > 512) return event.value = "%", void 0;
			e = Math.floor(e), t = Math.min(Math.max(0, Math.floor(t)), 4);
			var o = AFMakeNumber(event.value);
			if (null === o) return event.value = "%", void 0;
			event.value = 100 * o + "%"
		}
	}

	function AFSpecial_Keystroke(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AFSpecial_Format(e) {
		var t;
		switch (e = AFMakeNumber(e)) {
			case 0:
				t = "99999";
				break;
			case 1:
				t = "99999-9999";
				break;
			case 2:
				var o = "" + event.value;
				t = o.length > 8 || o.startsWith("(") ? "(999) 999-9999" : "999-9999";
				break;
			case 3:
				t = "999-99-9999";
				break;
			default:
				return alert("Invalid psf in AFSpecial_Keystroke"), void 0
		}
		event.value = util.printx(t, event.value)
	}

	function AFMakeNumber(e) {
		if ("number" == typeof e) return e;
		if ("string" != typeof e) return null;
		e = e.trim()
			.replace(",", ".");
		const t = parseFloat(e);
		return isNaN(t) || !isFinite(t) ? null : t
	}

	function AFNumber_Format(e, t, o, n, i, a) {
		var r = event.value;
		null != (r = AFMakeNumber(r)) && (event.value = r)
	}

	function AFNumber_Keystroke(e, t, o, n, i, a) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function AssembleFormAndImportFDF(e, t, o) {
		return console.log("method not defined contact - IDR SOLUTIONS"), doc
	}

	function ExportAsFDF(e, t, o, n, i, a, r) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function ExportAsFDFEx(e, t, o, n, i, a, r, s) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function ExportAsFDFWithParams(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function ExportAsHtml(e, t, o, n, i) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function ExportAsHtmlEx(e, t, o, n, i, a) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function ImportAnFDF(e, t) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function IsPDDocAcroForm(e) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function ResetForm(e, t, o) {
		console.log("method not defined contact - IDR SOLUTIONS")
	}

	function App() {
		this.getEcmaParser = function() { return EcmaParser; };
		this.isAcroForm = !0, this.activeDocs = [doc], this.calculate = !0, this.contstants = null, this.focusRect = !0, this.formsVersion = 6, this.fromPDFConverters = new Array, this.fs = new FullScreen, this.fullScreen = !1, this.language = "ENU", this.media = new Media, this.monitors = {}, this.numPlugins = 0, this.openInPlace = !1, this.platform = "WIN", this.plugins = new Array, this.printColorProfiles = new Array, this.printNames = new Array, this.runtimeHighlight = !1, this.runtimeHightlightColor = new Array, this.thermometer = new Thermometer, this.toolBar = !1, this.toolBarHorizontal = !1, this.toolBarVertical = !1, this.viewerType = "Exchange-Pro", this.viewerVariation = "Full", this.viewerVersion = 6, this.addMenuItem = function() {
			console.log("addMenuItem method not defined contact - IDR SOLUTIONS")
		}, this.addSubMenu = function() {
			console.log("addSubMenu method not defined contact - IDR SOLUTIONS")
		}, this.addToolButton = function() {
			console.log("addToolButton method not defined contact - IDR SOLUTIONS")
		}, this.alert = function(e, t, o, n, i, a) {
			var r = {
				cMsg: e
				, nIcon: 0
				, nType: 0
				, cTitle: "Adobe Acrobat"
				, oDoc: null
				, oCheckBox: null
			};
			if (e instanceof Object)
				for (var s in e) r[s] = e[s];
			switch (void 0 !== o && (r.nType = o), r.nType) {
				case 0:
					return window.alert(r.cMsg), void 0;
				case 1:
				case 2:
				case 3:
					return window.confirm(r.cMsg)
			}
		}, this.beep = function() {
			var e;
			new Audio("data:audio/wav;base64,//uQRAAAAWMSLwUIYAAsYkXgoQwAEaYLWfkWgAI0wWs/ItAAAGDgYtAgAyN+QWaAAihwMWm4G8QQRDiMcCBcH3Cc+CDv/7xA4Tvh9Rz/y8QADBwMWgQAZG/ILNAARQ4GLTcDeIIIhxGOBAuD7hOfBB3/94gcJ3w+o5/5eIAIAAAVwWgQAVQ2ORaIQwEMAJiDg95G4nQL7mQVWI6GwRcfsZAcsKkJvxgxEjzFUgfHoSQ9Qq7KNwqHwuB13MA4a1q/DmBrHgPcmjiGoh//EwC5nGPEmS4RcfkVKOhJf+WOgoxJclFz3kgn//dBA+ya1GhurNn8zb//9NNutNuhz31f////9vt///z+IdAEAAAK4LQIAKobHItEIYCGAExBwe8jcToF9zIKrEdDYIuP2MgOWFSE34wYiR5iqQPj0JIeoVdlG4VD4XA67mAcNa1fhzA1jwHuTRxDUQ//iYBczjHiTJcIuPyKlHQkv/LHQUYkuSi57yQT//uggfZNajQ3Vmz+Zt//+mm3Wm3Q576v////+32///5/EOgAAADVghQAAAAA//uQZAUAB1WI0PZugAAAAAoQwAAAEk3nRd2qAAAAACiDgAAAAAAABCqEEQRLCgwpBGMlJkIz8jKhGvj4k6jzRnqasNKIeoh5gI7BJaC1A1AoNBjJgbyApVS4IDlZgDU5WUAxEKDNmmALHzZp0Fkz1FMTmGFl1FMEyodIavcCAUHDWrKAIA4aa2oCgILEBupZgHvAhEBcZ6joQBxS76AgccrFlczBvKLC0QI2cBoCFvfTDAo7eoOQInqDPBtvrDEZBNYN5xwNwxQRfw8ZQ5wQVLvO8OYU+mHvFLlDh05Mdg7BT6YrRPpCBznMB2r//xKJjyyOh+cImr2/4doscwD6neZjuZR4AgAABYAAAABy1xcdQtxYBYYZdifkUDgzzXaXn98Z0oi9ILU5mBjFANmRwlVJ3/6jYDAmxaiDG3/6xjQQCCKkRb/6kg/wW+kSJ5//rLobkLSiKmqP/0ikJuDaSaSf/6JiLYLEYnW/+kXg1WRVJL/9EmQ1YZIsv/6Qzwy5qk7/+tEU0nkls3/zIUMPKNX/6yZLf+kFgAfgGyLFAUwY//uQZAUABcd5UiNPVXAAAApAAAAAE0VZQKw9ISAAACgAAAAAVQIygIElVrFkBS+Jhi+EAuu+lKAkYUEIsmEAEoMeDmCETMvfSHTGkF5RWH7kz/ESHWPAq/kcCRhqBtMdokPdM7vil7RG98A2sc7zO6ZvTdM7pmOUAZTnJW+NXxqmd41dqJ6mLTXxrPpnV8avaIf5SvL7pndPvPpndJR9Kuu8fePvuiuhorgWjp7Mf/PRjxcFCPDkW31srioCExivv9lcwKEaHsf/7ow2Fl1T/9RkXgEhYElAoCLFtMArxwivDJJ+bR1HTKJdlEoTELCIqgEwVGSQ+hIm0NbK8WXcTEI0UPoa2NbG4y2K00JEWbZavJXkYaqo9CRHS55FcZTjKEk3NKoCYUnSQ0rWxrZbFKbKIhOKPZe1cJKzZSaQrIyULHDZmV5K4xySsDRKWOruanGtjLJXFEmwaIbDLX0hIPBUQPVFVkQkDoUNfSoDgQGKPekoxeGzA4DUvnn4bxzcZrtJyipKfPNy5w+9lnXwgqsiyHNeSVpemw4bWb9psYeq//uQZBoABQt4yMVxYAIAAAkQoAAAHvYpL5m6AAgAACXDAAAAD59jblTirQe9upFsmZbpMudy7Lz1X1DYsxOOSWpfPqNX2WqktK0DMvuGwlbNj44TleLPQ+Gsfb+GOWOKJoIrWb3cIMeeON6lz2umTqMXV8Mj30yWPpjoSa9ujK8SyeJP5y5mOW1D6hvLepeveEAEDo0mgCRClOEgANv3B9a6fikgUSu/DmAMATrGx7nng5p5iimPNZsfQLYB2sDLIkzRKZOHGAaUyDcpFBSLG9MCQALgAIgQs2YunOszLSAyQYPVC2YdGGeHD2dTdJk1pAHGAWDjnkcLKFymS3RQZTInzySoBwMG0QueC3gMsCEYxUqlrcxK6k1LQQcsmyYeQPdC2YfuGPASCBkcVMQQqpVJshui1tkXQJQV0OXGAZMXSOEEBRirXbVRQW7ugq7IM7rPWSZyDlM3IuNEkxzCOJ0ny2ThNkyRai1b6ev//3dzNGzNb//4uAvHT5sURcZCFcuKLhOFs8mLAAEAt4UWAAIABAAAAAB4qbHo0tIjVkUU//uQZAwABfSFz3ZqQAAAAAngwAAAE1HjMp2qAAAAACZDgAAAD5UkTE1UgZEUExqYynN1qZvqIOREEFmBcJQkwdxiFtw0qEOkGYfRDifBui9MQg4QAHAqWtAWHoCxu1Yf4VfWLPIM2mHDFsbQEVGwyqQoQcwnfHeIkNt9YnkiaS1oizycqJrx4KOQjahZxWbcZgztj2c49nKmkId44S71j0c8eV9yDK6uPRzx5X18eDvjvQ6yKo9ZSS6l//8elePK/Lf//IInrOF/FvDoADYAGBMGb7FtErm5MXMlmPAJQVgWta7Zx2go+8xJ0UiCb8LHHdftWyLJE0QIAIsI+UbXu67dZMjmgDGCGl1H+vpF4NSDckSIkk7Vd+sxEhBQMRU8j/12UIRhzSaUdQ+rQU5kGeFxm+hb1oh6pWWmv3uvmReDl0UnvtapVaIzo1jZbf/pD6ElLqSX+rUmOQNpJFa/r+sa4e/pBlAABoAAAAA3CUgShLdGIxsY7AUABPRrgCABdDuQ5GC7DqPQCgbbJUAoRSUj+NIEig0YfyWUho1VBBBA//uQZB4ABZx5zfMakeAAAAmwAAAAF5F3P0w9GtAAACfAAAAAwLhMDmAYWMgVEG1U0FIGCBgXBXAtfMH10000EEEEEECUBYln03TTTdNBDZopopYvrTTdNa325mImNg3TTPV9q3pmY0xoO6bv3r00y+IDGid/9aaaZTGMuj9mpu9Mpio1dXrr5HERTZSmqU36A3CumzN/9Robv/Xx4v9ijkSRSNLQhAWumap82WRSBUqXStV/YcS+XVLnSS+WLDroqArFkMEsAS+eWmrUzrO0oEmE40RlMZ5+ODIkAyKAGUwZ3mVKmcamcJnMW26MRPgUw6j+LkhyHGVGYjSUUKNpuJUQoOIAyDvEyG8S5yfK6dhZc0Tx1KI/gviKL6qvvFs1+bWtaz58uUNnryq6kt5RzOCkPWlVqVX2a/EEBUdU1KrXLf40GoiiFXK///qpoiDXrOgqDR38JB0bw7SoL+ZB9o1RCkQjQ2CBYZKd/+VJxZRRZlqSkKiws0WFxUyCwsKiMy7hUVFhIaCrNQsKkTIsLivwKKigsj8XYlwt/WKi2N4d//uQRCSAAjURNIHpMZBGYiaQPSYyAAABLAAAAAAAACWAAAAApUF/Mg+0aohSIRobBAsMlO//Kk4soosy1JSFRYWaLC4qZBYWFRGZdwqKiwkNBVmoWFSJkWFxX4FFRQWR+LsS4W/rFRb/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////VEFHAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAU291bmRib3kuZGUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAMjAwNGh0dHA6Ly93d3cuc291bmRib3kuZGUAAAAAAAAAACU=")
				.play()
		}, this.beginPriv = function() {
			console.log("beginPriv method not defined contact - IDR SOLUTIONS")
		}, this.browseForDoc = function() {
			console.log("browseForDoc method not defined contact - IDR SOLUTIONS")
		}, this.clearInterval = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.clearTimeOut = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.endPriv = function() {
			console.log("endPriv method not defined contact - IDR SOLUTIONS")
		}, this.execDialog = function() {
			console.log("execDialog method not defined contact - IDR SOLUTIONS")
		}, this.execMenuItem = function(e) {
			var t = document.getElementsByClassName("pageArea")
				.length
				, o = e.toUpperCase();
			if ("SAVEAS" === o)
				if (this.isAcroForm) {
					var n = document.getElementById("FDFXFA_PDFName")
						.textContent;
					EcmaParser.saveFormToPDF(n)
				} else createXFAPDF();
			else "PRINT" === o ? this.activeDocs[0].print() : "FIRSTPAGE" === o ? this.activeDocs[0].pageNum = 0 : "PREVPAGE" === o ? this.activeDocs[0].pageNum-- : "NEXTPAGE" === o ? this.activeDocs[0].pageNum++ : "LASTPAGE" === o && (this.activeDocs[0].pageNum = t - 1)
		}, this.getNthPluginName = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.getPath = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.goBack = function() {
			this.activeDocs[0].pageNum--
		}, this.goForward = function() {
			this.activeDocs[0].pageNum++
		}, this.hideMenuItem = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.hideToolbarButton = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.launchURL = function(e, t) {
			app.activeDocs[0].getURL(e)
		}, this.listMenuItems = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.listToolbarButtons = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.mailGetAddrs = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.mailMsg = function(e, t, o, n, i, a) {
			var r = "mailto:";
			r += t.split(";")
				.join(",");
			var s = !1;
			o && (s = !0, r += "?cc=", r += o.split(";")
				.join(",")), n && (s ? r += "&" : (s = !0, r += "?"), r += n.split(";")
					.join(",")), i && (s ? r += "&" : (s = !0, r += "?"), r += i.split(" ")
						.join("%20")), a && (s ? r += "&" : (s = !0, r += "?"), r += a.split(" ")
							.join("%20")), window.location.href = r
		}, this.mailGetAddrs = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.newDoc = function() {
			return new Doc
		}, this.newFDF = function() {
			return new FDF
		}, this.openDoc = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.openFDF = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.popUpMenu = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.popUpMenuEx = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.removeToolButton = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.response = function(e, t, o, n) {
			var i;
			return i = t ? window.prompt(e, t) : window.prompt(e)
		}, this.setInterval = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.setTimeOut = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.trustedFunction = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.trustPropagatorFunction = function() {
			console.log("method not defined contact - IDR SOLUTIONS")
		}
	}

	function Doc() {
		this.pages = [], this.alternatePresentations = {}, this.author = "", this.baseURL = "", this.bookmarkRoot = {}, this.calculate = !1, this.creationDate = new Date, this.creator = "", this.dataObjects = [], this.delay = !1, this.dirty = !1, this.disclosed = !1, this.docID = [], this.documentFileName = "", this.dynamicXFAForm = !1, this.external = !0, this.fileSize = 0, this.hidden = !1, this.hostContainer = {}, this.icons = [], this.info = {}, this.innerAppWindowRect = [], this.innerDocWindowRect = [], this.isModal = !1, this.keywords = {}, this.layout = "", this.media = {}, this.metadata = "", this.modDate = new Date, this.mouseX = 0, this.mouseY = 0, this.noautoComplete = !1, this.nocache = !1, this.numPages = 0, this.numTemplates = 0, this.path = "", this.outerAppWindowRect = [], this.outerDocWindowRect = [], this.pageNum = 0, this.pageWindowRect = [], this.permStatusReady = !1, this.producer = "PDFWriter", this.requiresFullSave = !1, this.securityHandler = "", this.selectedAnnots = [], this.sounds = [], this.spellDictionaryOrder = [], this.spellLanguageOrder = [], this.subject = "", this.templates = [], this.title = "", this.URL = "", this.viewState = {}, this.xfa = {}, this.XFAForeground = !1, this.zoom = 100, this.zoomType = "novary", this.exec = function(scr, htmlEvent) {
			try {
				console.log(htmlEvent), event = new Event(htmlEvent, null), eval(scr), event = void 0
			} catch (e) {
				console.log(e)
			}
		}
	}

	function Events() {
		this.add = function() {
			console.log("add method not defined contact - IDR SOLUTIONS")
		}, this.dispatch = function() {
			console.log("dispatch method not defined contact - IDR SOLUTIONS")
		}, this.remove = function() {
			console.log("remove method not defined contact - IDR SOLUTIONS")
		}
	}

	function EventListener() {
		this.afterBlur = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterClose = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterDestroy = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterDone = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterError = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterEscape = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterEveryEvent = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterFocus = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterPause = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterPlay = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterReady = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterScript = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterSeek = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterStatus = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.afterStop = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onBlur = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onClose = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onDestroy = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onDone = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onError = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onEscape = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onEveryEvent = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onFocus = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onGetRect = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onPause = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onPlay = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onReady = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onScript = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onSeek = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onStatus = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}, this.onStrop = function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}
	}

	function hexToRgbCss(e) {
		var t = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
		e = e.replace(t, (function(e, t, o, n) {
			return t + t + o + o + n + n
		}));
		var o = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(e)
			, n, i, a;
		return "rgb(" + parseInt(o[1], 16) + "," + parseInt(o[2], 16) + "," + parseInt(o[3], 16) + ")"
	}

	function rgbToHexCss(e, t, o) {
		return "#" + ((1 << 24) + (e << 16) + (t << 8) + o)
			.toString(16)
			.slice(1)
	}

	function rgbCssToArr(e) {
		return e.replace(/[^\d,]/g, "")
			.split(",")
	}
	console.println = function(e) {
		console.log(e)
	}, Object.defineProperty(Doc.prototype, "addAnnot", {
		value: function(e) {
			return console.log("addAnnot method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addField", {
		value: function(e, t, o, n) {
			var i = document.getElementsByClassName("pageArea")
				, a;
			switch (t) {
				case "text":
					(a = document.createElement("input"))
						.setAttribute("type", "text");
					break;
				case "button":
					a = document.createElement("button");
					break;
				case "combobox":
					a = document.createElement("select");
					break;
				case "listbox":
					a = document.createElement("select");
					break;
				case "checkbox":
					(a = document.createElement("input"))
						.setAttribute("type", "checkbox");
					break;
				case "radiobutton":
					(a = document.createElement("input"))
						.setAttribute("type", "radio");
					break;
				default:
					a = document.createElement("div")
			}
			return a.setAttribute("data-field-name", e), a.style.position = "absolute", a.style.left = n[0], a.style.top = n[1], i[o].appendChild(a), new Field(a)
		}
	}), Object.defineProperty(Doc.prototype, "addIcon", {
		value: function(e, t) {
			return this.icons.push(t), null
		}
	}), Object.defineProperty(Doc.prototype, "addLink", {
		value: function(e, t) {
			var o = document.getElementsByClassName("pageArea")
				, n = document.createElement("a");
			return n.style.position = "absolute", n.style.left = t[0], n.style.top = t[1], o[e].appendChild(n), new Link(n)
		}
	}), Object.defineProperty(Doc.prototype, "addRecipientListCryptFilter", {
		value: function(e, t) {
			return console.log("addRecipientListCryptFilter method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addRequirement", {
		value: function(e, t) {
			return console.log("addRequirement method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addScript", {
		value: function(e, t) {
			return console.log("addScript method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addThumbnails", {
		value: function(e, t) {
			return console.log("addThumbnails method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addWatermarkFromFile", {
		value: function(e) {
			return console.log("addWatermarkFromFile method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addWatermarkFromText", {
		value: function(e) {
			return console.log("addWatermarkFromText method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "addWeblinks", {
		value: function(e, t) {
			return console.log("addWeblinks method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "bringToFront", {
		value: function() {
			return console.log("bringToFront method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "calculateNow", {
		value: function() {
			for (const [fieldId, script] of Object.entries(AVAIL_CALCULATES)) {
				const target = document.getElementById(fieldId);
				if (target) {
					event = new Event(null, target);
					const res = eval(script);
					null != res && (target.value = res)
				}
			}
			return event = void 0, 1
		}
	}), Object.defineProperty(Doc.prototype, "closeDoc", {
		value: function(e) {
			window.close()
		}
	}), Object.defineProperty(Doc.prototype, "colorConvertPage", {
		value: function(e, t, o) {
			return console.log("colorConvertPage method not defined contact - IDR SOLUTIONS"), !0
		}
	}), Object.defineProperty(Doc.prototype, "createDataObject", {
		value: function(e, t, o, n) {
			console.log("createDataObject method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "createTemplate", {
		value: function(e, t) {
			console.log("createTemplate method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "deletePages", {
		value: function(e, t) {
			console.log("deletePages method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "embedDocAsDataObject", {
		value: function(e, t, o, n) {
			console.log("embedDocAsDataObject method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "embedOutputIntent", {
		value: function(e) {
			console.log("embedOutputIntent method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "encryptForRecipients", {
		value: function(e, t, o) {
			return console.log("encryptForRecipients method not defined contact - IDR SOLUTIONS"), !1
		}
	}), Object.defineProperty(Doc.prototype, "encryptUsingPolicy", {
		value: function(e, t, o, n) {
			return console.log("encryptUsingPolicy method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "exportAsFDF", {
		value: function() {
			console.log("exportAsFDF method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "exportAsText", {
		value: function() {
			console.log("exportAsFDF method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "exportAsXFDF", {
		value: function() {
			console.log("exportAsXFDF method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "exportAsXFDFStr", {
		value: function() {
			console.log("exportAsXFDF method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "exportDataObject", {
		value: function() {
			console.log("exportDataObject method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "exportXFAData", {
		value: function() {
			console.log("exportXFAData method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "extractPages", {
		value: function(e, t, o) {
			console.log("extractPages method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "flattenPages", {
		value: function(e, t, o) {
			console.log("flattenPages method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "getAnnot", {
		value: function(e, t) {
			return console.log("getAnnot method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "getAnnot3D", {
		value: function(e, t) {
			return console.log("getAnnot3D method not defined contact - IDR SOLUTIONS"), null
		}
	}), Object.defineProperty(Doc.prototype, "getAnnots", {
		value: function(e, t, o) {
			return console.log("getAnnots method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getAnnots3D", {
		value: function(e, t, o) {
			return console.log("getAnnots3D method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getColorConvertAction", {
		value: function() {
			return console.log("getColorConvertAction method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getDataObject", {
		value: function(e) {
			return console.log("getDataObject method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getDataObjectContents", {
		value: function(e, t) {
			return console.log("getDataObjectContents method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getField", {
		value: function(e) {
			var t = document.querySelectorAll('[data-field-name="' + e + '"]')
				, o = t[0];
			if (t.length > 1 && "radio" == o.getAttribute("type"))
				for (var n = 0, i = t.length; n < i; n++)
					if (t[n].checked) return new Field(t[n]);
			return new Field(o)
		}
	}), Object.defineProperty(Doc.prototype, "getIcon", {
		value: function(e) {
			for (var t = 0, o = this.icons.length; t < o; t++)
				if (this.icons[t].name === e) return this.icons[t];
			return new Icon
		}
	}), Object.defineProperty(Doc.prototype, "getLegalWarnings", {
		value: function(e) {
			return console.log("getLegalWarnings method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getLinks", {
		value: function(e, t) {
			return console.log("getLinks method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getNthFieldName", {
		value: function(e) {
			var t, o = document.querySelectorAll("[data-field-name]")[e];
			return o ? o.getAttribute("data-field-name") : ""
		}
	}), Object.defineProperty(Doc.prototype, "getNthTemplate", {
		value: function(e) {
			return console.log("getNthTemplate method not defined contact - IDR SOLUTIONS"), ""
		}
	}), Object.defineProperty(Doc.prototype, "getOCGs", {
		value: function(e) {
			return console.log("getOCGs method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getOCGOrder", {
		value: function() {
			return console.log("getOCGOrder method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getPageBox", {
		value: function(e, t) {
			return console.log("getPageBox method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getPageLabel", {
		value: function(e) {
			return console.log("getPageLabel method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getPageNthWord", {
		value: function(e, t, o) {
			return console.log("getPageNthWord method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getPageNthWordQuads", {
		value: function(e, t) {
			return console.log("getPageNthWordQuards method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getPageNumWords", {
		value: function(e) {
			return console.log("getPageNumWords method not defined contact - IDR SOLUTIONS"), 0
		}
	}), Object.defineProperty(Doc.prototype, "getPageRotation", {
		value: function(e) {
			return console.log("getPageRotation method not defined contact - IDR SOLUTIONS"), 0
		}
	}), Object.defineProperty(Doc.prototype, "getPageTransition", {
		value: function(e) {
			return console.log("getPageTransition method not defined contact - IDR SOLUTIONS"), []
		}
	}), Object.defineProperty(Doc.prototype, "getPrintParams", {
		value: function() {
			return console.log("getPrintParams method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getSound", {
		value: function(e) {
			return console.log("getSound method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getTemplate", {
		value: function(e) {
			return console.log("getTemplate method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(Doc.prototype, "getURL", {
		value: function(e, t) {
			console.log("getURL method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "gotoNamedDest", {
		value: function(e) {
			console.log("gotoNamedDest method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "importAnFDF", {
		value: function(e) {
			console.log("importAnFDF method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "importDataObject", {
		value: function(e, t) {
			console.log("importDataObject method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "importIcon", {
		value: function(e, t) {
			console.log("importIcon method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "importSound", {
		value: function(e) {
			console.log("importSound method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "importTextData", {
		value: function(e, t) {
			return console.log("importTextData method not defined contact - IDR SOLUTIONS"), 0
		}
	}), Object.defineProperty(Doc.prototype, "importXFAData", {
		value: function(e) {
			console.log("importXFAData method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "insertPages", {
		value: function(e, t, o, n) {
			console.log("insertPages method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "mailDoc", {
		value: function() {
			console.log("mailDoc method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "mailForm", {
		value: function() {
			console.log("mailForm method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "movePage", {
		value: function(e, t) {
			console.log("movePage method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "newPage", {
		value: function(e, t, o) {
			console.log("newPage method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "numFields", {
		get: function() {
			var e;
			return document.querySelectorAll("[data-field-name]")
				.length
		}
	}), Object.defineProperty(Doc.prototype, "openDataObject", {
		value: function(e) {
			return console.log("openDataObject method not defined contact - IDR SOLUTIONS"), this
		}
	}), Object.defineProperty(Doc.prototype, "print", {
		value: function() {
			window.print()
		}
	}), Object.defineProperty(Doc.prototype, "removeDataObject", {
		value: function(e) {
			console.log("removeDataObject method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeField", {
		value: function(e) {
			var t;
			document.querySelector('[data-field-name="' + e + '"]')
				.remove()
		}
	}), Object.defineProperty(Doc.prototype, "removeIcon", {
		value: function(e) {
			console.log("removeIcon method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeLinks", {
		value: function(e, t) {
			console.log("removeLinks method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeRequirement", {
		value: function(e) {
			console.log("removeRequirement method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeScript", {
		value: function(e) {
			console.log("removeScript method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeTemplate", {
		value: function(e) {
			console.log("removeTemplate method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeThumbnails", {
		value: function(e, t) {
			console.log("removeThumbnails method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "removeWeblinks", {
		value: function(e, t) {
			console.log("removeWeblinks method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "replacePages", {
		value: function(e, t, o, n) {
			console.log("replacePages method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "resetForm", {
		value: function(e) {
			if (e);
			else {
				for (var t = document.getElementsByTagName("form")[0], o = t.elements, n = 0; n < o.length; n++) {
					var i;
					if (o[n].dataset && o[n].dataset.fieldName && o[n].dataset.defaultDisplay) idrform.doc.getField(o[n].dataset.fieldName)
						.display = Number(o[n].dataset.defaultDisplay)
				}
				t.reset()
			}
		}
	}), Object.defineProperty(Doc.prototype, "saveAs", {
		value: function(e, t, o, n, i) {
			var a;
			if (this._checkRequired()) return window.alert("At least one required field was empty on export. Please fill in required fields (highlighted) before continuing"), void 0;
			console.log("saveAs method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "scroll", {
		value: function(e, t) {
			console.log("scroll method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "selectPageNthWord", {
		value: function(e, t, o) {
			console.log("selectPageNthWord method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setAction", {
		value: function(e, t) {
			console.log("setAction method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setDataObjectContents", {
		value: function(e, t, o) {
			console.log("setDataObjectContents method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setOCGOrder", {
		value: function(e) {
			console.log("setOCGOrder method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setPageAction", {
		value: function(e, t) {
			console.log("setPageAction method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setPageBoxes", {
		value: function(e, t, o, n) {
			console.log("setPageBoxes method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setPageLabels", {
		value: function(e, t) {
			console.log("setPageLabels method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setPageTabOrder", {
		value: function(e, t) {
			console.log("setPageTabOrder method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "setPageTransitions", {
		value: function(e, t, o) {
			console.log("setPageTransitions method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "spawnPageFromTemplate", {
		value: function(e, t, o, n, i) {
			console.log("spawnPageFromTemplate method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Doc.prototype, "_getSelectElementValues", {
		value: function(e) {
			return e.getAttribute("multiple") ? [...e.children].filter((e => e.selected))
				.map((e => e.value)) : e.value
		}
	}), Object.defineProperty(Doc.prototype, "_getFieldsHTML", {
		value: function(e) {
			return e.flatMap((e => [...document.getElementsByTagName(e)]))
		}
	}), Object.defineProperty(Doc.prototype, "_getFormFields", {
		value: function() {
			const e = document.getElementsByTagName("input")
				, t = document.getElementsByTagName("textarea")
				, o = document.getElementsByTagName("select")
				, n = {
					texts: []
					, checks: []
					, checkGroups: []
					, radios: []
					, choices: []
					, editableChoices: []
					, buttons: []
				};
			return [...e].filter((e => e.dataset.objref?.length > 0 && e.dataset.fieldName))
				.forEach((e => {
					switch (e.type.toUpperCase()) {
						case "TEXT":
						case "PASSWORD":
							"input" === e.getAttribute("data-editable-combo") ? n.editableChoices.push(e) : n.texts.push(e);
							break;
						case "CHECKBOX":
							Object.keys(idrform.getCheckboxGroup(e.dataset.fieldName))
								.length > 1 ? n.checkGroups.push(e) : n.checks.push(e);
							break;
						case "RADIO":
							e.name === e.dataset.fieldName && n.radios.push(e);
							break;
						case "BUTTON":
							n.buttons.push(e);
							break
					}
				})), n.texts.push(...[...t].filter((e => e.dataset.objref?.length > 0 && e.dataset.fieldName))), n.choices = [...o].filter((e => e.dataset.objref?.length > 0 && e.dataset.fieldName)), n
		}
	}), Object.defineProperty(Doc.prototype, "_getFormData", {
		value: function(e = null, t = !1, o = !1) {
			if (e && 0 === e.length) return {};
			const n = {}
				, {
					texts: i
					, checks: a
					, checkGroups: r
					, radios: s
					, choices: c
					, editableChoices: l
				} = doc._getFormFields();
			let d = t => !e || e.includes(t.dataset.fieldName);
			return [...i, ...l].filter(d)
				.filter((e => t || (e.dataset.realValue || e.value)
					?.length > 0))
				.forEach((e => n[e.dataset.fieldName] = e.dataset.realValue || e.value || "")), [...a, ...r, ...s].filter(d)
					.filter((e => e.checked))
					.forEach((e => n[e.dataset.fieldName] = e.value)), c.filter(d)
						.filter((e => t || doc._getSelectElementValues(e)
							?.length > 0))
						.forEach((e => n[e.dataset.fieldName] = doc._getSelectElementValues(e) || "")), n
		}
	}), Object.defineProperty(Doc.prototype, "_checkRequired", {
		value: function(e) {
			let t = !1
				, o = t => void 0 === e || e.includes(t.dataset.fieldName);
			for (const e of this._getFieldsHTML(["input", "textarea", "select"])) o(e) && e.hasAttribute("required") && (null === e.value || "" === e.value) && (e.style.border = "1px solid red", t = !0);
			return t
		}
	}), Object.defineProperty(Doc.prototype, "_buildXMLString", {
		value: function(e) {
			let t = "<?xml version='1.0' encoding='UTF-8'?><fields xmlns:xfdf=\"http://ns.adobe.com/xfdf-transition/\">";
			for (const [o, n] of Object.entries(e)
				.sort(((e, t) => e[0].localeCompare(t[0])))) {
				const e = o.replace(/[<&"]/g, (e => {
					switch (e) {
						case "<":
							return "&lt;";
						case "&":
							return "&amp;";
						case '"':
							return "&quot;"
					}
				}));
				let i;
				if (i = o.match(/[\[\]<>&']/g) ? n instanceof Array ? "group" : "field" : e.replace(/[" ]/g, ""), t += i !== e ? `<${i} xfdf:original="${e}"` : `<${i}`, "TEXTAREA" === document.querySelector(`[data-field-name="${o}"]`)
					.tagName && (t += ' xmlns:xfa="http://www.xfa.org/schema/xfa-data/1.0/" xfa:contentType="text/html"'), t += ">", n instanceof Array)
					for (let e of n) t += `<value>${e}</value>`;
				else t += n;
				t += `</${i}>`
			}
			return t += "</fields>", t
		}
	}), Object.defineProperty(Doc.prototype, "_submitFormAsXML", {
		value: function(e, t) {
			return fetch(e, {
				method: "POST"
				, body: this._buildXMLString(t)
				, headers: {
					"Content-type": "application/xml; charset=UTF-8"
				}
			})
		}
	}), Object.defineProperty(Doc.prototype, "_urlEncodeFormData", {
		value: function(e) {
			return Object.entries(e)
				.flatMap((([e, t]) => {
					const o = encodeURIComponent(e);
					return t instanceof Array ? t.map((e => `${o}=${encodeURIComponent(e)}`)) : t ? `${o}=${encodeURIComponent(t)}` : o
				}))
				.join("&")
		}
	}), Object.defineProperty(Doc.prototype, "_submitFormAsHTML", {
		value: function(e, t, o = !1) {
			const n = this._urlEncodeFormData(t);
			let i;
			return i = o ? fetch(`${e}?${n}`, {
				method: "GET"
			}) : fetch(e, {
				method: "POST"
				, body: n
				, headers: {
					"Content-type": "application/x-www-form-urlencoded; charset=UTF-8"
				}
			}), i
		}
	}), Object.defineProperty(Doc.prototype, "_submitFormAsPDF", {
		value: function(e, t = !1) {
			if (this._checkRequired()) return window.alert("At least one required field was empty on export. Please fill in required fields (highlighted) before continuing"), void 0;
			const o = document.getElementById("FDFXFA_Processing");
			let n;
			if (o && (o.style.display = "block"), e || (e = window.prompt("Please Enter URL to Submit Form;\n[ Please refer to FormVu documentation for defining submit URL ]")), t) n = fetch(e, {
				method: "POST"
				, body: `pdfdata=${EcmaFilter.encodeBase64(EcmaParser._insertFieldsToPDF(""))}`
				, headers: {
					"Content-type": "application/x-www-form-urlencoded; charset=UTF-8"
				}
			});
			else {
				const t = idrform.getCompletedFormPDF()
					, o = document.querySelector("#FDFXFA_PDFName")
						.textContent;
				n = fetch(e, {
					method: "POST"
					, body: t
					, headers: {
						"Content-type": "application/pdf; charset=UTF-8"
						, "Content-Disposition": `filename="${o}"`
					}
				})
			}
			return o && (o.style.display = "none"), n
		}
	}), Object.defineProperty(Doc.prototype, "submitForm", {
		value: function(e, t = !0, o = !1, n, i = !1, a = !1, r = !1, s = !1, c = !1, l = !1, d = !1, u = !1, h, f, m, p, g = !1, y = ["datasets", "xfdf"], S = "utf-8", O, E, I, A) {
			let D = {
				cURL: "string" == typeof e || e instanceof String ? e : void 0
				, bFDF: t
				, bEmpty: o
				, aFields: n
				, bGet: i
				, bAnnotations: a
				, bXML: r
				, bIncrChanges: s
				, bPDF: c
				, bCanonical: l
				, bExclNonUserAnnots: d
				, bExclFKey: u
				, cPassword: h
				, bEmbedForm: f
				, oJavaScript: m
				, cSubmitAs: p
				, bInclNMKey: g
				, aPackets: y
				, cCharset: S
				, oXML: O
				, cPermID: E
				, cInstID: I
				, cUsageRights: A
			}
				, {
					cURL: b
					, bFDF: v
					, bEmpty: T
					, aFields: F
					, bGet: R
					, bAnnotations: L
					, bXML: P
					, bIncrChanges: N
					, bPDF: C
					, bCanonical: k
					, bExclNonUserAnnots: B
					, bExclFKey: U
					, cPassword: x
					, bEmbedForm: w
					, oJavaScript: M
					, cSubmitAs: j
					, bInclNMKey: X
					, aPackets: K
					, cCharset: V
					, oXML: _
					, cPermID: Y
					, cInstID: W
					, cUsageRights: H
				} = e instanceof Object && !(e instanceof String) ? {
					...D
					, ...e
				} : D;
			if (app.isAcroForm) {
				let t;
				switch (j || (j = C ? "PDF" : P ? "XML" : v ? "FDF" : "HTML"), j) {
					case "HTML":
						void 0 !== F && (F = F instanceof Array ? F : [F]);
						const e = undefined;
						if (this._checkRequired(F)) return window.alert("At least one required field was empty on export. Please fill in required fields (highlighted) before continuing"), void 0;
						const o = this._getFormData(F, T, k);
						t = this._submitFormAsHTML(b, o, R);
						break;
					case "XML":
						t = this._submitFormAsXML(b, this._getFormData());
						break;
					case "PDF":
						t = this._submitFormAsPDF(b);
						break;
					default:
						return console.error("Submission Type %s is not supported by FormVu\nContact IDR SOLUTIONS if you require this functionality", j), void 0
				}
				t.then((e => {
					e.ok ? alert("Successfully sent form") : alert(`Error sending form: ${t.status}`)
				}))
					.catch((t => {
						alert(`Failed to send form to: ${e}\n${t}`)
					}))
			} else {
				var G = new PdfDocument
					, Q = new PdfPage;
				G.addPage(Q);
				var q = new PdfText(70, 70, "Helvetica", 20, "Please wait...");
				Q.addText(q), q = new PdfText(70, 110, "Helvetica", 11, "If this message is not eventually replaced by proper contents of the document, your PDF"), Q.addText(q), q = new PdfText(70, 124, "Helvetica", 11, "viewer may not be able to display this type of document."), Q.addText(q), q = new PdfText(70, 150, "Helvetica", 11, "You can upgrade to the latest version of Adobe Reader for Windows, Mac, or Linux by"), Q.addText(q), q = new PdfText(70, 164, "Helvetica", 11, "visiting http://www.adobe.com/go/reader_download."), Q.addText(q), q = new PdfText(70, 190, "Helvetica", 11, "For more assistance with Adobe Reader visit http://www.adobe.com/go/acrreader."), Q.addText(q), q = new PdfText(70, 220, "Helvetica", 7.5, "Windows is either a registered trademark or a trademark of Microsoft Corporation in the United States and/or other countries. Mac is a trademark "), Q.addText(q), q = new PdfText(70, 229, "Helvetica", 7.5, "of Apple Inc., registered in the United States and other countries. Linux is the registered trademark of Linus Torvalds in the U.S. and other "), Q.addText(q), q = new PdfText(70, 238, "Helvetica", 7.5, "countries."), Q.addText(q);
				var z = generateXDP()
					, J = G.createPdfString(z)
					, Z = EcmaPDF.encode64(J)
					, $ = document.createElement("form");
				$.setAttribute("method", "post"), e || (e = window.prompt("Please Enter URL to Submit Form; \n[ Please use 'pdfdata' as named parameter for accessing filled pdf as base64 ] \n[ Please refer to FormVu documentation for defining submit URL ]")), $.setAttribute("action", e), document.body.appendChild($);
				var ee = document.createElement("textarea");
				ee.setAttribute("name", "pdfdata"), ee.value = Z, $.appendChild(ee), $.submit()
			}
		}
	}), Object.defineProperty(Doc.prototype, "syncAnnotScan", {
		value: function() {
			console.log("syncAnnotScan method not defined contact - IDR SOLUTIONS")
		}
	});
	var color = {
		transparent: ["T"]
		, black: ["G", 0]
		, white: ["G", 1]
		, red: ["RGB", 1, 0, 0]
		, green: ["RGB", 0, 1, 0]
		, blue: ["RGB", 0, 0, 1]
		, cyan: ["CMYK", 1, 0, 0, 0]
		, magenta: ["CMYK", 0, 1, 0, 0]
		, yellow: ["CMYK", 0, 0, 1, 0]
		, dkGray: ["G", .25]
		, gray: ["G", .5]
		, ltGray: ["G", .75]
		, convert: function(e, t) {
			var o = e;
			switch (t) {
				case "G":
					"RGB" === e[0] ? o = new Array("G", .3 * e[1] + .59 * e[2] + .11 * e[3]) : "CMYK" === e[0] && (o = new Array("G", 1 - Math.min(1, .3 * e[1] + .59 * e[2] + .11 * e[3] + e[4])));
					break;
				case "RGB":
					"G" === e[0] ? o = new Array("RGB", e[1], e[1], e[1]) : "CMYK" === e[0] && (o = new Array("RGB", 1 - Math.min(1, e[1] + e[4]), 1 - Math.min(1, e[2] + e[4]), 1 - Math.min(1, e[3] + e[4])));
					break;
				case "CMYK":
					"G" === e[0] ? o = new Array("CMYK", 0, 0, 0, 1 - e[1]) : "RGB" === e[0] && (o = new Array("CMYK", 1 - e[1], 1 - e[2], 1 - e[3], 0));
					break
			}
			return o
		}
		, equal: function(e, t) {
			if ("G" === e[0] ? e = this.convert(e, t[0]) : t = this.convert(t, e[0]), e[0] !== t[0]) return !1;
			for (var o = e[0].length, n = 1; n <= o; n++)
				if (e[n] !== t[n]) return !1;
			return !0
		}
		, htmlColorToPDF: function(e) {
			e.hasOwnProperty("indexof") && -1 !== e.indexof("#") && (e = hexToRgbCss(e));
			var t = rgbCssToArr(e);
			return ["RGB", t[0] / 255, t[1] / 255, t[2] / 255]
		}
		, pdfColorToHTML: function(e) {
			var t = color.convert(e, "RGB");
			return rgbToHexCss(parseInt(255 * t[1]), parseInt(255 * t[2]), parseInt(255 * t[3]))
		}
	};

	function Field(e) {
		this.input = e, this.buttonAlignX = 0, this.buttonAlignY = 0, this.buttonFitBounds = !1, this.buttonPosition = 0, this.buttonScaleHoq = 0, this.buttonScaleWhen = 0, this.calcOrderIndex = 0, this.comb = !1, this.commitOnSelChange = !0, this.currentValueIndices = [], this.defaultStyle = {}, this.defaultValue = "", this.doNotScroll = !1, this.doNotSpellCheck = !1, this.delay = !1, this.doc = doc, this.exportValues = [], this.fileSelect = !1, this.highlight = "none", this.multiline = !1, this.multipleSelection = !1, this.page = 0, this.password = !1, this.print = !0, this.radiosInUnison = !1, this.rect = [], this.richText = !1, this.richValue = [], this.rotation = 0, this.style = "", this.submitName = "", this.textFont = null, this.userName = ""
	}

	function FDF() {
		this.deleteOption = 0, this.isSigned = !1, this.numEmbeddedFiles = 0
	}

	function FullScreen() { }
	Object.defineProperty(Field.prototype, "alignment", {
		get: function() {
			return this.input.style.textAlign ? this.input.style.textAlign : "left"
		}
		, set: function(e) {
			this.input.style.textAlign = e
		}
	}), Object.defineProperty(Field.prototype, "borderStyle", {
		get: function() {
			switch (this.input.style.borderStyle) {
				case "solid":
					return border.s;
				case "dashed":
					return border.d;
				case "beveled":
					return border.b;
				case "inset":
					return border.i;
				case "underline":
					return border.u
			}
			return "none"
		}
		, set: function(e) {
			this.input.style.borderStyle = e
		}
	}), Object.defineProperty(Field.prototype, "charLimit", {
		get: function() {
			return this.input.maxLength
		}
		, set: function(e) {
			this.input.maxLength = e
		}
	}), Object.defineProperty(Field.prototype, "display", {
		get: function() {
			return this.input && ("none" === this.input.style.display || this.input.classList.contains("idr-hidden")) ? display.hidden : display.visible
		}
		, set: function(e) {
			switch (this.input && (this.input.dataset.defaultDisplay = this.input.dataset.defaultDisplay ?? this.display), e) {
				case display.visible:
					this.input.classList.contains("idr-hidden") && this.input.classList.remove("idr-hidden"), this.input.style.display = "initial";
					break;
				case display.hidden:
				case display.noView:
					this.input.style.display = "none";
					break
			}
		}
	}), Object.defineProperty(Field.prototype, "editable", {
		get: function() {
			return this.input.disabled
		}
		, set: function(e) {
			this.input.style.disabled = e
		}
	}), Object.defineProperty(Field.prototype, "fillColor", {
		get: function() {
			if (!this.input) return "";
			var e = window.getComputedStyle(this.input);
			return color.htmlColorToPDF(e.backgroundColor)
		}
		, set: function(e) {
			this.input.style.backgroundColor = color.pdfColorToHTML(e)
		}
	}), Object.defineProperty(Field.prototype, "hidden", {
		get: function() {
			return this.input.hidden
		}
		, set: function(e) {
			this.input.hidden = e
		}
	}), Object.defineProperty(Field.prototype, "lineWidth", {
		get: function() {
			return parseInt(this.style.borderWidth)
		}
		, set: function(e) {
			this.input.style.borderWidth = e + "px"
		}
	}), Object.defineProperty(Field.prototype, "name", {
		get: function() {
			return this.input.getAttribute("data-field-name")
		}
		, set: function(e) {
			this.input.setAttribute("data-field-name", e)
		}
	}), Object.defineProperty(Field.prototype, "numItems", {
		get: function() {
			return this.input.options.length
		}
	}), Object.defineProperty(Field.prototype, "readOnly", {
		get: function() {
			return this.input.readOnly
		}
		, set: function(e) {
			this.input.readOnly = e
		}
	}), Object.defineProperty(Field.prototype, "required", {
		get: function() {
			return this.input.required
		}
		, set: function(e) {
			this.input.required = e
		}
	}), Object.defineProperty(Field.prototype, "textSize", {
		get: function() {
			return parseInt(this.input.style.fontSize)
		}
		, set: function(e) {
			this.input.style.fontSize = parseInt(e) + "px"
		}
	}), Object.defineProperty(Field.prototype, "strokeColor", {
		get: function() {
			return color.htmlColorToPDF(this.input.style.borderColor)
		}
		, set: function(e) {
			this.input.style.borderColor = color.pdfColorToHTML(e)
		}
	}), Object.defineProperty(Field.prototype, "textColor", {
		get: function() {
			return color.htmlColorToPDF(this.input.style.color)
		}
		, set: function(e) {
			this.input.style.color = color.pdfColorToHTML(e)
		}
	}), Object.defineProperty(Field.prototype, "type", {
		get: function() {
			var e = this.input.tagName;
			return "INPUT" === e ? this.getAttribute("type") : "SELECT" === e ? "listbox" : "BUTTON" === e ? "button" : "text"
		}
	}), Object.defineProperty(Field.prototype, "value", {
		get: function() {
			if (this.input) {
				var e = this.input.value
					, t;
				switch (this.input.getAttribute("type")) {
					case "checkbox":
						if (!this.input.checked) return !1;
						break;
					case "radio":
						if (null != e) return EcmaFormField.hexDecodeName(e);
						break;
					case "text":
						if ("" === e) return e;
						break
				}
				return isNaN(e) ? e : 1 * e
			}
		}
		, set: function(e) {
			"radio" == this.input.getAttribute("type") ? this.input.value = EcmaFormField.hexEncodeName(e) : this.input.value = e
		}
	}), Object.defineProperty(Field.prototype, "valueAsString", {
		get: function() {
			return "" + this.input.value
		}
		, set: function(e) {
			this.input.value = "" + e
		}
	}), Object.defineProperty(Field.prototype, "browseForFileToSubmit", {
		value: function() {
			console.log("browseForFileToSubmit is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "buttonGetCaption", {
		value: function() {
			return this.input.innerHTML
		}
	}), Object.defineProperty(Field.prototype, "buttonGetIcon", {
		value: function() {
			console.log("buttonGetIcon is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "buttonImportIcon", {
		value: function() {
			console.log("buttonImportIcon is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "buttonSetCaption", {
		value: function(e) {
			this.input.innerHTML = e
		}
	}), Object.defineProperty(Field.prototype, "buttonSetIcon", {
		value: function() {
			console.log("buttonSetIcon is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "checkThisBox", {
		value: function(e, t) {
			this.input.checked = !0
		}
	}), Object.defineProperty(Field.prototype, "clearItems", {
		value: function() {
			var e, t;
			for (e = this.input.options.length - 1; e >= 0; e--) this.input.remove(e)
		}
	}), Object.defineProperty(Field.prototype, "defaultsChecked", {
		value: function() {
			return this.input.checked
		}
	}), Object.defineProperty(Field.prototype, "deleteItemAt", {
		value: function(e) {
			if (-1 === e) {
				var t = this.input.options.length - 1;
				this.input.remove(t)
			} else this.input.remove(e)
		}
	}), Object.defineProperty(Field.prototype, "getArray", {
		value: function() {
			console.log("getArray is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "getItemAt", {
		value: function(e, t) {
			return this.input.options[e]
		}
	}), Object.defineProperty(Field.prototype, "getLock", {
		value: function() {
			console.log("getLock is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "insertItemAt", {
		value: function(e, t, o) {
			var n = document.createElement("option");
			n.text = e, this.input.add(n, o)
		}
	}), Object.defineProperty(Field.prototype, "isBoxChecked", {
		value: function(e) {
			return this.input.checked
		}
	}), Object.defineProperty(Field.prototype, "isDefaultChecked", {
		value: function(e) {
			console.log("isDefaultChecked is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "setAction", {
		value: function(e, t) {
			switch (e) {
				case "MouseUp":
					this.input.addEventListener("mouseup", new Function(t));
					break;
				case "MouseDown":
					this.input.addEventListener("mousedown", new Function(t));
					break;
				case "MouseEnter":
					this.input.addEventListener("mouseenter", new Function(t));
					break;
				case "MouseExit":
					this.input.addEventListener("mouseexit", new Function(t));
					break;
				case "OnFocus":
					this.input.addEventListener("focus", new Function(t));
					break;
				case "OnBlur":
					this.input.addEventListener("blur", new Function(t));
					break;
				case "Keystroke":
					this.input.addEventListener("keydown", new Function(t));
					break;
				case "Validate":
					break;
				case "Calculate":
					break;
				case "Format":
					break
			}
		}
	}), Object.defineProperty(Field.prototype, "setFocus", {
		value: function() {
			this.input.focus()
		}
	}), Object.defineProperty(Field.prototype, "setItems", {
		value: function(e) {
			for (var t = 0; t < e.length; t++) {
				var o = document.createElement("option");
				o.text = e[t], this.input.add(o)
			}
		}
	}), Object.defineProperty(Field.prototype, "setLock", {
		value: function(e) {
			console.log("setLock is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "signatureGetModifications", {
		value: function() {
			console.log("signatureGetModifications is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "signatureGetSeedValue", {
		value: function() {
			console.log("signatureGetSeedValue is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "signatureInfo", {
		value: function() {
			console.log("signatureInfo is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "signauteSetSeedValue", {
		value: function() {
			console.log("signauteSetSeedValue is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "signatureSign", {
		value: function() {
			console.log("signatureSign is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(Field.prototype, "signatureValidate", {
		value: function() {
			console.log("signatureValidate is method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "addContact", {
		value: function(e) {
			console.log("addContact method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "addEmbeddedFile", {
		value: function(e, t) {
			console.log("addEmbeddedFile method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "addRequest", {
		value: function(e, t, o) {
			console.log("addRequest method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "close", {
		value: function() {
			console.log("close method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "mail", {
		value: function() {
			console.log("mail method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "save", {
		value: function() {
			console.log("save method not defined contact - IDR SOLUTIONS")
		}
	}), Object.defineProperty(FDF.prototype, "signatureClear", {
		value: function() {
			return console.log("signatureClear method not defined contact - IDR SOLUTIONS"), !1
		}
	}), Object.defineProperty(FDF.prototype, "signatureSign", {
		value: function() {
			return console.log("signatureSign method not defined contact - IDR SOLUTIONS"), !1
		}
	}), Object.defineProperty(FDF.prototype, "signatureValidate", {
		value: function(e, t) {
			return console.log("signatureSign method not defined contact - IDR SOLUTIONS"), {}
		}
	}), Object.defineProperty(FullScreen.prototype, "isFullscreen", {
		get: function() {
			return this.isinFullscreen
		}
		, set: function(e) {
			var t, o;
			e ? (document.body.requestFullscreen || document.body.msRequestFullscreen || document.body.mozRequestFullScreen || document.body.webkitRequestFullscreen)
				.call(document.body) : (document.exitFullscreen || document.msExitFullscreen || document.mozCancelFullScreen || document.webkitCancelFullScreen)
					.call(document)
		}
		, configurable: !0
		, enumerable: !0
	}), Object.defineProperty(FullScreen.prototype, "isFullscreenEnabled", {
		value: function() {
			return document.fullscreenEnabled || document.msFullscreenEnabled || document.mozFullScreenEnabled || document.webkitFullscreenEnabled
		}
	}), Object.defineProperty(FullScreen.prototype, "isinFullscreen", {
		value: function() {
			return !!(document.fullscreenElement || document.msFullscreenElement || document.mozFullScreenElement || document.webkitFullscreenElement)
		}
	}), Object.defineProperty(FullScreen.prototype, "toggleFullscreen", {
		value: function() {
			var e, t;
			this.isinFullscreen() ? (document.exitFullscreen || document.msExitFullscreen || document.mozCancelFullScreen || document.webkitCancelFullScreen)
				.call(document) : (document.body.requestFullscreen || document.body.msRequestFullscreen || document.body.mozRequestFullScreen || document.body.webkitRequestFullscreen)
					.call(document.body)
		}
	});
	var ADBC = {
		SQLT_BIGINT: 0
		, SQLT_BINARY: 1
		, SQLT_BIT: 2
		, SQLT_CHAR: 3
		, SQLT_DATE: 4
		, SQLT_DECIMAL: 5
		, SQLT_DOUBLE: 6
		, SQLT_FLOAT: 7
		, SQLT_INTEGER: 8
		, SQLT_LONGVARBINARY: 9
		, SQLT_LONGVARCHAR: 10
		, SQLT_NUMERIC: 11
		, SQLT_REAL: 12
		, SQLT_SMALLINT: 13
		, SQLT_TIME: 14
		, SQLT_TIMESTAMP: 15
		, SQLT_TINYINT: 16
		, SQLT_VARBINARY: 17
		, SQLT_VARCHAR: 18
		, SQLT_NCHAR: 19
		, SQLT_NVARCHAR: 20
		, SQLT_NTEXT: 21
		, Numeric: 0
		, String: 1
		, Binary: 2
		, Boolean: 3
		, Time: 4
		, Date: 5
		, TimeStamp: 6
		, getDataSourceList: function() {
			return console.log("ADBC.getDataSourceList() method not defined contact - IDR SOLUTIONS"), new Array
		}
		, newConnnection: function() {
			var e = {};
			if (arguments[0] instanceof Object) e = arguments[0];
			else switch (e.cDSN = arguments[0], arguments.length) {
				case 2:
					e.cUID = arguments[1];
					break;
				case 3:
					e.cUID = arguments[1], e.cPWD = arguments[2];
					break
			}
			return console.log("ADBC.newConnection method not defined contact - IDR SOLUTIONS"), null
		}
	};

	function Alerter() {
		this.dispathc = function() {
			console.log("dispatch method not defined contact - IDR SOLUTIONS")
		}
	}

	function Alert() {
		this.type = "", this.doc = null, this.fromUser = !1, this.error = {
			message: ""
		}, this.errorText = "", this.fileName = "", this.selection = null
	}

	function AlternatePresentation() {
		this.active = !1, this.type = "", this.start = function() {
			console.log("start method not defined contact - IDR SOLUTIONS")
		}, this.stop = function() {
			console.log("stop method not defined contact - IDR SOLUTIONS")
		}
	}

	function Annotation() {
		this.type = "Text", this.rect = [], this.page = 0, this.alignment = 0, this.AP = "Approved", this.arrowBegin = "None", this.arrowEnd = "None", this.attachIcon = "PushPin", this.author = "", this.borderEffectIntensity = "", this.callout = [], this.caretSymbol = "", this.contents = "", this.creationDate = new Date, this.dash = [], this.delay = !1, this.doc = null, this.doCaption = !1, this.fillColor = [], this.gestures = [], this.hidden = !1, this.inReplyTo = "", this.intent = "", this.leaderExtend = 0, this.leaderLength = 0, this.lineEnding = "none", this.lock = !1, this.modDate = new Date, this.name = "", this.noteIcon = "Note", this.noView = !1, this.opacity = 1, this.open = !1, this.point = [], this.points = [], this.popupOpen = !0, this.popupRect = [], this.print = !1, this.quads = [], this.readOnly = !1, this.refType = "", this.richContents = [], this.richDefaults = null, this.rotate = 0, this.seqNum = 0, this.soundIcon = "", this.state = "", this.stateModel = "", this.strokeColor = [], this.style = "", this.subject = "", this.textFont = "", this.textSize = 10, this.toggleNoView = !1, this.vertices = null, this.width = 1, this.URI = "", this.GoTo = ""
	}

	function Bookmark() {
		this.children = null, this.color = ["RGB", 1, 0, 0], this.name = "", this.open = !0, this.parent = null, this.style = 0, this.createChild = function(e, t, o) {
			console.log("createChild method not defined contact - IDR SOLUTIONS")
		}, this.execute = function() {
			console.log("execute method not defined contact - IDR SOLUTIONS")
		}, this.insertChild = function(e, t) {
			console.log("insertChild method not defined contact - IDR SOLUTIONS")
		}, this.remove = function() {
			console.log("remove method not defined contact - IDR SOLUTIONS")
		}, this.setAction = function(e) {
			console.log("setAction method not defined contact - IDR SOLUTIONS")
		}
	}

	function Catalog() {
		this.isIdle = !1, this.jobs = new Array, this.getIndex = function(e) {
			console.log("getIndex method not defined contact - IDR SOLUTIONS")
		}, this.remove = function(e) {
			console.log("remove method not defined contact - IDR SOLUTIONS")
		}
	}

	function CatalogJob() {
		this.path = "", this.type = "", this.status = ""
	}

	function Certificate() {
		this.binary = "", this.issuerDN = {}, this.keyUsage = new Array, this.MD5Hash = "", this.privateKeyValidityEnd = {}, this.privateKeyValidityStart = {}, this.SHA1Hash = "", this.serialNumber = "", this.subjectCN = "", this.subjectDN = "", this.ubRights = {}, this.usage = {}, this.validityEnd = {}, this.validityStart = {}
	}

	function Rights() {
		this.mode = "", this.rights = new Array
	}

	function Usage() {
		this.endUserSigning = !1, this.endUserEncryption = !1
	}
	Object.defineProperty(Annotation.prototype, "getDictionaryString", {
		value: function() {
			for (var e = "<</Type /Annot /Subtype /" + this.type + " /Rect [ ", t = 0, o = this.rect.length; t < o; t++) e += this.rect[t] + " ";
			if (e += "]", this.type === AnnotationType.Highlight) {
				e += "/QuadPoints [ ";
				for (var t = 0, o = this.quads.length; t < o; t++) e += this.quads[t] + " ";
				e += "]"
			} else if (this.type === AnnotationType.Text) this.contents.length > 0 && (e += "/Contents (" + this.contents + ")"), this.open && (e += "/Open true");
			else if (this.type === AnnotationType.Link) {
				if (this.URI.length > 0 ? e += "/A <</Type /Action /S /URI /URI (" + this.URI + ")>>" : this.GoTo.length > 0 && (e += "/Dest [" + this.GoTo + " /Fit]>>"), this.quads.length > 0) {
					e += "/QuadPoints [ ";
					for (var t = 0, o = this.quads.length; t < o; t++) e += this.quads[t] + " ";
					e += "]"
				}
			} else if (this.type === AnnotationType.Line) e += "/L [" + this.points[0] + " " + this.points[1] + " " + this.points[2] + " " + this.points[3] + "]";
			else if (this.type === AnnotationType.Polygon || this.type === AnnotationType.PolyLine) {
				e += "/Vertices [";
				for (var t = 0, o = this.vertices.length; t < o; t++) e += this.vertices[t] + " ";
				e += "]"
			} else if (this.type === AnnotationType.Ink) {
				e += "/InkList [";
				for (var n = this.gestures, t = 0, o = n.length; t < o; t++) {
					var i = n[t];
					e += " [";
					for (var a = 0, r = i.length; a < r; a++) e += i[a] + " ";
					e += "] "
				}
				e += "]"
			} else if (this.type === AnnotationType.FreeText) {
				for (var s = "", t = 0, o = this.richContents.length; t < o; t++) {
					var c = this.richContents[t];
					s += "<span style='font-size:" + c.textSize + ";color:" + c.textColor + "'>" + c.text + "</span>"
				}
				e += "/DA (/Arial " + this.textSize + " Tf)/RC (<body><p>" + s + "</p></body>)"
			}
			if (this.type === AnnotationType.Line || this.type === AnnotationType.Highlight || this.type === AnnotationType.FreeText || this.type === AnnotationType.Link || this.type === AnnotationType.Square || this.type === AnnotationType.Circle || this.type === AnnotationType.Polygon || this.type === AnnotationType.PolyLine || this.type === AnnotationType.Ink) {
				if (this.opacity < 1 && (e += "/CA " + this.opacity), 1 !== this.width && (e += "/BS <</Type /Border /W " + this.width + ">>"), this.fillColor.length > 0) {
					e += "/IC [";
					for (var t = 0, o = this.fillColor.length; t < o; t++) e += this.fillColor[t] + " ";
					e += "]"
				}
				if (this.strokeColor.length > 0) {
					e += "/C [";
					for (var t = 0, o = this.strokeColor.length; t < o; t++) e += this.strokeColor[t] + " ";
					e += "]"
				}
			}
			return e += ">>"
		}
	}), Object.defineProperty(Annotation.prototype, "destroy", {
		value: function() {
			return console.log("destroy method not defined contact - IDR SOLUTIONS"), !0
		}
	}), Object.defineProperty(Annotation.prototype, "getProps", {
		value: function() {
			return console.log("getProps method not defined contact - IDR SOLUTIONS"), !0
		}
	}), Object.defineProperty(Annotation.prototype, "getStateInModel", {
		value: function() {
			return console.log("getStateInModel method not defined contact - IDR SOLUTIONS"), !0
		}
	}), Object.defineProperty(Annotation.prototype, "setProps", {
		value: function(e) {
			for (var t in e) t in this && (this[t] = e[t]);
			return !0
		}
	}), Object.defineProperty(Annotation.prototype, "transitionToState", {
		value: function(e) {
			console.log("transitionToState method not defined contact - IDR SOLUTIONS")
		}
	});
	var Collab = {
		addStateModel: function(e, t, o, n, i, a) {
			console.log("addStateModel not implemented")
		}
		, documentToStream: function(e) {
			console.log("documentToStream not implemented")
		}
		, removeStateModel: function(e) {
			console.log("removeStateModel not implemented")
		}
	};

	function States() {
		this.cUIName = "", this.oIcon = {}
	}

	function ColorConvertAction() {
		this.action = {}, this.alias = "", this.colorantName = "", this.convertIntent = 0, this.convertProfile = "", this.embed = !1, this.isProcessColor = !1, this.matchAttributesAll = {}, this.matchAttributesAny = 0, this.matchIntent = {}, this.matchSpaceTypeAll = {}, this.matchSpaceTypeAny = 0, this.preserveBlack = !1, this.useBlackPointCompensation = !1
	}

	function Column() {
		this.columnNum - 0, this.name = "", this.type = 0, this.typeName = "", this.value = ""
	}

	function ColumnInfo() {
		this.name = "", this.description = "", this.type = "", this.typeName = ""
	}

	function Connection() {
		this.close = function() {
			console.log("close method not defined contact - IDR SOLUTIONS")
		}, this.getColumnList = function(e) {
			console.log("getColumnList method not defined contact - IDR SOLUTIONS")
		}, this.getTableList = function() {
			console.log("getTableList method not defined contact - IDR SOLUTIONS")
		}, this.newStatement = function() {
			console.log("newStatement method not defined contact - IDR SOLUTIONS")
		}
	}

	function Data() {
		this.creationDate = {}, this.description = "", this.MIMEType = "", this.modDate = {}, this.name = "", this.path = "", this.size = 0
	}

	function DataSourceInfo() {
		this.name = "", this.description = ""
	}

	function dbg() {
		this.bps = new Array, this.c = new function() {
			console.log("c method not defined contact - IDR SOLUTIONS")
		}, this.cb = function(e, t) {
			console.log("cb method not defined contact - IDR SOLUTIONS")
		}, this.q = function() {
			console.log("q method not defined contact - IDR SOLUTIONS")
		}, this.sb = function(e, t, o) {
			console.log("sb method not defined contact - IDR SOLUTIONS")
		}, this.si = function() {
			console.log("si method not defined contact - IDR SOLUTIONS")
		}, this.sn = function() {
			console.log("sn method not defined contact - IDR SOLUTIONS")
		}, this.so = function() {
			console.log("so method not defined contact - IDR SOLUTIONS")
		}, this.sv = function() {
			console.log("sv method not defined contact - IDR SOLUTIONS")
		}
	}

	function Dialog() {
		this.enable = function(e) {
			console.log("enable method not defined contact - IDR SOLUTIONS")
		}, this.end = function(e) {
			console.log("end method not defined contact - IDR SOLUTIONS")
		}, this.load = function(e) {
			console.log("load method not defined contact - IDR SOLUTIONS")
		}, this.store = function(e) {
			console.log("store method not defined contact - IDR SOLUTIONS")
		}
	}

	function DirConnection() {
		this.canList = !1, this.canDoCustomSearch = !1, this.canDoCustomUISearch = !1, this.canDoStandardSearch = !1, this.groups = new Array, this.name = "", this.uiName = ""
	}

	function Directory() {
		this.info = {}, this.connect = function(e, t) {
			return console.log("connect method not defined contact - IDR SOLUTIONS"), null
		}
	}

	function DirectoryInformation() {
		this.dirStdEntryID = "", this.dirStdEntryName = "", this.dirStdEntryPrefDirHandlerID = "", this.dirStdEntryDirType = "", this.dirStdEntryDirVersion = "", this.server = "", this.port = 0, this.searchBase = "", this.maxNumEntries = 0, this.timeout = 0
	}

	function Icon() {
		this.name = ""
	}

	function IconStream() {
		this.width = 0, this.height = 0
	}

	function Identity() {
		this.corporation = "", this.email = "", this.loginName = "", this.name = ""
	}

	function Index() {
		this.available = !1, this.name = "", this.path = "", this.selected = !1, this.build = function() {
			console.log("build is method not defined contact - IDR SOLUTIONS")
		}, this.parameters = function() {
			console.log("parameters is method not defined contact - IDR SOLUTIONS")
		}
	}

	function Link(e) {
		this.ele = e, this.borderColor = [], this.borderWidth = 0, this.highlightMode = "invert", this.rect = [], this.setAction = function() {
			console.log("setAction is method not defined contact - IDR SOLUTIONS")
		}
	}

	function Marker() {
		this.frame = 0, this.index = 0, this.name = "", this.time = 0
	}

	function Markers() {
		this.player = {}, this.get = function(e) {
			console.log("get is method not defined contact - IDR SOLUTIONS")
		}
	}

	function Media() {
		this.align = {
			topLeft: 0
			, topCenter: 1
			, topRight: 2
			, centerLeft: 3
			, center: 4
			, centerRight: 5
			, bottomLeft: 6
			, bottomCenter: 7
			, bottomRight: 8
		}, this.canResize = {
			no: 0
			, keepRatio: 1
			, yes: 2
		}, this.closeReason = {
			general: 0
			, error: 1
			, done: 2
			, stop: 3
			, play: 4
			, uiGeneral: 5
			, uiScreen: 6
			, uiEdit: 7
			, docClose: 8
			, docSave: 9
			, docChange: 10
		}, this.defaultVisible = !0, this.ifOffScreen = {
			allow: 0
			, forseOnScreen: 1
			, cancel: 2
		}, this.layout = {
			meet: 0
			, slice: 1
			, fill: 2
			, scroll: 3
			, hidden: 4
			, standard: 5
		}, this.monitorType = {
			document: 0
			, nonDocument: 1
			, primary: 3
			, bestColor: 4
			, largest: 5
			, tallest: 6
			, widest: 7
		}, this.openCode = {
			success: 0
			, failGeneral: 1
			, failSecurityWindow: 2
			, failPlayerMixed: 3
			, failPlayerSecurityPrompt: 4
			, failPlayerNotFound: 5
			, failPlayerMimeType: 6
			, failPlayerSecurity: 7
			, failPlayerData: 8
		}, this.over = {
			pageWindow: 0
			, appWindow: 1
			, desktop: 2
			, monitor: 3
		}, this.pageEventNames = {
			Open: 0
			, Close: 1
			, InView: 2
			, OutView: 3
		}, this.raiseCode = {
			fileNotFound: 0
			, fileOpenFailed: 1
		}, this.raiseSystem = {
			fileError: 0
		}, this.renditionType = {
			unknown: 0
			, media: 1
			, selector: 2
		}, this.status = {
			clear: 0
			, message: 1
			, contacting: 2
			, buffering: 3
			, init: 4
			, seeking: 5
		}, this.trace = !1, this.version = 7, this.windowType = {
			docked: 0
			, floating: 1
			, fullScreen: 2
		}, this.addStockEvents = function(e, t) {
			console.log("addStockEvents method not defined contact - IDR SOLUTIONS")
		}, this.alertFileNotFound = function(e, t, o) {
			console.log("alertFileNotFound method not defined contact - IDR SOLUTIONS")
		}, this.alertSelectFailed = function(e, t, o, n) {
			console.log("alertFileNotFound method not defined contact - IDR SOLUTIONS")
		}, this.argsDWIM = function(e) {
			console.log("argsDWIM method not defined contact - IDR SOLUTIONS")
		}, this.canPlayOrAlert = function(e) {
			console.log("canPlayOrAlert method not defined contact - IDR SOLUTIONS")
		}, this.computeFloatWinRect = function(e, t, o, n) {
			console.log("computeFloatWinRect method not defined contact - IDR SOLUTIONS")
		}, this.constrainRectToScreen = function(e, t) {
			console.log("constrainRectToScreen method not defined contact - IDR SOLUTIONS")
		}, this.createPlayer = function(e) {
			console.log("createPlayer method not defined contact - IDR SOLUTIONS")
		}, this.getAltTextData = function(e) {
			console.log("getAltTextData method not defined contact - IDR SOLUTIONS")
		}, this.getAltTextSettings = function() {
			console.log("getAltTextSettings method not defined contact - IDR SOLUTIONS")
		}, this.getAnnotStockEvents = function() {
			console.log("getAnnotStockEvents method not defined contact - IDR SOLUTIONS")
		}, this.getAnnotTraceEvents = function() {
			console.log("getAnnotTraceEvents method not defined contact - IDR SOLUTIONS")
		}, this.getPlayers = function() {
			console.log("getPlayers method not defined contact - IDR SOLUTIONS")
		}, this.getPlayerStockEvents = function() {
			console.log("getPlayerStockEvents method not defined contact - IDR SOLUTIONS")
		}, this.getPlayerTraceEvents = function() {
			console.log("getPlayerTraceEvents method not defined contact - IDR SOLUTIONS")
		}, this.getRenditionSettings = function() {
			console.log("getRenditionSettings method not defined contact - IDR SOLUTIONS")
		}, this.getURLData = function() {
			console.log("getURLData method not defined contact - IDR SOLUTIONS")
		}, this.getURLSettings = function() {
			console.log("getURLSettings method not defined contact - IDR SOLUTIONS")
		}, this.getWindowBorderSize = function() {
			console.log("getWindowBorderSize method not defined contact - IDR SOLUTIONS")
		}, this.openPlayer = function() {
			console.log("openPlayer method not defined contact - IDR SOLUTIONS")
		}, this.removeStockEvents = function() {
			console.log("removeStockEvents method not defined contact - IDR SOLUTIONS")
		}, this.startPlayer = function() {
			console.log("startPlayer method not defined contact - IDR SOLUTIONS")
		}
	}

	function MediaOffset() {
		this.frame = 0, this.marker = "", this.time = 0
	}

	function MediaPlayer() {
		this.annot = {}, this.defaultSize = {
			width: 0
			, height: 0
		}, this.doc = {}, this.events = {}, this.hasFocus = !1, this.id = 0, this.innerRect = [], this.isOpen = !1, this.isPlaying = !1, this.outerRect = [], this.page = 0, this.settings = {}, this.uiSize = [], this.visible = !1, this.close = function() {
			console.log("close is not implemented")
		}, this.open = function() {
			console.log("open is not implemented")
		}, this.pause = function() {
			console.log("pause is not implemented")
		}, this.play = function() {
			console.log("play is not implemented")
		}, this.seek = function() {
			console.log("seek is not implemented")
		}, this.setFocus = function() {
			console.log("setFocus is not implemented")
		}, this.stop = function() {
			console.log("stop is not implemented")
		}, this.triggerGetRect = function() {
			console.log("triggerGetRect is not implemented")
		}
	}

	function MediaReject() {
		this.rendition = {}
	}

	function MediaSelection() {
		this.selectContext = {}, this.players = [], this.rejects = [], this.rendtion = {}
	}

	function MediaSettings() {
		this.autoPlay = !1, this.baseURL = "", this.bgColor = [], this.bgOpacity = 1, this.data = {}, this.duration = 0, this.endAt = 0, this.floating = {}, this.layout = 0, this.monitor = {}, this.monitorType = 0, this.page = 0, this.palindrome = !1, this.players = {}, this.rate = 0, this.repeat = 0, this.showUI = !1, this.startAt = {}, this.visible = !1, this.volume = 0, this.windowType = 0
	}

	function Monitor() {
		this.colorDepth = 0, this.isPrimary = !1, this.rect = [], this.workRect = []
	}

	function Monitors() {
		this.bestColor = function() {
			console.log("bestColor is not implemented")
		}, this.bestFit = function() {
			console.log("bestFit is not implemented")
		}, this.desktop = function() {
			console.log("desktop is not implemented")
		}, this.document = function() {
			console.log("document is not implemented")
		}, this.filter = function() {
			console.log("filter is not implemented")
		}, this.largest = function() {
			console.log("largest is not implemented")
		}, this.leastOverlap = function() {
			console.log("leastOverlap is not implemented")
		}, this.mostOverlap = function() {
			console.log("mostOverlap is not implemented")
		}, this.nonDocument = function() {
			console.log("nonDocument is not implemented")
		}, this.primary = function() {
			console.log("primary is not implemented")
		}, this.secondary = function() {
			console.log("secondary is not implemented")
		}, this.select = function() {
			console.log("select is not implemented")
		}, this.tallest = function() {
			console.log("tallest is not implemented")
		}, this.widest = function() {
			console.log("widest is not implemented")
		}
	}

	function Net() {
		this.SOAP = {}, this.Discovery = {}, this.HTTP = {}, this.streamDecode = function() {
			console.log("streamDecode is not implemented")
		}, this.streamDigest = function() {
			console.log("streamDigest is not implemented")
		}, this.streamEncode = function() {
			console.log("streamEncode is not implemented")
		}
	}

	function OCG() {
		this.constants = {}, this.initState = !1, this.locked = !1, this.name = "", this.state = !1, this.getIntent = function() {
			console.log("getIntent is not implemented")
		}, this.setAction = function() {
			console.log("setAction is not implemented")
		}, this.setIntent = function() {
			console.log("setIntent is not implemented")
		}
	}

	function PlayerInfo() {
		this.id = "", this.mimeTypes = [], this.name = "", this.version = "", this.canPlay = function() {
			console.log("canPlay is not implemented")
		}, this.canUseData = function() {
			console.log("canUseData is not implemented")
		}, this.honors = function() {
			console.log("honors is not implemented")
		}
	}

	function PlayerInfoList() {
		this.select = function() {
			console.log("select is not implemented")
		}
	}

	function Plugin() {
		this.certified = !1, this.loaded = !1, this.name = "", this.path = "", this.version = 0
	}

	function Booklet() {
		this.binding = 0, this.duplexMode = 0, this.subsetForm = 0, this.subsetTo = 0
	}

	function PrintParams() {
		this.binaryOK = !0, this.bitmapDPI = 0, this.booklet = new Booklet, this.colorOverride = 0, this.colorProfile = "", this.constants = {}, this.downloadFarEastFonts = !1, this.fileName = "", this.firstPage = 0, this.flags = 0, this.fontPolicy = 0, this.gradientDPI = 0, this.interactive = 0, this.lastPage = 0, this.nUpAutoRotate = !1, this.nUpNumPagesH = 0, this.nUpNumPagesV = 0, this.nUpPageBorder = !1, this.nUpPageOrder = 0, this.pageHandling = 0, this.pageSubset = 0, this.printAsImage = !1, this.printContent = 0, this.printName = "", this.psLevel = 0, this.rasterFlags = 0, this.reversePages = !1, this.tileLabel = !1, this.tileMark = 0, this.tileOverlap = 0, this.tileScale = 0, this.transparencyLevel = 0, this.usePrinterCRD = 0, this.useT1Conversion = 0
	}

	function Span() {
		this.alignement = 0, this.fontFamily = ["serif", "sans-serif", "monospace"], this.fontStretch = "normal", this.fontStyle = "normal", this.fontWeight = 400, this.strikeThrough = !1, this.subscript = !1, this.superscript = !1, this.text = "", this.textColor = color.black, this.textSize = 12, this.underline = !1
	}

	function Thermometer() {
		this.cancelled = !1, this.duration = 0, this.text = "", this.value = 0, this.begin = function() {
			console.log("begin method not defined contact - IDR SOLUTIONS")
		}, this.end = function() {
			console.log("end method not defined contact - IDR SOLUTIONS")
		}
	}
	var util = {
		crackURL: function(e) {
			return console.log("crackURL method not defined contact - IDR SOLUTIONS"), {}
		}
		, iconStreamFromIcon: function() {
			return console.log("iconStreamFromIcon method not defined contact - IDR SOLUTIONS"), {}
		}
		, printd: function(e, t, o) {
			var n = ["JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"]
				, i = ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"];
			switch (e) {
				case 0:
					return this.printd("D:yyyymmddHHMMss", t);
				case 1:
					return this.printd("yyyy.mm.dd HH:MM:ss", t);
				case 2:
					return this.printd("m/d/yy h:MM:ss tt", t)
			}
			var a = {
				year: t.getFullYear()
				, month: t.getMonth()
				, day: t.getDate()
				, dayOfWeek: t.getDay()
				, hours: t.getHours()
				, minutes: t.getMinutes()
				, seconds: t.getSeconds()
			}
				, r = /(mmmm|mmm|mm|m|dddd|ddd|dd|d|yyyy|yy|HH|H|hh|h|MM|M|ss|s|tt|t|\\.)/g;
			return e.replace(r, (function(e, t) {
				switch (t) {
					case "mmmm":
						return n[a.month];
					case "mmm":
						return n[a.month].substring(0, 3);
					case "mm":
						return (a.month + 1)
							.toString()
							.padStart(2, "0");
					case "m":
						return (a.month + 1)
							.toString();
					case "dddd":
						return i[a.dayOfWeek];
					case "ddd":
						return i[a.dayOfWeek].substring(0, 3);
					case "dd":
						return a.day.toString()
							.padStart(2, "0");
					case "d":
						return a.day.toString();
					case "yyyy":
						return a.year.toString();
					case "yy":
						return (a.year % 100)
							.toString()
							.padStart(2, "0");
					case "HH":
						return a.hours.toString()
							.padStart(2, "0");
					case "H":
						return a.hours.toString();
					case "hh":
						return (1 + (a.hours + 11) % 12)
							.toString()
							.padStart(2, "0");
					case "h":
						return (1 + (a.hours + 11) % 12)
							.toString();
					case "MM":
						return a.minutes.toString()
							.padStart(2, "0");
					case "M":
						return a.minutes.toString();
					case "ss":
						return a.seconds.toString()
							.padStart(2, "0");
					case "s":
						return a.seconds.toString();
					case "tt":
						return a.hours < 12 ? "am" : "pm";
					case "t":
						return a.hours < 12 ? "a" : "p"
				}
				return t.charCodeAt(1)
			}))
		}
		, printf: function(e, arguments) {
			var t = e.indexOf("%");
			if (-1 === t) return e + " " + arguments;
			var o = e[t + 1]
				, n = e.indexOf(".")
				, i = e[n + 1]
				, a = arguments.toFixed(i);
			return t > 0 && (a = e.substring(0, t) + a), a
		}
		, printx: function(e, t) {
			var o = [e => e, e => e.toUpperCase(), e => e.toLowerCase()]
				, n = []
				, i = 0
				, a = t.length
				, r = o[0]
				, s = !1;
			for (var c of e)
				if (s) n.push(c), s = !1;
				else {
					if (i >= a) break;
					switch (c) {
						case "?":
							n.push(r(t.charAt(i++)));
							break;
						case "X":
							for (; i < a;) {
								var l;
								if ("a" <= (l = t.charAt(i++)) && l <= "z" || "A" <= l && l <= "Z" || "0" <= l && l <= "9") {
									n.push(r(l));
									break
								}
							}
							break;
						case "A":
							for (; i < a;) {
								var l;
								if ("a" <= (l = t.charAt(i++)) && l <= "z" || "A" <= l && l <= "Z") {
									n.push(r(l));
									break
								}
							}
							break;
						case "9":
							for (; i < a;) {
								var l;
								if ("0" <= (l = t.charAt(i++)) && l <= "9") {
									n.push(l);
									break
								}
							}
							break;
						case "*":
							for (; i < a;) n.push(r(t.charAt(i++)));
							break;
						case "\\":
							s = !0;
							break;
						case ">":
							r = o[1];
							break;
						case "<":
							r = o[2];
							break;
						case "=":
							r = o[0];
							break;
						default:
							n.push(c)
					}
				} return n.join("")
		}
		, scand: function(e, t) {
			var o = e.split(/[ \-:\/\.]/)
				, n = t.split(/[ \-:\/\.]/);
			if (o.length != n.length) return null;
			for (var i = new Date, a = 0; a < o.length; a++) {
				var r;
				switch (r = (r = n[a])
					.toUpperCase(), o[a]) {
					case "d":
					case "dd":
						if (isNaN(r)) return null;
						i.setDate(parseInt(r));
						break;
					case "m":
					case "mm":
						if (isNaN(r)) return null;
						var r;
						if (0 == (r = parseInt(r)) || r > 12) return null;
						i.setMonth(r);
						break;
					case "mmm":
					case "mmmm":
						if (isNaN(r)) {
							for (var s = ["JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"], c = -1, l = 0, d = s.length; l < d; l++)
								if (-1 !== s[l].indexOf(r)) {
									c = l;
									break
								} if (-1 === c) return null;
							i.setMonth(c)
						} else i.setMonth(parseInt(r) - 1);
						break;
					case "y":
					case "yy":
						if (isNaN(r)) return null;
						i.setFullYear(parseInt(r));
						break;
					case "yyy":
					case "yyyy":
						if (isNaN(r) || r.length != o[a].length) return null;
						i.setFullYear(parseInt(r));
						break;
					case "H":
					case "HH":
						if (isNaN(r)) return null;
						i.setHours(parseInt(r));
						break;
					case "M":
					case "MM":
						if (isNaN(r)) return null;
						i.setMinutes(parseInt(r));
					case "s":
					case "ss":
						if (isNaN(r)) return null;
						i.setSeconds(parseInt(r))
				}
			}
			return i
		}
		, spansToXML: function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}
		, streamFromString: function(e, t) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}
		, stringFromStream: function(e, t) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}
		, xmlToSpans: function(e) {
			console.log("method not defined contact - IDR SOLUTIONS")
		}
	}
		, JSRESERVED = ["break", "delete", "function", "return", "typeof", "case", "do", "if", "switch", "var", "catch", "else", "in", "this", "void", "continue", "false", "instanceof", "throw", "while", "debugger", "finally", "new", "true", "with", "default", "for", "null", "try", "class", "const", "enum", "export", "extends", "import", "super", "implements", "let", "private", "public", "yield", "interface", "package", "protected", "static", "abstract", "double", "goto", "native", "static", "boolean", "enum", "implements", "package", "super", "byte", "export", "import", "private", "synchronized", "char", "extends", "int", "protected", "throws", "class", "final", "interface", "public", "transient", "const", "float", "long", "short", "volatile", "arguments", "encodeURI", "Infinity", "Number", "RegExp", "Array", "encodeURIComponent", "isFinite", "Object", "String", "Boolean", "Error", "isNaN", "parseFloat", "SyntaxError", "Date", "eval", "JSON", "parseInt", "TypeError", "decodeURI", "EvalError", "Math", "RangeError", "undefined", "decodeURIComponent", "Function", "NaN", "ReferenceError", "URIError"]
		, EcmaFilter = {
			decode: function(e, t) {
				if ("FlateDecode" === e) {
					for (var o = new EcmaFlate, n = [], i = 0, a = 2, r = t.length; a < r; a++) n[i++] = t[a];
					return o.decode(n)
				}
				var s, c, l;
				return "ASCII85Decode" === e ? (new EcmaAscii85)
					.decode(t) : "ASCIIHexDecode" === e ? (new EcmaAsciiHex)
						.decode(t) : "RunLengthDecode" === e ? (new EcmaRunLength)
							.decode() : (console.log("This type of decoding is not supported yet : " + e), t)
			}
			, applyPredictor: function(e, t, o, n, i, a, r) {
				if (1 === t) return e;
				for (var s, c = e.length, l = new EcmaBuffer(e), d = n * i + 7 >> 3, u = (a * n * i + 7 >> 3) + d, h = this.createByteBuffer(u), f = this.createByteBuffer(u), m, p = 0, g = 0; !(c <= g);) {
					var y = 0
						, S = d;
					if ((s = t) >= 10) {
						if (-1 === (m = l.getByte())) break;
						m += 10
					} else m = s;
					for (; S < u && -1 !== (y = l.readTo(h, S, u - S));) S += y, g += y;
					if (-1 === y) break;
					switch (m) {
						case 2:
							for (var O = d; O < u; O++) {
								var E = 255 & h[O]
									, I = 255 & h[O - d];
								h[O] = E + I & 255, o && (o[p] = h[O]), p++
							}
							break;
						case 10:
							for (var O = d; O < u; O++) o && (o[p] = 255 & h[O]), p++;
							break;
						case 11:
							for (var O = d; O < u; O++) {
								var E = 255 & h[O]
									, I = 255 & h[O - d];
								h[O] = E + I, o && (o[p] = 255 & h[O]), p++
							}
							break;
						case 12:
							for (var O = d; O < u; O++) {
								var E = (255 & f[O]) + (255 & h[O]);
								h[O] = E, o && (o[p] = 255 & h[O]), p++
							}
							break;
						case 13:
							for (var O = d; O < u; O++) {
								var A = 255 & h[O]
									, D = (255 & h[O - d]) + (255 & f[O]) >> 1;
								h[O] = A + D, o && (o[p] = 255 & h[O]), p++
							}
							break;
						case 14:
							for (var O = d; O < u; O++) {
								var b = 255 & h[O - d]
									, v = 255 & f[O]
									, T = 255 & f[O - d]
									, F = b + v - T
									, R = F - b
									, L = F - v
									, P = F - T;
								R = R < 0 ? -R : R, L = L < 0 ? -L : L, P = P < 0 ? -P : P, h[O] = R <= L && R <= P ? h[O] + b : L <= P ? h[O] + v : h[O] + T, o && (o[p] = 255 & h[O]), p++
							}
							break;
						case 15:
							break
					}
					for (var y = 0; y < u; y++) f[y] = h[y]
				}
				return p
			}
			, createByteBuffer: function(e) {
				for (var t = [], o = 0; o < e; o++) t.push(0);
				return t
			}
			, decodeBase64: function(e) {
				for (var t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=", o, n, i, a, r = [], s = e.replace(/[^A-Za-z0-9\+\/\=]/g, ""), c = s.length, l = 0; l < c;) o = t.indexOf(s.charAt(l++)), n = t.indexOf(s.charAt(l++)), i = t.indexOf(s.charAt(l++)), a = t.indexOf(s.charAt(l++)), r.push(o << 2 | n >> 4), 64 !== i && r.push((15 & n) << 4 | i >> 2), 64 !== a && r.push((3 & i) << 6 | a);
				return r
			}
			, encodeBase64: function(e) {
				for (var t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=", o = "", n, i, a, r, s, c, l, d = 0, u = e.length; d < u;) r = (n = e[d++]) >> 2, s = (3 & n) << 4 | (i = e[d++]) >> 4, c = (15 & i) << 2 | (a = e[d++]) >> 6, l = 63 & a, isNaN(i) ? c = l = 64 : isNaN(a) && (l = 64), o += t.charAt(r) + t.charAt(s) + t.charAt(c) + t.charAt(l);
				return o
			}
		};

	function EcmaFlate() {
		this.decode = function(e) {
			var t, o, n, i, a = 1024;
			for (p = 0, h = 0, f = 0, l = -1, m = !1, E = I = 0, b = null, d = e, u = 0, o = new Array(a), t = [];
				(n = j(o, 0, a)) > 0;)
				for (i = 0; i < n; i++) t.push(o[i]);
			return d = null, t
		};
		var e = [0, 1, 3, 7, 15, 31, 63, 127, 255, 511, 1023, 2047, 4095, 8191, 16383, 32767, 65535]
			, t = [3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0]
			, o = [1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577]
			, n = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15]
			, i = [0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 99, 99]
			, a = [0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13, 13]
			, r = 32768
			, s = 0
			, c = new Array(r << 1)
			, l, d, u, h, f, m, p, g = null
			, y, S, O, E, I, A = 9
			, D = 6
			, b, v, T, F;

		function R() {
			return d.length === u ? -1 : 255 & d[u++]
		}

		function L(t) {
			return h & e[t]
		}

		function P() {
			this.next = null, this.list = null
		}

		function N() {
			this.e = 0, this.b = 0, this.n = 0, this.t = null
		}

		function C(e, t, o, n, i, a) {
			this.BMAX = 16, this.N_MAX = 288, this.status = 0, this.root = null, this.m = 0;
			var r, s = new Array(this.BMAX + 1)
				, c, l, d, u, h, f, m, p = new Array(this.BMAX + 1)
				, g, y, S, O = new N
				, E = new Array(this.BMAX)
				, I = new Array(this.N_MAX)
				, A = new Array(this.BMAX + 1)
				, D, b, v, T, F, R;
			for (R = this.root = null, h = 0; h < s.length; h++) s[h] = 0;
			for (h = 0; h < p.length; h++) p[h] = 0;
			for (h = 0; h < E.length; h++) E[h] = null;
			for (h = 0; h < I.length; h++) I[h] = 0;
			for (h = 0; h < A.length; h++) A[h] = 0;
			c = t > 256 ? e[256] : this.BMAX, g = e, y = 0, h = t;
			do {
				s[g[y]]++, y++
			} while (--h > 0);
			if (s[0] === t) return this.root = null, this.m = 0, this.status = 0, void 0;
			for (f = 1; f <= this.BMAX && 0 === s[f]; f++);
			for (m = f, a < f && (a = f), h = this.BMAX; 0 !== h && 0 === s[h]; h--);
			for (d = h, a > h && (a = h), v = 1 << f; f < h; f++, v <<= 1)
				if ((v -= s[f]) < 0) return this.status = 2, this.m = a, void 0;
			if ((v -= s[h]) < 0) return this.status = 2, this.m = a, void 0;
			for (s[h] += v, A[1] = f = 0, g = s, y = 1, b = 2; --h > 0;) A[b++] = f += g[y++];
			g = e, y = 0, h = 0;
			do {
				0 !== (f = g[y++]) && (I[A[f]++] = h)
			} while (++h < t);
			for (t = A[d], A[0] = h = 0, g = I, y = 0, u = -1, D = p[0] = 0, S = null, T = 0; m <= d; m++)
				for (r = s[m]; r-- > 0;) {
					for (; m > D + p[1 + u];) {
						if (D += p[1 + u], u++, T = (T = d - D) > a ? a : T, (l = 1 << (f = m - D)) > r + 1)
							for (l -= r + 1, b = m; ++f < T && !((l <<= 1) <= s[++b]);) l -= s[b];
						for (D + f > c && D < c && (f = c - D), T = 1 << f, p[1 + u] = f, S = new Array(T), F = 0; F < T; F++) S[F] = new N;
						(R = R ? R.next = new P : this.root = new P)
							.next = null, R.list = S, E[u] = S, u > 0 && (A[u] = h, O.b = p[u], O.e = 16 + f, O.t = S, f = (h & (1 << D) - 1) >> D - p[u], E[u - 1][f].e = O.e, E[u - 1][f].b = O.b, E[u - 1][f].n = O.n, E[u - 1][f].t = O.t)
					}
					for (O.b = m - D, y >= t ? O.e = 99 : g[y] < o ? (O.e = g[y] < 256 ? 16 : 15, O.n = g[y++]) : (O.e = i[g[y] - o], O.n = n[g[y++] - o]), l = 1 << m - D, f = h >> D; f < T; f += l) S[f].e = O.e, S[f].b = O.b, S[f].n = O.n, S[f].t = O.t;
					for (f = 1 << m - 1; 0 != (h & f); f >>= 1) h ^= f;
					for (h ^= f;
						(h & (1 << D) - 1) !== A[u];) D -= p[u], u--
				}
			this.m = p[1], this.status = 0 !== v && 1 !== d ? 1 : 0
		}

		function k(e) {
			for (; f < e;) h |= R() << f, f += 8
		}

		function B(e) {
			h >>= e, f -= e
		}

		function U(e, t, o) {
			if (0 === o) return 0;
			for (var n, i, a = 0; ;) {
				for (k(T), n = (i = b.list[L(T)])
					.e; n > 16;) {
					if (99 === n) return -1;
					B(i.b), k(n -= 16), n = (i = i.t[L(n)])
						.e
				}
				if (B(i.b), 16 !== n) {
					if (15 === n) break;
					for (k(n), E = i.n + L(n), B(n), k(F), n = (i = v.list[L(F)])
						.e; n > 16;) {
						if (99 === n) return -1;
						B(i.b), k(n -= 16), n = (i = i.t[L(n)])
							.e
					}
					for (B(i.b), k(n), I = p - i.n - L(n), B(n); E > 0 && a < o;) E--, I &= r - 1, p &= r - 1, e[t + a++] = c[p++] = c[I++];
					if (a === o) return o
				} else if (p &= r - 1, e[t + a++] = c[p++] = i.n, a === o) return o
			}
			return l = -1, a
		}

		function x(e, t, o) {
			var n;
			if (B(n = 7 & f), k(16), n = L(16), B(16), k(16), n !== (65535 & ~h)) return -1;
			for (B(16), E = n, n = 0; E > 0 && n < o;) E--, p &= r - 1, k(8), e[t + n++] = c[p++] = L(8), B(8);
			return 0 === E && (l = -1), n
		}

		function w(e, n, r) {
			if (null === g) {
				var s, c, l = new Array(288);
				for (s = 0; s < 144; s++) l[s] = 8;
				for (; s < 256; s++) l[s] = 9;
				for (; s < 280; s++) l[s] = 7;
				for (; s < 288; s++) l[s] = 8;
				if (0 !== (c = new C(l, 288, 257, t, i, S = 7))
					.status) {
					throw "EcmaFlateDecodeError : Huffman Status " + c.status;
					return -1
				}
				for (g = c.root, S = c.m, s = 0; s < 30; s++) l[s] = 5;
				if ((c = new C(l, 30, 0, o, a, O = 5))
					.status > 1) {
					throw g = null, "EcmaFlateDecodeError : Huffman Status" + c.status;
					return -1
				}
				y = c.root, O = c.m
			}
			return b = g, v = y, T = S, F = O, U(e, n, r)
		}

		function M(e, r, s) {
			var c, l, d, u, h, f, m, p, g, y = new Array(316);
			for (c = 0; c < y.length; c++) y[c] = 0;
			if (k(5), m = 257 + L(5), B(5), k(5), p = 1 + L(5), B(5), k(4), f = 4 + L(4), B(4), m > 286 || p > 30) return -1;
			for (l = 0; l < f; l++) k(3), y[n[l]] = L(3), B(3);
			for (; l < 19; l++) y[n[l]] = 0;
			if (0 !== (g = new C(y, 19, 19, null, null, T = 7))
				.status) return -1;
			for (b = g.root, T = g.m, u = m + p, c = d = 0; c < u;)
				if (k(T), B(l = (h = b.list[L(T)])
					.b), (l = h.n) < 16) y[c++] = d = l;
				else if (16 === l) {
					if (k(2), l = 3 + L(2), B(2), c + l > u) return -1;
					for (; l-- > 0;) y[c++] = d
				} else if (17 === l) {
					if (k(3), l = 3 + L(3), B(3), c + l > u) return -1;
					for (; l-- > 0;) y[c++] = 0;
					d = 0
				} else {
					if (k(7), l = 11 + L(7), B(7), c + l > u) return -1;
					for (; l-- > 0;) y[c++] = 0;
					d = 0
				}
			if (g = new C(y, m, 257, t, i, T = A), 0 === T && (g.status = 1), 0 !== g.status) return -1;
			for (b = g.root, T = g.m, c = 0; c < p; c++) y[c] = y[c + m];
			return g = new C(y, p, 0, o, a, F = D), v = g.root, 0 === (F = g.m) && m > 257 || 0 !== g.status ? -1 : U(e, r, s)
		}

		function j(e, t, o) {
			for (var n = 0, i; n < o;) {
				if (m && -1 === l) return n;
				if (E > 0) {
					if (l !== s)
						for (; E > 0 && n < o;) E--, I &= r - 1, p &= r - 1, e[t + n++] = c[p++] = c[I++];
					else {
						for (; E > 0 && n < o;) E--, p &= r - 1, k(8), e[t + n++] = c[p++] = L(8), B(8);
						0 === E && (l = -1)
					}
					if (n === o) return n
				}
				if (-1 === l) {
					if (m) break;
					k(1), 0 !== L(1) && (m = !0), B(1), k(2), l = L(2), B(2), b = null, E = 0
				}
				switch (l) {
					case 0:
						i = x(e, t + n, o - n);
						break;
					case 1:
						i = b ? U(e, t + n, o - n) : w(e, t + n, o - n);
						break;
					case 2:
						i = b ? U(e, t + n, o - n) : M(e, t + n, o - n);
						break;
					default:
						i = -1;
						break
				}
				if (-1 === i) return m ? 0 : -1;
				n += i
			}
			return n
		}
	}

	function EcmaAsciiHex() {
		this.decode = function(e) {
			for (var t = [], o = -1, n = 0, i, a, r = !1, s = 0, c = e.length; s < c; s++) {
				if ((i = e[s]) >= 48 && i <= 57) a = 15 & i;
				else {
					if (!(i >= 65 && i <= 70 || i >= 97 && i <= 102)) {
						if (62 === i) {
							r = !0;
							break
						}
						continue
					}
					a = 9 + (15 & i)
				}
				o < 0 ? o = a : (t[n++] = o << 4 | a, o = -1)
			}
			return o >= 0 && r && (t[n++] = o << 4, o = -1), t
		}
	}

	function EcmaAscii85() {
		this.decode = function(e) {
			for (var t = e.length, o = [], n = [0, 0, 0, 0, 0], i, a, r, s, c, l = 0; l < t; ++l)
				if (122 !== e[l]) {
					for (i = 0; i < 5; ++i) n[i] = e[l + i] - 33;
					if ((c = t - l) < 5) {
						for (i = c; i < 4; n[++i] = 0);
						n[c] = 85
					}
					for (r = 255 & (a = 85 * (85 * (85 * (85 * n[0] + n[1]) + n[2]) + n[3]) + n[4]), s = 255 & (a >>>= 8), a >>>= 8, o.push(a >>> 8, 255 & a, s, r), i = c; i < 5; ++i, o.pop());
					l += 4
				} else o.push(0, 0, 0, 0);
			return o
		}
	}

	function EcmaRunLength() {
		this.decode = function(e) {
			for (var t, o, n = e.length, i = 0, a = [], r = 0; r < n; r++)
				if ((t = e[r]) < 0 && (t = 256 + t), 128 === t) r = n;
				else if (t > 128) {
					t = 257 - t, o = e[++r];
					for (var s = 0; s < t; s++) a[i++] = o
				} else {
					r++, t++;
					for (var s = 0; s < t; s++) a[i++] = e[r + s];
					r = r + t - 1
				}
			return a
		}
	}
	var EcmaKEY = {
		A: "A"
		, AA: "AA"
		, AC: "AC"
		, AcroForm: "AcroForm"
		, ActualText: "ActualText"
		, AIS: "AIS"
		, Alternate: "Alternate"
		, AlternateSpace: "AlternateSpace"
		, Annot: "Annot"
		, Annots: "Annots"
		, AntiAlias: "AntiAlias"
		, AP: "AP"
		, Array: "Array"
		, ArtBox: "ArtBox"
		, AS: "AS"
		, Asset: "Asset"
		, Assets: "Assets"
		, Ascent: "Ascent"
		, Author: "Author"
		, AvgWidth: "AvgWidth"
		, B: "B"
		, BlackPoint: "BlackPoint"
		, Background: "Background"
		, Base: "Base"
		, BaseEncoding: "BaseEncoding"
		, BaseFont: "BaseFont"
		, BaseState: "BaseState"
		, BBox: "BBox"
		, BC: "BC"
		, BDC: "BDC"
		, BG: "BG"
		, BI: "BI"
		, BitsPerComponent: "BitsPerComponent"
		, BitsPerCoordinate: "BitsPerCoordinate"
		, BitsPerFlag: "BitsPerFlag"
		, BitsPerSample: "BitsPerSample"
		, BlackIs1: "BlackIs1"
		, BleedBox: "BleedBox"
		, Blend: "Blend"
		, Bounds: "Bounds"
		, Border: "Border"
		, BM: "BM"
		, BPC: "BPC"
		, BS: "BS"
		, Btn: "Btn"
		, ByteRange: "ByteRange"
		, C: "C"
		, C0: "C0"
		, C1: "C1"
		, C2: "C2"
		, CA: "CA"
		, ca: "ca"
		, Calculate: "Calculate"
		, CapHeight: "CapHeight"
		, Caret: "Caret"
		, Category: "Category"
		, Catalog: "Catalog"
		, Cert: "Cert"
		, CF: "CF"
		, CFM: "CFM"
		, Ch: "Ch"
		, CIDSet: "CIDSet"
		, CIDSystemInfo: "CIDSystemInfo"
		, CharProcs: "CharProcs"
		, CharSet: "CharSet"
		, CheckSum: "CheckSum"
		, CIDFontType0C: "CIDFontType0C"
		, CIDToGIDMap: "CIDToGIDMap"
		, Circle: "Circle"
		, ClassMap: "ClassMap"
		, CMap: "CMap"
		, CMapName: "CMapName"
		, CMYK: "CMYK"
		, CO: "CO"
		, Color: "Color"
		, Colors: "Colors"
		, ColorBurn: "ColorBurn"
		, ColorDodge: "ColorDodge"
		, ColorSpace: "ColorSpace"
		, ColorTransform: "ColorTransform"
		, Columns: "Columns"
		, Components: "Components"
		, CompressedObject: "CompressedObject"
		, Compatible: "Compatible"
		, Configurations: "Configurations"
		, Configs: "Configs"
		, ContactInfo: "ContactInfo"
		, Contents: "Contents"
		, Coords: "Coords"
		, Count: "Count"
		, CreationDate: "CreationDate"
		, Creator: "Creator"
		, CropBox: "CropBox"
		, CS: "CS"
		, CVMRC: "CVMRC"
		, D: "D"
		, DA: "DA"
		, DamageRowsBeforeError: "DamageRowsBeforeError"
		, Darken: "Darken"
		, DC: "DC"
		, DCT: "DCT"
		, Decode: "Decode"
		, DecodeParms: "DecodeParms"
		, Desc: "Desc"
		, DescendantFonts: "DescendantFonts"
		, Descent: "Descent"
		, Dest: "Dest"
		, Dests: "Dests"
		, Difference: "Difference"
		, Differences: "Differences"
		, Domain: "Domain"
		, DP: "DP"
		, DR: "DR"
		, DS: "DS"
		, DV: "DV"
		, DW: "DW"
		, DW2: "DW2"
		, E: "E"
		, EarlyChange: "EarlyChange"
		, EF: "EF"
		, EFF: "EFF"
		, EmbeddedFiles: "EmbeddedFiles"
		, EOPROPtype: "EOPROPtype"
		, Encode: "Encode"
		, EncodeByteAlign: "EncodeByteAlign"
		, Encoding: "Encoding"
		, Encrypt: "Encrypt"
		, EncryptMetadata: "EncryptMetadata"
		, EndOfBlock: "EndOfBlock"
		, EndOfLine: "EndOfLine"
		, Exclusion: "Exclusion"
		, Export: "Export"
		, Extend: "Extend"
		, Extends: "Extends"
		, ExtGState: "ExtGState"
		, Event: "Event"
		, F: "F"
		, FDF: "FDF"
		, Ff: "Ff"
		, Fields: "Fields"
		, FIleAccess: "FIleAccess"
		, FileAttachment: "FileAttachment"
		, Filespec: "Filespec"
		, Filter: "Filter"
		, First: "First"
		, FirstChar: "FirstChar"
		, FIrstPage: "FIrstPage"
		, Fit: "Fit"
		, FItB: "FItB"
		, FitBH: "FitBH"
		, FItBV: "FItBV"
		, FitH: "FitH"
		, FItHeight: "FItHeight"
		, FitR: "FitR"
		, FitV: "FitV"
		, FitWidth: "FitWidth"
		, Flags: "Flags"
		, Fo: "Fo"
		, Font: "Font"
		, FontBBox: "FontBBox"
		, FontDescriptor: "FontDescriptor"
		, FontFamily: "FontFamily"
		, FontFile: "FontFile"
		, FontFIle2: "FontFIle2"
		, FontFile3: "FontFile3"
		, FontMatrix: "FontMatrix"
		, FontName: "FontName"
		, FontStretch: "FontStretch"
		, FontWeight: "FontWeight"
		, Form: "Form"
		, Format: "Format"
		, FormType: "FormType"
		, FreeText: "FreeText"
		, FS: "FS"
		, FT: "FT"
		, FullScreen: "FullScreen"
		, Function: "Function"
		, Functions: "Functions"
		, FunctionType: "FunctionType"
		, G: "G"
		, Gamma: "Gamma"
		, GoBack: "GoBack"
		, GoTo: "GoTo"
		, GoToR: "GoToR"
		, Group: "Group"
		, H: "H"
		, HardLight: "HardLight"
		, Height: "Height"
		, Hide: "Hide"
		, Highlight: "Highlight"
		, Hue: "Hue"
		, Hival: "Hival"
		, I: "I"
		, ID: "ID"
		, Identity: "Identity"
		, Identity_H: "Identity_H"
		, Identity_V: "Identity_V"
		, IDTree: "IDTree"
		, IM: "IM"
		, Image: "Image"
		, ImageMask: "ImageMask"
		, Index: "Index"
		, Indexed: "Indexed"
		, Info: "Info"
		, Ink: "Ink"
		, InkList: "InkList"
		, Instances: "Instances"
		, Intent: "Intent"
		, InvisibleRect: "InvisibleRect"
		, IRT: "IRT"
		, IT: "IT"
		, ItalicAngle: "ItalicAngle"
		, JavaScript: "JavaScript"
		, JS: "JS"
		, JT: "JT"
		, JBIG2Globals: "JBIG2Globals"
		, K: "K"
		, Keywords: "Keywords"
		, Keystroke: "Keystroke"
		, Kids: "Kids"
		, L: "L"
		, Lang: "Lang"
		, Last: "Last"
		, LastChar: "LastChar"
		, LastModified: "LastModified"
		, LastPage: "LastPage"
		, Launch: "Launch"
		, Layer: "Layer"
		, Leading: "Leading"
		, Length: "Length"
		, Length1: "Length1"
		, Length2: "Length2"
		, Length3: "Length3"
		, Lighten: "Lighten"
		, Limits: "Limits"
		, Line: "Line"
		, Linearized: "Linearized"
		, LinearizedReader: "LinearizedReader"
		, Link: "Link"
		, ListMode: "ListMode"
		, Location: "Location"
		, Lock: "Lock"
		, Locked: "Locked"
		, Lookup: "Lookup"
		, Luminosity: "Luminosity"
		, LW: "LW"
		, M: "M"
		, MacExpertEncoding: "MacExpertEncoding"
		, MacRomanEncoding: "MacRomanEncoding"
		, Margin: "Margin"
		, MarkInfo: "MarkInfo"
		, Mask: "Mask"
		, Matrix: "Matrix"
		, Matte: "Matte"
		, max: "max"
		, MaxLen: "MaxLen"
		, MaxWidth: "MaxWidth"
		, MCID: "MCID"
		, MediaBox: "MediaBox"
		, Metadata: "Metadata"
		, min: "min"
		, MissingWidth: "MissingWidth"
		, MK: "MK"
		, ModDate: "ModDate"
		, MouseDown: "MouseDown"
		, MouseEnter: "MouseEnter"
		, MouseExit: "MouseExit"
		, MouseUp: "MouseUp"
		, Movie: "Movie"
		, Multiply: "Multiply"
		, N: "N"
		, Name: "Name"
		, Named: "Named"
		, Names: "Names"
		, NeedAppearances: "NeedAppearances"
		, Next: "Next"
		, NextPage: "NextPage"
		, NM: "NM"
		, None: "None"
		, Normal: "Normal"
		, Nums: "Nums"
		, O: "O"
		, ObjStm: "ObjStm"
		, OC: "OC"
		, OCGs: "OCGs"
		, OCProperties: "OCProperties"
		, OE: "OE"
		, OFF: "OFF"
		, off: "off"
		, ON: "ON"
		, On: "On"
		, OnBlur: "OnBlur"
		, OnFocus: "OnFocus"
		, OP: "OP"
		, op: "op"
		, Open: "Open"
		, OpenAction: "OpenAction"
		, OPI: "OPI"
		, OPM: "OPM"
		, Opt: "Opt"
		, Order: "Order"
		, Ordering: "Ordering"
		, Outlines: "Outlines"
		, Overlay: "Overlay"
		, P: "P"
		, PaintType: "PaintType"
		, Page: "Page"
		, PageLabels: "PageLabels"
		, PageMode: "PageMode"
		, Pages: "Pages"
		, Params: "Params"
		, Parent: "Parent"
		, ParentTree: "ParentTree"
		, Pattern: "Pattern"
		, PatternType: "PatternType"
		, PC: "PC"
		, PDFDocEncoding: "PDFDocEncoding"
		, Perms: "Perms"
		, Pg: "Pg"
		, PI: "PI"
		, PieceInfo: "PieceInfo"
		, PO: "PO"
		, Polygon: "Polygon"
		, PolyLine: "PolyLine"
		, Popup: "Popup"
		, Predictor: "Predictor"
		, Prev: "Prev"
		, PrevPage: "PrevPage"
		, Print: "Print"
		, PrinterMark: "PrinterMark"
		, PrintState: "PrintState"
		, Process: "Process"
		, ProcSet: "ProcSet"
		, Producer: "Producer"
		, Projection: "Projection"
		, Properties: "Properties"
		, PV: "PV"
		, Q: "Q"
		, Qfactor: "Qfactor"
		, QuadPoints: "QuadPoints"
		, R: "R"
		, Range: "Range"
		, RBGroups: "RBGroups"
		, RC: "RC"
		, Reason: "Reason"
		, Recipients: "Recipients"
		, Rect: "Rect"
		, Reference: "Reference"
		, Registry: "Registry"
		, ResetForm: "ResetForm"
		, Resources: "Resources"
		, RGB: "RGB"
		, RichMedia: "RichMedia"
		, RichMediaContent: "RichMediaContent"
		, RD: "RD"
		, RoleMap: "RoleMap"
		, Root: "Root"
		, Rotate: "Rotate"
		, Rows: "Rows"
		, RT: "RT"
		, RV: "RV"
		, S: "S"
		, SA: "SA"
		, Saturation: "Saturation"
		, SaveAs: "SaveAs"
		, Screen: "Screen"
		, SetOCGState: "SetOCGState"
		, Shading: "Shading"
		, ShadingType: "ShadingType"
		, Sig: "Sig"
		, SigFlags: "SigFlags"
		, Signed: "Signed"
		, Size: "Size"
		, SM: "SM"
		, SMask: "SMask"
		, SoftLight: "SoftLight"
		, Sound: "Sound"
		, Square: "Square"
		, Squiggly: "Squiggly"
		, Stamp: "Stamp"
		, Standard: "Standard"
		, StandardEncoding: "StandardEncoding"
		, State: "State"
		, StemH: "StemH"
		, StemV: "StemV"
		, StmF: "StmF"
		, StrF: "StrF"
		, StrikeOut: "StrikeOut"
		, StructElem: "StructElem"
		, StructParent: "StructParent"
		, StructParents: "StructParents"
		, StructTreeRoot: "StructTreeRoot"
		, Style: "Style"
		, SubFilter: "SubFilter"
		, Subj: "Subj"
		, Subject: "Subject"
		, SubmitForm: "SubmitForm"
		, Subtype: "Subtype"
		, Supplement: "Supplement"
		, T: "T"
		, Tabs: "Tabs"
		, TagSuspect: "TagSuspect"
		, Text: "Text"
		, TI: "TI"
		, TilingType: "TilingType"
		, tintTransform: "tintTransform"
		, Title: "Title"
		, TM: "TM"
		, Toggle: "Toggle"
		, ToUnicode: "ToUnicode"
		, TP: "TP"
		, TR: "TR"
		, TrapNet: "TrapNet"
		, Trapped: "Trapped"
		, TrimBox: "TrimBox"
		, Tx: "Tx"
		, TxFontSize: "TxFontSize"
		, TxOutline: "TxOutline"
		, TU: "TU"
		, Type: "Type"
		, U: "U"
		, UE: "UE"
		, UF: "UF"
		, Uncompressed: "Uncompressed"
		, Unsigned: "Unsigned"
		, Usage: "Usage"
		, V: "V"
		, Validate: "Validate"
		, VerticesPerRow: "VerticesPerRow"
		, View: "View"
		, VIewState: "VIewState"
		, VP: "VP"
		, W: "W"
		, W2: "W2"
		, Watermark: "Watermark"
		, WhitePoint: "WhitePoint"
		, Widget: "Widget"
		, Win: "Win"
		, WinAnsiEncoding: "WinAnsiEncoding"
		, Width: "Width"
		, Widths: "Widths"
		, WP: "WP"
		, WS: "WS"
		, X: "X"
		, XFA: "XFA"
		, XFAImages: "XFAImages"
		, XHeight: "XHeight"
		, XObject: "XObject"
		, XRef: "XRef"
		, XRefStm: "XRefStm"
		, XStep: "XStep"
		, XYZ: "XYZ"
		, YStep: "YStep"
		, Zoom: "Zoom"
		, ZoomTo: "ZoomTo"
		, Unchanged: "Unchanged"
		, Underline: "Underline"
	}
		, EcmaLEX = {
			CHAR256: [1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 2, 0, 0, 2, 2, 0, 0, 0, 0, 0, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
			, STRPDF: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 728, 711, 710, 729, 733, 731, 730, 732, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8226, 8224, 8225, 8230, 8212, 8211, 402, 8260, 8249, 8250, 8722, 8240, 8222, 8220, 8221, 8216, 8217, 8218, 8482, 64257, 64258, 321, 338, 352, 376, 381, 305, 322, 339, 353, 382, 0, 8364]
			, isWhiteSpace: function(e) {
				return 1 === this.CHAR256[e]
			}
			, isEOL: function(e) {
				return 10 === e || 13 === e
			}
			, isDelimiter: function(e) {
				return 2 === this.CHAR256[e]
			}
			, isComment: function(e) {
				return 37 === e
			}
			, isBacklash: function(e) {
				return 92 === e
			}
			, isEscSeq: function(e, t) {
				if (252 === e) switch (t) {
					case 40:
					case 41:
					case 98:
					case 102:
					case 110:
					case 114:
					case 116:
					case 92:
					case 12:
					case 13:
						return !0;
					default:
						return !1
				}
				return !1
			}
			, isDigit: function(e) {
				return 4 === this.CHAR256[e]
			}
			, isBoolean: function(e) {
				return "boolean" == typeof e
			}
			, isNull: function(e) {
				return null === typeof e
			}
			, isNumber: function(e) {
				return "number" == typeof e
			}
			, isString: function(e) {
				return "string" == typeof e
			}
			, isHexString: function(e) {
				return e instanceof EcmaHEX
			}
			, isArray: function(e) {
				return e instanceof Array
			}
			, isName: function(e) {
				return e instanceof EcmaNAME
			}
			, isDict: function(e) {
				return e instanceof EcmaDICT
			}
			, isRef: function(e) {
				return e instanceof EcmaOREF
			}
			, isStreamDict: function(e) {
				return EcmaKEY.Length in e.keys
			}
			, getDA: function(e) {
				for (var t = {
					fontSize: 10
					, fontName: "Arial"
					, fontColor: "0 g"
				}, o = e.length, n = 0, i = "", a = new Array; n < o;) {
					var r = e.charCodeAt(n++);
					EcmaLEX.isWhiteSpace(r) || EcmaLEX.isEOL(r) ? (i.length > 0 && a.push(i), i = "") : i += String.fromCharCode(r)
				}
				i.length > 0 && a.push(i);
				for (var n = 0, o = a.length; n < o; n++) "/" === a[n].charAt(0) ? (t.fontName = a[n].substring(1), a[n + 1] && (t.fontSize = parseInt(a[n + 1]))) : n > 3 && "rg" === a[n] && (t.fontColor = a[n - 3] + " " + a[n - 2] + " " + a[n - 1] + " rg");
				return t
			}
			, getRefFromString: function(e) {
				var t = e.trim()
					.split(" ");
				return new EcmaOREF(parseInt(t[0]), parseInt(t[1]))
			}
			, getZeroLead: function(e) {
				for (var t = "" + e, o = 10 - t.length, n = 0; n < o; n++) t = "0" + t;
				return t
			}
			, toPDFString: function(e) {
				var t = e.length
					, o = []
					, n;
				if ("þ" === e[0] && "ÿ" === e[1])
					for (var i = 2; i < t; i += 2) n = e.charCodeAt(i) << 8 | e.charCodeAt(i + 1), o.push(String.fromCharCode(n));
				else
					for (var i = 0; i < t; ++i) {
						var a = this.STRPDF[e.charCodeAt(i)];
						o.push(a ? String.fromCharCode(a) : e.charAt(i))
					}
				return o.join("")
			}
			, toPDFHex16String: function(e) {
				var t = e.length
					, o = []
					, n;
				o.push("fe"), o.push("ff");
				for (var i = 0; i < t; ++i) {
					n = e.charCodeAt(i);
					for (var a = Number(n >> 8)
						.toString(16); a.length < 2;) a = "0" + a;
					for (var r = Number(255 & n)
						.toString(16); r.length < 2;) r = "0" + r;
					o.push(a), o.push(r)
				}
				return o.join("")
			}
			, toBytes32: function(e) {
				return [(4278190080 & e) >> 24, (16711680 & e) >> 16, (65280 & e) >> 8, 255 & e]
			}
			, textToBytes: function(e) {
				for (var t = [], o, n = 0, i = e.length; n < i; n++)(o = e.charCodeAt(n)) < 256 ? t.push(o) : (t.push(o >> 8), t.push(255 & o));
				return t
			}
			, bytesToText: function(e, t, o) {
				for (var n = "", i = t; i < o; i++) n += String.fromCharCode(e[t + i]);
				return n
			}
			, pushBytesToBuffer: function(e, t) {
				for (var o = 0, n = e.length; o < n; o++) t.push(e[o])
			}
			, objectToText: function(e) {
				if (this.isDict(e)) {
					var t = "<<";
					for (var o in e.keys) t += "/" + o + " " + this.objectToText(e.keys[o]) + " ";
					return t += ">>"
				}
				if (this.isArray(e)) {
					for (var t = "[", n = 0, i = e.length; n < i; n++) t += " " + this.objectToText(e[n]);
					return t += "]"
				}
				return this.isRef(e) ? e.ref : this.isName(e) ? "/" + e.name : this.isNumber(e) ? "" + e : this.isString(e) ? "(" + EcmaLEX.toPDFString(e) + ")" : this.isHexString(e) ? e.str : this.isBoolean(e) ? e ? "true" : "false" : "null"
			}
		}
		, EcmaFontWidths = {
			Arial: [750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 750, 278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278, 556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556, 1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778, 667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556, 333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556, 556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584, 350, 556, 350, 222, 556, 333, 1e3, 556, 556, 333, 1e3, 667, 333, 1e3, 350, 611, 350, 350, 222, 222, 333, 333, 350, 556, 1e3, 333, 1e3, 500, 333, 944, 350, 500, 667, 278, 333, 556, 556, 556, 556, 260, 556, 333, 737, 370, 556, 584, 333, 737, 552, 400, 549, 333, 333, 333, 576, 537, 333, 333, 333, 365, 556, 834, 834, 834, 611, 667, 667, 667, 667, 667, 667, 1e3, 722, 667, 667, 667, 667, 278, 278, 278, 278, 722, 722, 778, 778, 778, 778, 778, 584, 778, 722, 722, 722, 722, 667, 667, 611, 556, 556, 556, 556, 556, 556, 889, 500, 556, 556, 556, 556, 278, 278, 278, 278, 556, 556, 556, 556, 556, 556, 556, 549, 611, 556, 556, 556, 556, 500, 556, 500]
		}
		, EcmaFormField = {
			READONLY_ID: 1
			, REQUIRED_ID: 2
			, NOEXPORT_ID: 3
			, MULTILINE_ID: 13
			, PASSWORD_ID: 14
			, NOTOGGLETOOFF_ID: 15
			, RADIO_ID: 16
			, PUSHBUTTON_ID: 17
			, COMBO_ID: 18
			, EDIT_ID: 19
			, SORT_ID: 20
			, FILESELECT_ID: 21
			, MULTISELECT_ID: 22
			, DONOTSPELLCHECK_ID: 23
			, DONOTSCROLL_ID: 24
			, COMB_ID: 25
			, RICHTEXT_ID: 26
			, RADIOINUNISON_ID: 26
			, COMMITONSELCHANGE_ID: 27
			, handleDisplayChange: function(e, t, o) {
				const n = this.flagToBooleans(o);
				switch (t.display) {
					case display.hidden:
						n[2] = !0, n[3] = !0, n[6] = !1;
						break;
					case display.noPrint:
						n[2] = !1, n[3] = !1, n[6] = !1;
						break;
					case display.noView:
						n[2] = !1, n[3] = !0, n[6] = !0;
					case display.visible:
						n[2] = !1, n[3] = !0, n[6] = !1;
						break
				}
				e.keys[EcmaKEY.F] = this.booleansToFlag(n)
			}
			, editTextField: function(e, t, o, n, i, a) {
				const r = n.keys[EcmaKEY.Parent]
					, s = r ? e.getIndirectObject(r) : null;
				let c = this.flagToBooleans(n.keys[EcmaKEY.Ff] || s?.keys[EcmaKEY.Ff] || 0)
					, l = c[this.PASSWORD_ID] ? "*".repeat(i.length) : i;
				if (EcmaLEX.isDict(s) && (s.keys[EcmaKEY.V] = i, t.push(s), o.push(r)), n.keys[EcmaKEY.V] = i, n.keys[EcmaKEY.TU]) {
					let e = n.keys[EcmaKEY.TU];
					EcmaLEX.isHexString(e) || (n.keys[EcmaKEY.TU] = e.replace(/[{()}]/g, "_"))
				}
				const d = n.keys[EcmaKEY.MK]?.keys?.[EcmaKEY.R] || 0
					, u = n.keys[EcmaKEY.Rect];
				let h, f;
				d % 180 == 0 ? (h = Math.round(100 * Math.abs(u[2] - u[0])) / 100, f = Math.round(100 * Math.abs(u[3] - u[1])) / 100) : (h = Math.round(100 * Math.abs(u[3] - u[1])) / 100, f = Math.round(100 * Math.abs(u[2] - u[0])) / 100);
				let m = 10
					, p = "0 g"
					, g = "Arial";
				const y = n.keys.DA;
				if (y) {
					const e = EcmaLEX.getDA(y);
					m = e.fontSize, m = 0 === m ? 10 : m, p = e.fontColor, g = e.fontName
				}
				if (i.length <= 0) return;
				n.keys[EcmaKEY.AP] = new EcmaDICT;
				const S = new EcmaOREF(a, 0)
					, O = n.keys[EcmaKEY.AP].keys.N
					, E = O ? e.getObjectValue(O) : new EcmaDICT;
				if (n.keys[EcmaKEY.AP].keys.N = S, E.keys[EcmaKEY.BBox] = [0, 0, h, f], 0 !== d) {
					const e = d * Math.PI / 180
						, t = Math.round(Math.cos(e))
						, o = Math.round(Math.sin(e))
						, n = E.keys[EcmaKEY.BBox][d % 180 == 0 ? 2 : 3];
					E.keys[EcmaKEY.Matrix] = [t, o, -o, t, n, 0]
				} else E.keys[EcmaKEY.Matrix] = [1, 0, 0, 1, h, 0];
				E.keys[EcmaKEY.Subtype] = new EcmaNAME(EcmaKEY.Form);
				const I = new EcmaDICT;
				I.keys[EcmaKEY.Name] = new EcmaNAME("Helv"), I.keys[EcmaKEY.Type] = new EcmaNAME("Font"), I.keys[EcmaKEY.Subtype] = new EcmaNAME("Type1"), I.keys[EcmaKEY.BaseFont] = new EcmaNAME("Helvetica"), I.keys[EcmaKEY.Encoding] = new EcmaNAME("PDFDocEncoding");
				const A = new EcmaDICT;
				A.keys.Helv = I;
				const D = new EcmaDICT;
				if (D.keys[EcmaKEY.Font] = A, E.keys[EcmaKEY.Resources] = D, E.keys[EcmaKEY.FormType] = 1, E.keys[EcmaKEY.Type] = new EcmaNAME(EcmaKEY.XObject), c[this.MULTILINE_ID]) {
					let e = 0
						, t = 0;
					const o = [];
					let n = ""
						, i = "";
					for (let a = 0; a < l.length; a++) {
						const r = l.charCodeAt(a)
							, s = String.fromCharCode(r)
							, c = this.findCodeWidth(r, m);
						10 !== r ? (EcmaLEX.isWhiteSpace(r) ? (i += n + s, e += t + c, n = "", t = 0) : (n += s, t += c), e + t > h && (0 === i.length ? (o.push(n), n = "", t = 0) : (o.push(i), i = "", e = 0))) : (o.push(i + n), n = "", i = "", e = 0, t = 0)
					} (i.length > 0 || n.length > 0) && o.push(i + n);
					const a = Math.round(1.2 * m * 100) / 100
						, r = o.length * a;
					let s = Math.max(f - r + r - m, 0);
					s > 0 && (s = Math.round(100 * s) / 100);
					const c = EcmaLEX.textToBytes(`\n                /Tx BMC\n                q\n                1 1 ${h - 1} ${f - 1} re\n                W\n                n\n                BT\n                /Helv ${m} Tf\n                ${p}\n                2 ${s} Td\n                (${o[0]}) Tj\n                ${o.slice(1).map((e => `0 ${-a} Td\n(${e}) Tj`)).join("\n")}\n                ET\n                Q\n                EMC\n                `);
					E.keys[EcmaKEY.Length] = c.length, E.rawStream = c, E.stream = c
				} else {
					const e = m - .2 * m;
					let t = 2;
					const o = f - e > 0 ? (f - e) / 2 : 0;
					let i = n.keys[EcmaKEY.Q] || 0;
					if (i > 0) {
						let e, o, n = 0;
						t = 0;
						for (let t = 0, i = l.length; t < i; t++) e = l.charCodeAt(t), o = String.fromCharCode(e), n += this.findCodeWidth(e, m);
						n < h && (t = 1 === i ? (h - n) / 2 : h - n)
					}
					const a = EcmaLEX.textToBytes(`\n                /Tx BMC\n                q\n                1 1 ${h - 1} ${f - 1} re\n                W\n                n\n                BT\n                /Helv ${m} Tf\n                ${p}\n                ${t} ${o} Td\n                (${l}) Tj\n                ET\n                Q\n                E\n                `);
					E.keys[EcmaKEY.Length] = a.length, E.rawStream = a, E.stream = a
				}
				t.push(E), o.push(S)
			}
			, selectCheckBox: function(e, t, o) {
				let n = "Yes";
				const i = "Off";
				let a = o.keys[EcmaKEY.AP];
				if (a) {
					a = e.getObjectValue(a);
					let r = a.keys[EcmaKEY.D];
					if (r) {
						r = e.getObjectValue(r);
						for (const e of Object.keys(r.keys)) "off" !== e.toLowerCase() && (n = e)
					}
					t ? (o.keys[EcmaKEY.AS] = new EcmaNAME(n), o.keys[EcmaKEY.V] = new EcmaNAME(n)) : (o.keys[EcmaKEY.AS] = new EcmaNAME(i), o.keys[EcmaKEY.V] = new EcmaNAME(i))
				}
			}
			, selectRadioChild: function(e, t, o) {
				let n = "Yes";
				const i = "Off";
				let a = o.keys[EcmaKEY.AP];
				if (a) {
					a = e.getObjectValue(a);
					let r = a.keys[EcmaKEY.D];
					if (r) {
						r = e.getObjectValue(r);
						for (const e of Object.keys(r.keys)) "off" !== e.toLowerCase() && (t.value !== e ? (n = t.value, this.handleAPNameChange(a, t.value)) : n = e)
					}
					t.checked ? o.keys[EcmaKEY.AS] = new EcmaNAME(n) : o.keys[EcmaKEY.AS] = new EcmaNAME(i)
				}
			}
			, handleAPNameChange: function(e, t) {
				let o = e.keys[EcmaKEY.D];
				if (o) {
					o = (new EcmaBuffer)
						.getObjectValue(o);
					for (const [e, n] of Object.entries(o.keys)) "off" !== e.toLowerCase() && t !== e && (o.keys[t] = n, delete o.keys[e])
				}
				let n = e.keys[EcmaKEY.N];
				if (n) {
					n = (new EcmaBuffer)
						.getObjectValue(n);
					for (const [e, o] of Object.entries(n.keys)) "off" !== e.toLowerCase() && t !== e && (n.keys[t] = o, delete n.keys[e])
				}
				let i = e.keys[EcmaKEY.R];
				if (i) {
					i = (new EcmaBuffer)
						.getObjectValue(i);
					for (const [e, o] of Object.entries(i.keys)) "off" !== e.toLowerCase() && t !== e && (i.keys[t] = o, delete i.keys[e])
				}
			}
			, selectChoice: function(e, t, o, n, i, a) {
				let r = n.keys[EcmaKEY.Opt];
				EcmaLEX.isRef(r) && (r = e.getIndirectObject(r));
				const s = i instanceof Array ? i : [i];
				if (n.keys[EcmaKEY.V] = 1 === s.length ? s[0] : s, r) {
					const e = s.map((e => r.findIndex((t => EcmaLEX.isArray(t) ? t[0] === e : t === e))))
						.filter((e => -1 !== e));
					0 === e.length ? delete n.keys[EcmaKEY.I] : n.keys[EcmaKEY.I] = e
				}
				n.keys[EcmaKEY.AP] = new EcmaDICT;
				let c = n.keys[EcmaKEY.Rect]
					, l = c[2] - c[0]
					, d = c[3] - c[1]
					, u = 10
					, h = n.keys.DA;
				if (h) {
					const e = h.indexOf("/");
					e >= 0 && (h = h.substring(e)
						.split(" "), u = parseInt(h[1])), n.keys.DA = "/Arial " + u + " Tf"
				}
				const f = new EcmaDICT
					, m = new EcmaOREF(a, 0);
				n.keys[EcmaKEY.AP].keys.N = m, f.keys[EcmaKEY.BBox] = [0, 0, l, d], f.keys[EcmaKEY.Matrix] = [1, 0, 0, 1, 0, 0], f.keys[EcmaKEY.Subtype] = new EcmaNAME(EcmaKEY.Form), f.keys[EcmaKEY.Resources] = new EcmaDICT, f.keys[EcmaKEY.FormType] = 1, f.keys[EcmaKEY.Type] = new EcmaNAME(EcmaKEY.XObject);
				const p = u - .2 * u
					, g = 1.2 * u
					, y = g * s.length
					, S = undefined;
				let O = `/Tx BMC\nBT\n/Arial ${u} Tf\n0 g\n2 ${d - p + y > 0 ? (d - p + y) / 2 : 0} Td\n(${s[0]}) Tj\n`;
				for (let e = 1; e < s.length; e++) O += `0 ${-g} Td\n(${s[e]}) Tj\n`;
				O += "ET\nEMC";
				const E = EcmaLEX.textToBytes(O);
				f.keys[EcmaKEY.Length] = E.length, f.rawStream = E, f.stream = E, t.push(f), o.push(m)
			}
			, findStringWidth: function(e, t) {
				for (var o = 0, n = 0, i = e.length; n < i; n++) {
					var a = e.charCodeAt(n);
					o += a < 256 ? EcmaFontWidths.Arial[a] / 1e3 * t : t
				}
				return o
			}
			, findCodeWidth: function(e, t) {
				return e < 256 ? EcmaFontWidths.Arial[e] / 1e3 * t : t
			}
			, flagToBooleans: function(e) {
				for (var t = [!1], o = 0; o < 32; o++) t[o + 1] = (e & 1 << o) == 1 << o;
				return t
			}
			, booleansToFlag: function(e) {
				for (var t = 0, o = 0; o < 32; o++) t = e[32 - o] ? t << 1 | 1 : t <<= 1;
				return t
			}
			, hexEncodeName: function(e) {
				for (var t = "", o = 0; o < e.length; o++) {
					var n = e.charCodeAt(o);
					n < 33 || n > 126 || '"' === e[o] || "#" === e[o] || "/" === e[o] ? t += n.toString(16) : t += e[o]
				}
				return t
			}
			, hexDecodeName: function(e) {
				for (var t = "", o = 0; o < e.length; o++) {
					var n = e.charCodeAt(o);
					if ("#" === e[o] && o + 2 < e.length) {
						var i = parseInt(e[o + 1] + e[o + 2], 16);
						t += String.fromCharCode(i), o += 2
					} else (n >= 33 || n <= 126) && (t += e[o])
				}
				return t
			}
		};
	let EcmaNAMES = {}
		, EcmaOBJECTOFFSETS = {}
		, EcmaPageOffsets = []
		, EcmaFieldOffsets = []
		, EcmaMainCatalog = null
		, EcmaMainData = []
		, EcmaXRefType = 0;

	function showEcmaParserError(e) {
		alert(e)
	}

	function EcmaOBJOFF(e, t, o) {
		this.data = t, this.offset = e, this.isStream = o
	}

	function EcmaDICT() {
		this.keys = {}, this.stream = null, this.rawStream = null
	}

	function EcmaNAME(e) {
		this.name = e, this.value = null
	}

	function EcmaOREF(e, t) {
		this.num = e, this.gen = t, this.ref = e + " " + t + " R"
	}

	function EcmaHEX(e) {
		this.str = e
	}

	function EcmaBuffer(e) {
		this.data = e, this.pos = 0, this.markPos = 0, this.length = 0, e && (this.length = e.length)
	}
	EcmaBuffer.prototype.getByte = function() {
		return this.pos >= this.length ? -1 : this.data[this.pos++]
	}, EcmaBuffer.prototype.mark = function() {
		this.markPos = this.pos
	}, EcmaBuffer.prototype.reset = function() {
		this.pos = this.markPos
	}, EcmaBuffer.prototype.movePos = function(e) {
		this.pos = e
	}, EcmaBuffer.prototype.readTo = function(e) {
		for (var t = this.length - this.pos, o = Math.min(e.length, t), n = 0; n < o; n++) e[n] = this.getByte()
	}, EcmaBuffer.prototype.readTo = function(e, t, o) {
		if (this.pos < this.length) {
			for (var n = 0, i = this.length - this.pos, a = Math.min(o, i), r = 0; r < a; r++) e[t + r] = this.getByte(), n++;
			return n
		}
		return -1
	}, EcmaBuffer.prototype.lookNext = function() {
		return this.pos >= this.length ? -1 : this.data[this.pos]
	}, EcmaBuffer.prototype.lookNextNext = function() {
		return this.pos >= this.length ? -1 : this.data[this.pos + 1]
	}, EcmaBuffer.prototype.getNextLine = function() {
		for (var e = "", t = this.getByte(); ;)
			if (13 === t) {
				if (e.endsWith(" ")) break;
				if (10 === (t = this.getByte())) break
			} else {
				if (10 === t) break;
				e += String.fromCharCode(t), t = this.getByte()
			} return e
	}, EcmaBuffer.prototype.skipLine = function() {
		for (var e = this.getByte(); - 1 !== e;) {
			if (13 === e) {
				if (10 === (e = this.lookNext())) {
					this.getByte();
					break
				}
				break
			}
			if (10 === e) break;
			e = this.getByte()
		}
	}, EcmaBuffer.prototype.getNumberValue = function() {
		var e = this.getByte()
			, t = 1
			, o = !1;
		if (43 === e ? e = this.getByte() : 45 === e && (t = -1, e = this.getByte()), 46 === e && (o = !0, e = this.getByte()), e < 48 || e > 57) return 0;
		for (var n = "" + String.fromCharCode(e); ;) {
			var i = this.lookNext();
			if (46 !== i && !EcmaLEX.isDigit(i)) break;
			e = this.getByte(), n += "" + String.fromCharCode(e)
		}
		return o ? t * parseFloat("0." + n) : -1 !== n.indexOf(".") ? t * parseFloat(n) : t * parseInt(n)
	}, EcmaBuffer.prototype.getNameValue = function() {
		var e = ""
			, t;
		for (this.getByte();
			(t = this.lookNext()) >= 0 && !EcmaLEX.isDelimiter(t) && !EcmaLEX.isWhiteSpace(t);) e += String.fromCharCode(this.getByte());
		return e
	}, EcmaBuffer.prototype.getNormalString = function() {
		var e = [];
		this.getByte();
		for (var t = 1, o = this.getByte(), n = !1; ;) {
			var i = !1;
			switch (o) {
				case -1:
					n = !0;
					break;
				case 40:
					e.push("("), t++;
					break;
				case 41:
					--t ? e.push(")") : n = !0;
					break;
				case 92:
					switch (o = this.getByte()) {
						case -1:
							n = !0;
							break;
						case 40:
						case 41:
						case 92:
							e.push(String.fromCharCode(o));
							break;
						case 110:
							e.push("\n");
							break;
						case 114:
							e.push("\r");
							break;
						case 116:
							e.push("\t");
							break;
						case 98:
							e.push("\b");
							break;
						case 102:
							e.push("\f");
							break;
						case 48:
						case 49:
						case 50:
						case 51:
						case 52:
						case 53:
						case 54:
						case 55:
							var a = 15 & o;
							i = !0, (o = this.getByte()) >= 48 && o <= 55 && (a = (a << 3) + (15 & o), (o = this.getByte()) >= 48 && o <= 55 && (i = !1, a = (a << 3) + (15 & o))), e.push(String.fromCharCode(a));
							break;
						case 13:
							10 === this.lookNext() && this.getByte();
							break;
						case 10:
							break;
						default:
							e.push(String.fromCharCode(o));
							break
					}
					break;
				default:
					e.push(String.fromCharCode(o))
			}
			if (n) break;
			i || (o = this.getByte())
		}
		return e.join("")
	}, EcmaBuffer.prototype.getHexString = function() {
		this.getByte();
		for (var e = this.getByte(), t = "<"; ;) {
			if (e < 0 || 62 === e) {
				t += ">";
				break
			}
			EcmaLEX.isWhiteSpace(e) ? e = this.getByte() : (t += String.fromCharCode(e), e = this.getByte())
		}
		return new EcmaHEX(t)
	}, EcmaBuffer.prototype.getDictionary = function() {
		var e = new EcmaDICT;
		this.getByte(), this.getByte();
		for (var t = [], o = !0; o;) {
			var n;
			switch (this.lookNext()) {
				case -1:
					return e;
				case 48:
				case 49:
				case 50:
				case 51:
				case 52:
				case 53:
				case 54:
				case 55:
				case 56:
				case 57:
				case 43:
				case 45:
				case 46:
					var i = this.getNumberValue()
						, a = this.lookNext()
						, r = this.lookNextNext();
					if (t.length > 0) {
						var s, c = (s = t.pop())
							.name;
						EcmaLEX.isWhiteSpace(a) && EcmaLEX.isDigit(r) ? (this.getByte(), r = this.getNumberValue(), this.getByte(), this.getByte(), e.keys[c] = new EcmaOREF(i, r)) : e.keys[c] = i
					}
					break;
				case 47:
					var l = this.getNameValue()
						, d;
					if (EcmaNAMES[l] ? d = EcmaNAMES[l] : (d = new EcmaNAME(l), EcmaNAMES[l] = d), 0 === t.length) t.push(d);
					else {
						var s, c = (s = t.pop())
							.name;
						e.keys[c] = d
					}
					break;
				case 40:
					var u = this.getNormalString();
					if (0 !== t.length) {
						var s, c = (s = t.pop())
							.name;
						e.keys[c] = u
					}
					break;
				case 60:
					if (60 === this.lookNextNext()) {
						var h = this.getDictionary();
						if (0 !== t.length) {
							var s, c = (s = t.pop())
								.name;
							e.keys[c] = h
						}
					} else {
						var f = this.getHexString();
						if (0 !== t.length) {
							var s, c = (s = t.pop())
								.name;
							e.keys[c] = f
						}
					}
					break;
				case 91:
					var m = this.getArray();
					if (0 !== t.length) {
						var s, c = (s = t.pop())
							.name;
						e.keys[c] = m
					}
					break;
				case 116:
					if (114 === this.data[this.pos + 1] && 117 === this.data[this.pos + 2] && 101 === this.data[this.pos + 3]) {
						for (var p = 0; p < 4; p++) this.getByte();
						if (t.length > 0) {
							var s, c = (s = t.pop())
								.name;
							e.keys[c] = !0
						}
					} else this.getByte();
					break;
				case 102:
					if (97 === this.data[this.pos + 1] && 108 === this.data[this.pos + 2] && 115 === this.data[this.pos + 3] && 101 === this.data[this.pos + 4]) {
						for (var p = 0; p < 5; p++) this.getByte();
						if (t.length > 0) {
							var s, c = (s = t.pop())
								.name;
							e.keys[c] = !1
						}
					} else this.getByte();
					break;
				case 110:
					if (117 === this.data[this.pos + 1] && 108 === this.data[this.pos + 2] && 108 === this.data[this.pos + 3]) {
						for (var p = 0; p < 4; p++) this.getByte();
						if (t.length > 0) {
							var s, c = (s = t.pop())
								.name;
							e.keys[c] = null
						}
					} else this.getByte();
					break;
				case 62:
					this.getByte(), 62 === this.lookNext() && (this.getByte(), o = !1);
					break;
				default:
					this.getByte();
					break
			}
		}
		return EcmaLEX.isStreamDict(e) && !e.stream && (e.rawStream = this.getRawStream(e)), e
	}, EcmaBuffer.prototype.getArray = function() {
		this.getByte();
		for (var e = []; ;) {
			var t;
			switch (this.lookNext()) {
				case -1:
					return e;
				case 48:
				case 49:
				case 50:
				case 51:
				case 52:
				case 53:
				case 54:
				case 55:
				case 56:
				case 57:
				case 43:
				case 45:
				case 46:
					var o = this.getNumberValue()
						, n = this.data[this.pos]
						, i = this.data[this.pos + 1];
					if (EcmaLEX.isWhiteSpace(n) && EcmaLEX.isDigit(i)) {
						this.mark(), this.getByte(), i = this.getNumberValue(), this.getByte();
						var a = this.getByte()
							, r = this.lookNext();
						82 === a && (EcmaLEX.isWhiteSpace(r) || EcmaLEX.isDelimiter(r)) ? e.push(new EcmaOREF(o, i)) : (e.push(o), this.reset())
					} else e.push(o);
					break;
				case 47:
					var s = this.getNameValue();
					EcmaNAMES[s] || (EcmaNAMES[s] = new EcmaNAME(s)), e.push(EcmaNAMES[s]);
					break;
				case 40:
					e.push(this.getNormalString());
					break;
				case 60:
					if (60 === this.lookNextNext()) {
						var c = this.getDictionary();
						e.push(c)
					} else e.push(this.getHexString());
					break;
				case 91:
					e.push(this.getArray());
					break;
				case 93:
					return this.getByte(), e;
				case 116:
					if (114 === this.data[this.pos + 1] && 117 === this.data[this.pos + 2] && 101 === this.data[this.pos + 3]) {
						e.push(!0);
						for (var l = 0; l < 4; l++) this.getByte()
					} else this.getByte();
					break;
				case 102:
					if (97 === this.data[this.pos + 1] && 108 === this.data[this.pos + 2] && 115 === this.data[this.pos + 3] && 101 === this.data[this.pos + 4]) {
						e.push(!1);
						for (var l = 0; l < 5; l++) this.getByte()
					} else this.getByte();
					break;
				case 110:
					if (117 === this.data[this.pos + 1] && 108 === this.data[this.pos + 2] && 108 === this.data[this.pos + 3]) {
						e.push(null);
						for (var l = 0; l < 4; l++) this.getByte()
					} else this.getByte();
				default:
					this.getByte();
					break
			}
		}
	}, EcmaBuffer.prototype.getRawStream = function(e) {
		for (; ;) {
			var t;
			if (115 === (t = this.lookNext()) && 116 === this.data[this.pos + 1] && 114 === this.data[this.pos + 2] && 101 === this.data[this.pos + 3] && 97 === this.data[this.pos + 4] && 109 === this.data[this.pos + 5]) {
				for (var o = 0; o < 6; o++) this.getByte();
				break
			}
			if (-1 === t) return null;
			this.getByte()
		}
		this.skipLine();
		for (var n = this.getObjectValue(e.keys[EcmaKEY.Length]), i = new Array(n), o = 0; o < n; o++) i[o] = 255 & this.getByte();
		for (; ;) {
			var t;
			if (-1 === (t = this.lookNext())) break;
			if (101 === t && 110 === this.data[this.pos + 1] && 100 === this.data[this.pos + 2] && 115 === this.data[this.pos + 3] && 116 === this.data[this.pos + 4] && 114 === this.data[this.pos + 5] && 101 === this.data[this.pos + 6] && 97 === this.data[this.pos + 7] && 109 === this.data[this.pos + 8]) {
				for (var o = 0; o < 9; o++) this.getByte();
				break
			}
			this.getByte()
		}
		return i
	}, EcmaBuffer.prototype.getStream = function(e) {
		if (e.stream) return e.stream;
		var t = e.rawStream
			, o = e.keys[EcmaKEY.Filter];
		if (o)
			if (o instanceof Array)
				for (var n = 0, i = o.length; n < i; n++) t = EcmaFilter.decode(o[n].name, t);
			else t = EcmaFilter.decode(o.name, t);
		var a = e.keys[EcmaKEY.DecodeParms];
		if (a) {
			var r = 1
				, s = 1
				, c = 8
				, l = 1
				, d = 1
				, u, h;
			if (a instanceof Array)
				for (var n = 0, i = a.length; n < i; n++) {
					var u, h;
					(h = (u = this.getObjectValue(a[n]))
						.keys[EcmaKEY.Predictor]) && (r = h), (h = u.keys[EcmaKEY.Colors]) && (s = h), (h = u.keys[EcmaKEY.BitsPerComponent]) && (c = h), (h = u.keys[EcmaKEY.Columns]) && (l = h), (h = u.keys[EcmaKEY.EarlyChange]) && (d = h)
				} else (h = (u = this.getObjectValue(a))
					.keys[EcmaKEY.Predictor]) && (r = h), (h = u.keys[EcmaKEY.Colors]) && (s = h), (h = u.keys[EcmaKEY.BitsPerComponent]) && (c = h), (h = u.keys[EcmaKEY.Columns]) && (l = h), (h = u.keys[EcmaKEY.EarlyChange]) && (d = h);
			if (1 !== r) {
				var f = EcmaFilter.applyPredictor(t, r, null, s, c, l, d)
					, m = EcmaFilter.createByteBuffer(f);
				EcmaFilter.applyPredictor(t, r, m, s, c, l, d)
			}
			t = m
		}
		return e.stream = t, t
	}, EcmaBuffer.prototype.getObjectValue = function(e) {
		if (EcmaLEX.isName(e)) return e.name;
		if (EcmaLEX.isDict(e)) return e;
		if (EcmaLEX.isRef(e)) {
			var t = this.getIndirectObject(e, this.data, !1);
			return this.getObjectValue(t)
		}
		return e
	}, EcmaBuffer.prototype.getIndirectObject = function(e) {
		for (var t in EcmaOBJECTOFFSETS)
			if (t === e.ref) {
				var o = EcmaOBJECTOFFSETS[t]
					, n = o.offset
					, i = new EcmaBuffer(o.data)
					, a;
				if (o.isStream) return o.data ? (i.movePos(n), i.getObj()) : null;
				for (i.movePos(n); ;) {
					var r = i.lookNext();
					if (-1 === r) return null;
					if (111 === r && 98 === i.data[i.pos + 1] && 106 === i.data[i.pos + 2]) {
						for (var s = 0; s < 3; s++) i.getByte();
						break
					}
					i.getByte()
				}
				return i.getObj()
			} return null
	}, EcmaBuffer.prototype.getObj = function() {
		for (; ;) {
			var e;
			switch (this.lookNext()) {
				case -1:
					return null;
				case 48:
				case 49:
				case 50:
				case 51:
				case 52:
				case 53:
				case 54:
				case 55:
				case 56:
				case 57:
				case 43:
				case 45:
				case 46:
					var t = this.getNumberValue()
						, o = this.lookNext()
						, n = this.lookNextNext()
						, i = this.data[this.pos + 2]
						, a = this.data[this.pos + 3];
					return EcmaLEX.isWhiteSpace(o) && EcmaLEX.isDigit(n) && EcmaLEX.isWhiteSpace(i) && 82 === a ? (this.getByte(), n = this.getNumberValue(), this.getByte(), this.getByte(), new EcmaOREF(t, n)) : t;
				case 47:
					return this.getNameValue();
				case 40:
					return this.getNormalString();
				case 60:
					return 60 === this.lookNextNext() ? this.getDictionary() : this.getHexString();
				case 91:
					return this.getArray();
				case 116:
					if (114 === this.data[this.pos + 1] && 117 === this.data[this.pos + 2] && 101 === this.data[this.pos + 3]) {
						for (var r = 0; r < 4; r++) this.getByte();
						return !0
					}
					this.getByte();
					break;
				case 102:
					if (97 === this.data[this.pos + 1] && 108 === this.data[this.pos + 2] && 115 === this.data[this.pos + 3] && 101 === this.data[this.pos + 4]) {
						for (var r = 0; r < 5; r++) this.getByte();
						return !1
					}
					this.getByte();
				case 110:
					if (117 === this.data[this.pos + 1] && 108 === this.data[this.pos + 2] && 108 === this.data[this.pos + 3]) {
						for (var r = 0; r < 4; r++) this.getByte();
						return null
					}
					this.getByte();
				default:
					this.getByte();
					break
			}
		}
		return null
	}, EcmaBuffer.prototype.readSimpleXREF = function() {
		var e = this.lookNext();
		if (EcmaLEX.isDigit(e)) return this.readStreamXREF(), void 0;
		this.skipLine();
		for (var t = null; ;) {
			var o = this.lookNext();
			if (-1 === o) break;
			if (EcmaLEX.isEOL(o)) this.skipLine();
			else {
				if (116 === o && 114 === this.data[this.pos + 1] && 97 === this.data[this.pos + 2] && 105 === this.data[this.pos + 3] && 108 === this.data[this.pos + 4] && 101 === this.data[this.pos + 5] && 114 === this.data[this.pos + 6]) {
					t = this.getObj();
					break
				}
				var n = this.getObj();
				this.getByte();
				var i = this.getObj();
				this.skipLine();
				for (var a = 0; a < i; a++) {
					var r = this.getObj()
						, s = this.getObj()
						, c = this.getNextLine()
						, l = n + a + " " + s + " R";
					"n" !== (c = c.trim()) || EcmaOBJECTOFFSETS[l] || (EcmaOBJECTOFFSETS[l] = new EcmaOBJOFF(r, this.data, !1))
				}
			}
		}
		if (t) {
			EcmaMainCatalog || (EcmaMainCatalog = t.keys.Root);
			var d = t.keys[EcmaKEY.Prev];
			if (d) {
				var u = this.getObjectValue(d);
				this.movePos(u), this.readSimpleXREF()
			}
		} else showEcmaParserError("Trailer not found")
	}, EcmaBuffer.prototype.readStreamXREF = function() {
		EcmaXRefType = 1, this.getObj(), this.getObj();
		var e = this.getObj()
			, t = this.getStream(e)
			, o = e.keys[EcmaKEY.W]
			, n = e.keys[EcmaKEY.Index];
		n || (n = [0, e.keys[EcmaKEY.Size]]);
		for (var i = o[0], a = o[1], r = o[2], s = n.length, c = 0, l = new EcmaBuffer(t); s > c;)
			for (var d = n[c++], u = d + n[c++], h = d; h < u; h++) {
				var f = 0
					, m = 0
					, p = 0;
				if (0 === i) f = 1;
				else
					for (var g = 0; g < i; g++) f = f << 8 | l.getByte();
				for (var g = 0; g < a; g++) m = m << 8 | l.getByte();
				for (var g = 0; g < r; g++) p = p << 8 | l.getByte();
				var y = h + " " + p + " R";
				if (!EcmaOBJECTOFFSETS[y]) switch (f) {
					case 0:
						break;
					case 1:
						EcmaOBJECTOFFSETS[y] = new EcmaOBJOFF(m, EcmaMainData, !1);
						break;
					case 2:
						EcmaOBJECTOFFSETS[y] = new EcmaOBJOFF(m, null, !0);
						break
				}
			}
		EcmaMainCatalog || (EcmaMainCatalog = e.keys.Root);
		var S = e.keys[EcmaKEY.Prev];
		if (S) {
			var O = this.getObjectValue(S);
			this.movePos(O), this.readSimpleXREF()
		}
	}, EcmaBuffer.prototype.findFirstXREFOffset = function() {
		for (var e = this.data.length - 10; e > 0;) {
			var t, o;
			if (115 === this.data[e] && 116 === this.data[e + 1] && 97 === this.data[e + 2] && 114 === this.data[e + 3] && 116 === this.data[e + 4] && 120 === this.data[e + 5] && 114 === this.data[e + 6] && 101 === this.data[e + 7] && 102 === this.data[e + 8]) return this.movePos(e), this.skipLine(), this.getObj();
			e--
		}
		return -1
	}, EcmaBuffer.prototype.updateAllObjStm = function() {
		for (var e in EcmaOBJECTOFFSETS) {
			var t = e.split(" ")
				, o = new EcmaOREF(t[0], t[1])
				, n = this.getIndirectObject(o);
			if (n instanceof EcmaDICT && n.keys[EcmaKEY.Type] && n.keys[EcmaKEY.Type].name === EcmaKEY.ObjStm)
				for (var i = n.keys[EcmaKEY.N], a = n.keys[EcmaKEY.First], r = new EcmaBuffer(this.getStream(n)), s = 0; s < i; s++) {
					var c = r.getNumberValue();
					r.getByte();
					var l = r.getNumberValue();
					r.getByte();
					var d = c + " 0 R"
						, u = new EcmaOBJOFF(a + l, this.getStream(n), !0);
					d in EcmaOBJECTOFFSETS ? EcmaOBJECTOFFSETS[d].isStream && !EcmaOBJECTOFFSETS[d].data && (EcmaOBJECTOFFSETS[d] = u) : EcmaOBJECTOFFSETS[d] = u
				}
		}
	}, EcmaBuffer.prototype.updatePageOffsets = function() {
		var e, t = this.getObjectValue(EcmaMainCatalog)
			.keys[EcmaKEY.Pages];
		t && (t = this.getObjectValue(t), this.getPagesFromPageTree(t))
	}, EcmaBuffer.prototype.getPagesFromPageTree = function(e) {
		for (var t = e.keys[EcmaKEY.Kids], o = 0, n = (t = this.getObjectValue(t))
			.length; o < n; o++) {
			var i = t[o]
				, a = this.getObjectValue(i)
				, r = a.keys[EcmaKEY.Type];
			r.name === EcmaKEY.Pages ? this.getPagesFromPageTree(a) : r.name === EcmaKEY.Page && EcmaPageOffsets.push(i)
		}
	};
	var EcmaParser = {
		saveFormToPDF: function(e) {
			var t = this._insertFieldsToPDF(e);
			this._openURL(e, t)
		}
		, _insertFieldsToPDF: function(e) {
			this._updateFileInfo(e);
			const t = new EcmaBuffer(EcmaMainData)
				, o = t.findFirstXREFOffset();
			o && (t.movePos(o), t.readSimpleXREF()), t.updateAllObjStm(), t.updatePageOffsets();
			let n = 1;
			for (const e of Object.keys(EcmaOBJECTOFFSETS)) n = Math.max(parseInt(e.split(" ")[0]), n);
			n++;
			const i = []
				, a = []
				, r = undefined
				, s = t.getObjectValue(EcmaMainCatalog)
					.keys[EcmaKEY.AcroForm]
				, c = t.getObjectValue(s);
			delete c.keys[EcmaKEY.XFA], i.push(c), a.push(s);
			const {
				texts: l
				, checks: d
				, checkGroups: u
				, radios: h
				, choices: f
				, editableChoices: m
				, buttons: p
			} = doc._getFormFields();
			for (const e of l) {
				const o = e.dataset.realValue || e.value
					, r = e.dataset.objref
					, s = EcmaLEX.getRefFromString(r)
					, c = t.getObjectValue(s);
				i.push(c), a.push(s), EcmaFormField.editTextField(t, i, a, c, o, n++)
			}
			for (const e of p) {
				const o = e.dataset.objref
					, n = EcmaLEX.getRefFromString(o)
					, r = t.getObjectValue(n)
					, s = r.keys[EcmaKEY.F]
					, c = e.dataset.fieldName
					, l = idrform.doc.getField(c);
				e.dataset && e.dataset.defaultDisplay && l.display !== Number(e.dataset.defaultDisplay) && (i.push(r), a.push(n), EcmaFormField.handleDisplayChange(r, l, s))
			}
			for (const e of [...d, ...u]) {
				const o = e.checked
					, n = e.dataset.objref
					, r = EcmaLEX.getRefFromString(n)
					, s = t.getObjectValue(r);
				i.push(s), a.push(r), EcmaFormField.selectCheckBox(t, o, s)
			}
			for (const e of [...f, ...m]) {
				const o = "INPUT" === e.tagName ? e.dataset.realValue || e.value : doc._getSelectElementValues(e)
					, r = e.dataset.objref
					, s = EcmaLEX.getRefFromString(r)
					, c = t.getObjectValue(s);
				i.push(c), a.push(s), EcmaFormField.selectChoice(t, i, a, c, o, n++)
			}
			const g = {};
			for (const e of h) {
				const o = e.dataset.objref
					, n = EcmaLEX.getRefFromString(o)
					, i = undefined
					, a = t.getObjectValue(n)
						.keys[EcmaKEY.Parent].ref
					, r = e.checked
					, s = e.value;
				if (a) {
					let e = g[a];
					void 0 === e && (e = g[a] = []), e.push({
						radioRef: o
						, parentRef: a
						, checked: r
						, value: s
					})
				} else g[o] = [{
					radioRef: o
					, parentRef: null
					, checked: r
					, value: s
				}]
			}
			for (const [e, o] of Object.entries(g)) {
				const e = o[0].parentRef;
				if (e) {
					const n = EcmaLEX.getRefFromString(e)
						, r = t.getObjectValue(n);
					a.push(n), i.push(r);
					let s = !1
						, c = null;
					for (const e of o)
						if (e.checked) {
							c = e.value, s = !0;
							break
						} s ? r.keys[EcmaKEY.V] = new EcmaNAME(c) : delete r.keys[EcmaKEY.V];
					for (const e of o) {
						const o = EcmaLEX.getRefFromString(e.radioRef)
							, n = t.getObjectValue(o);
						a.push(o), i.push(n), EcmaFormField.selectRadioChild(t, e, n)
					}
				} else {
					const e = EcmaLEX.getRefFromString(o[0].radioRef)
						, n = t.getObjectValue(e);
					a.push(e), i.push(n), EcmaFormField.selectRadioChild(t, o[0], n)
				}
			}
			return this._saveFieldObjects(o, n, a, i), EcmaMainData
		}
		, _saveFieldObjects: function(e, t, o, n) {
			let i = EcmaMainData.length;
			const a = [];
			for (let e = 0, t = o.length; e < t; e++) {
				const t = o[e].num
					, r = n[e];
				a.push({
					ref: t
					, offset: i
				});
				let s = [];
				if (r.keys[EcmaKEY.Length]) {
					const o = EcmaLEX.textToBytes(t + " 0 obj\n")
						, i = EcmaLEX.textToBytes(EcmaLEX.objectToText(n[e]) + "stream\n")
						, a = n[e].rawStream
						, r = EcmaLEX.textToBytes("\nendstream\nendobj\n");
					EcmaLEX.pushBytesToBuffer(o, s), EcmaLEX.pushBytesToBuffer(i, s), EcmaLEX.pushBytesToBuffer(a, s), EcmaLEX.pushBytesToBuffer(r, s), EcmaLEX.pushBytesToBuffer(s, EcmaMainData)
				} else {
					const o = t + " 0 obj\n" + EcmaLEX.objectToText(n[e]) + "\nendobj\n";
					s = EcmaLEX.textToBytes(o), EcmaLEX.pushBytesToBuffer(s, EcmaMainData)
				}
				i += s.length
			}
			const r = EcmaMainData.length
				, s = document.querySelector("#FDFXFA_PDFID")
					.textContent
				, c = [...(new TextEncoder)
					.encode(EcmaFilter.encodeBase64(Date.now()
						.toString()))].map((e => e.toString(16)))
					.join("")
					.toUpperCase()
					.padStart(16, "0");
			if (EcmaXRefType) {
				let o = "[";
				const n = [];
				for (let e = 0, t = a.length; e < t; e++) {
					const t = a[e].ref
						, i = a[e].offset;
					n.push(1), EcmaLEX.pushBytesToBuffer(EcmaLEX.toBytes32(i), n), n.push(0), o += t + " 1 "
				}
				o += "]";
				const i = t;
				let l = `${i} 0 obj\n<</Type /XRef /Root ${EcmaMainCatalog.ref} /Prev ${e} /ID [<${s}> <${c}>] /Index ${o} /W [1 4 1] /Size ${i}/Length ${n.length}>>stream\n`;
				EcmaLEX.pushBytesToBuffer(EcmaLEX.textToBytes(l), EcmaMainData), EcmaLEX.pushBytesToBuffer(n, EcmaMainData), l = `\nendstream\nendobj\nstartxref\n${r}\n%%EOF`, EcmaLEX.pushBytesToBuffer(EcmaLEX.textToBytes(l), EcmaMainData)
			} else {
				EcmaLEX.pushBytesToBuffer([120, 114, 101, 102, 10], EcmaMainData);
				let o = "";
				for (let e = 0, t = a.length; e < t; e++) {
					const t = a[e].ref
						, n = a[e].offset;
					o += t + " 1\n" + EcmaLEX.getZeroLead(n) + " 00000 n \n"
				}
				o += `trailer\n<</Size ${t} /Root ${EcmaMainCatalog.ref} /Prev ${e} /ID [<${s}><${c}>]>>\n`, o += `startxref\n${r}\n%%EOF`, EcmaLEX.pushBytesToBuffer(EcmaLEX.textToBytes(o), EcmaMainData)
			}
		}
		, saveAnnotationToPDF: function(e, t) {
			this._updateFileInfo(e);
			var o = new EcmaBuffer(EcmaMainData)
				, n = o.findFirstXREFOffset();
			n && (o.movePos(n), o.readSimpleXREF()), o.updateAllObjStm(), o.updatePageOffsets();
			var i = 1;
			for (var a in EcmaOBJECTOFFSETS) {
				var r = a.split(" ");
				i = Math.max(parseInt(r[0]), i)
			}
			i++, this._saveAnnotObjects(e, n, i, t)
		}
		, _updateFileInfo: function(e) {
			EcmaNAMES = {}, EcmaOBJECTOFFSETS = {}, EcmaPageOffsets = [], EcmaMainCatalog = null, EcmaXRefType = 0;
			var t = document.getElementById("FDFXFA_PDFDump");
			if (t) EcmaMainData = EcmaFilter.decodeBase64(t.textContent);
			else {
				var o = new XMLHttpRequest;
				o.onreadystatechange = function() {
					if (EcmaMainData = [], 4 !== o.readyState || 200 !== o.status);
					else
						for (var e = o.responseText, t = 0, n = e.length; t < n; t++) EcmaMainData.push(255 & e.charCodeAt(t))
				}, o.open("GET", e, !1), o.overrideMimeType("text/plain; charset=x-user-defined"), o.send()
			}
		}
		, _saveAnnotObjects: function(e, t, o, n) {
			for (var i = o, a = EcmaMainData.length, r = [], s = {}, c = {}, l = new EcmaBuffer(EcmaMainData), d = 0, u = n.length; d < u; d++) {
				var h = n[d].page
					, f = "" + h
					, m = EcmaPageOffsets[h]
					, p;
				f in c ? p = c[f] : (p = l.getObjectValue(m), c[f] = p);
				var g = p.keys[EcmaKEY.Annots];
				s[f] = m.num, r.push({
					ref: i
					, offset: a
				});
				var y = i + " 0 obj\n" + n[d].getDictionaryString() + "\nendobj\n"
					, S = EcmaLEX.textToBytes(y);
				if (EcmaLEX.pushBytesToBuffer(S, EcmaMainData), g)
					if (EcmaLEX.isRef(g)) {
						var O = l.getObjectValue(g);
						if (EcmaLEX.isArray(O)) {
							p.keys[EcmaKEY.Annots] = [];
							for (var E = 0, I = O.length; E < I; E++) p.keys[EcmaKEY.Annots].push(O[E]);
							p.keys[EcmaKEY.Annots].push(new EcmaOREF(i, 0))
						} else p.keys[EcmaKEY.Annots] = [g], p.keys[EcmaKEY.Annots].push(new EcmaOREF(i, 0))
					} else EcmaLEX.isArray(g) ? g.push(new EcmaOREF(i, 0)) : p.keys[EcmaKEY.Annots] = [new EcmaOREF(i, 0)];
				else p.keys[EcmaKEY.Annots] = [new EcmaOREF(i, 0)];
				a += S.length, i++
			}
			var A = EcmaMainData.length;
			for (var D in s) {
				var b = s[D];
				s[D] = {
					ref: b
					, offset: A
				};
				var p = c[D]
					, v = b + " 0 obj\n" + EcmaLEX.objectToText(p) + "\nendobj\n"
					, S = EcmaLEX.textToBytes(v);
				EcmaLEX.pushBytesToBuffer(S, EcmaMainData), A = EcmaMainData.length
			}
			var T = EcmaMainData.length;
			EcmaXRefType ? this._generateStreamXREF(t, T, o, r, s) : this._generateSimpleXREF(t, T, o, r, s), this._openURL(e)
		}
		, _generateSimpleXREF: function(e, t, o, n, i) {
			EcmaLEX.pushBytesToBuffer([120, 114, 101, 102, 10], EcmaMainData);
			var a = "";
			for (var r in i) {
				var s = i[r].ref
					, c = i[r].offset;
				a += s + " 1\n" + EcmaLEX.getZeroLead(c) + " 00000 n \n"
			}
			var l = n.length
				, d;
			if (l) {
				a += o + " " + l + "\n";
				for (var u = 0, h = l; u < h; u++) a += EcmaLEX.getZeroLead(n[u].offset) + " 00000 n \n"
			}
			a += "trailer\n<</Size " + (o + l) + " /Root " + EcmaMainCatalog.ref + " /Prev " + e + ">>\n", a += "startxref\n" + t + "\n%%EOF", EcmaLEX.pushBytesToBuffer(EcmaLEX.textToBytes(a), EcmaMainData)
		}
		, _generateStreamXREF: function(e, t, o, n, i) {
			var a = n.length
				, r = "["
				, s = [];
			for (var c in i) {
				var l = i[c].ref
					, d = i[c].offset;
				s.push(1), EcmaLEX.pushBytesToBuffer(EcmaLEX.toBytes32(d), s), s.push(0), r += l + " 1 "
			}
			r += o + " " + a + "]";
			for (var u = 0; u < a; u++) {
				var d = n[u].offset;
				s.push(1), EcmaLEX.pushBytesToBuffer(EcmaLEX.toBytes32(d), s), s.push(0)
			}
			var h = o + a + 1
				, f = h + " 0 obj\n<</Type /XRef /Root " + EcmaMainCatalog.ref + " /Prev " + e + " /Index " + r + " /W [1 4 1] /Size " + h + "/Length " + s.length + ">>stream\n";
			EcmaLEX.pushBytesToBuffer(EcmaLEX.textToBytes(f), EcmaMainData), EcmaLEX.pushBytesToBuffer(s, EcmaMainData), f = "\nendstream\nendobj\nstartxref\n" + t + "\n%%EOF", EcmaLEX.pushBytesToBuffer(EcmaLEX.textToBytes(f), EcmaMainData)
		}
		, _openURL: function(e, t) {
			var o, n = "data:application/octet-stream;base64," + EcmaFilter.encodeBase64(t)
				, i = e
				, a = "" + navigator.userAgent;
			if (-1 !== a.indexOf("Edge") || -1 !== a.indexOf("MSIE ")) {
				for (var r = new ArrayBuffer(t.length), s = new Uint8Array(r), c = 0, l = t.length; c < l; c++) s[c] = 255 & t[c];
				var d = new Blob([r], {
					type: "application/octet-stream"
				});
				return window.navigator.msSaveBlob(d, i), void 0
			}
			var u = document.createElement("a");
			if (u.setAttribute("download", i), u.setAttribute("href", n), document.body.appendChild(u), "click" in u) u.click();
			else {
				var h = document.createEvent("MouseEvent");
				h.initEvent("click", !0, !0), u.dispatchEvent(h)
			}
			u.setAttribute("href", "")
		},
		getPDFBlob: function(e) {
					t = this._insertFieldsToPDF(e);
					for (var r = new ArrayBuffer(t.length), s = new Uint8Array(r), c = 0, l = t.length; c < l; c++) s[c] = 255 & t[c];
					var d = new Blob([r], {
						type: "application/octet-stream"
					});
					return d;
				}
		
	}
		, FONTNAMES = {
			ARIAL: "Arial"
			, HELVETICA: "Helvetica"
			, COURIER: "Courier"
		}
		, EcmaPDF = {
			hashKey: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/="
			, getDictionaryString: function(e, t) {
				for (var o = t, n = e.length; o < n && 60 !== e[o];) o++;
				var i = [1]
					, a = "<<";
				for (o += 2; 0 !== i.length && o < n;) {
					var r = e[o]
						, s = e[o + 1];
					60 === r && s && 60 === s ? (i.push(1), o += 2, a += "<<") : 62 === r && s && 62 === s ? (i.pop(), o += 2, a += ">>") : (a += String.fromCharCode(r), o++)
				}
				return a
			}
			, byteToString: function(e) {
				return String.fromCharCode(e)
			}
			, bytesToString: function(e) {
				for (var t = "", o = 0; o < e.length; o++) t += String.fromCharCode(parseInt(e[o]));
				return t
			}
			, writeBytes: function(e, t) {
				for (var o = 0; o < e.length; o++) {
					var n = e.charCodeAt(o);
					n < 128 ? t.push(n) : n < 2048 ? t.push(192 | n >> 6, 128 | 63 & n) : n < 55296 || n >= 57344 ? t.push(224 | n >> 12, 128 | n >> 6 & 63, 128 | 63 & n) : (o++, n = 65536 + ((1023 & n) << 10 | 1023 & e.charCodeAt(o)), t.push(240 | n >> 18, 128 | n >> 12 & 63, 128 | n >> 6 & 63, 128 | 63 & n))
				}
			}
			, encode64: function(e) {
				var t = ""
					, o, n, i, a, r, s, c, l = 0;
				for (e = this.encodeUTF8(e); l < e.length;) a = (o = e.charCodeAt(l++)) >> 2, r = (3 & o) << 4 | (n = e.charCodeAt(l++)) >> 4, s = (15 & n) << 2 | (i = e.charCodeAt(l++)) >> 6, c = 63 & i, isNaN(n) ? s = c = 64 : isNaN(i) && (c = 64), t += this.hashKey.charAt(a) + this.hashKey.charAt(r) + this.hashKey.charAt(s) + this.hashKey.charAt(c);
				return t
			}
			, decode64: function(e) {
				for (var t = "", o, n, i, a, r, s, c, l = 0, d = (e = e.replace(/[^A-Za-z0-9\+\/\=]/g, ""))
					.length; l < d;) o = (a = this.hashKey.indexOf(e.charAt(l++))) << 2 | (r = this.hashKey.indexOf(e.charAt(l++))) >> 4, n = (15 & r) << 4 | (s = this.hashKey.indexOf(e.charAt(l++))) >> 2, i = (3 & s) << 6 | (c = this.hashKey.indexOf(e.charAt(l++))), t += String.fromCharCode(o), 64 !== s && (t += String.fromCharCode(n)), 64 !== c && (t += String.fromCharCode(i));
				return t = this.decodeUTF8(t)
			}
			, encodeUTF8: function(e) {
				for (var t = "", o = 0, n = (e = e.replace(/\r\n/g, "\n"))
					.length; o < n; o++) {
					var i = e.charCodeAt(o);
					i < 128 ? t += String.fromCharCode(i) : i > 127 && i < 2048 ? (t += String.fromCharCode(i >> 6 | 192), t += String.fromCharCode(63 & i | 128)) : (t += String.fromCharCode(i >> 12 | 224), t += String.fromCharCode(i >> 6 & 63 | 128), t += String.fromCharCode(63 & i | 128))
				}
				return t
			}
			, decodeUTF8: function(e) {
				for (var t = "", o = 0, n = 0, i, a, r = e.length; o < r;)(n = e.charCodeAt(o)) < 128 ? (t += String.fromCharCode(n), o++) : n > 191 && n < 224 ? (i = e.charCodeAt(o + 1), t += String.fromCharCode((31 & n) << 6 | 63 & i), o += 2) : (i = e.charCodeAt(o + 1), a = e.charCodeAt(o + 2), t += String.fromCharCode((15 & n) << 12 | (63 & i) << 6 | 63 & a), o += 3);
				return t
			}
		};

	function PdfDocument() {
		for (var e in this._pages = new Array, this._xfaStreams = new Array, this._fontResources = new Array, FONTNAMES) {
			var t = "<</Type /Font /Subtype /Type1 /BaseFont /" + FONTNAMES[e] + ">>"
				, o = new PdfResource(FONTNAMES[e], t);
			this._fontResources.push(o)
		}
	}

	function PdfPage() {
		this._width = 612, this._height = 792, this._rotation = 0, this._texts = new Array, this._images = new Array
	}

	function PdfText(e, t, o, n, i) {
		this._x = e, this._y = t, this._fontName;
		var a = o.toUpperCase();
		this._fontName = a in FONTNAMES ? FONTNAMES[a] : FONTNAMES.ARIAL, this._fontSize = n, this._fontText = i
	}

	function PdfImage(e, t, o) {
		this._x = e, this._y = t, this._imageData = o
	}

	function PdfResource(e, t) {
		this._name = e, this._stream = t
	}

	function XFAStream(e, t) {
		this._name = e, this._data = t
	}

	function getObjStart(e) {
		return e + " 0 obj"
	}

	function getObjRef(e) {
		return e + " 0 R"
	}

	function getCatalogString(e) {
		return getObjStart(e) + " <</Type /Catalog /Pages " + getObjRef(e + 1) + ">>\nendobj\n"
	}

	function getStructTreeString(e) {
		return getObjStart(e) + " <</Type /StructTreeRoot>>\nendobj\n"
	}

	function getXFACatalogString(e, t, o) {
		return getObjStart(e) + " <</NeedsRendering true/AcroForm " + getObjRef(t) + "/Extensions<</ADBE<</BaseVersion/1.7/ExtensionLevel 5>>>>/MarkInfo<</Marked true>>/Type /Catalog /Pages " + getObjRef(e + 1) + ">>\nendobj\n"
	}

	function getPageTreeString(e, t) {
		for (var o = getObjStart(e) + " <</Type /Pages /Kids [ ", n = e + 1, i = 0; i < t; i++) o += getObjRef(i + n) + " ";
		return o += "] /Count " + t + ">>\nendobj\n"
	}

	function getPageString(e, t, o, n, i) {
		return getObjStart(e) + " <</Type /Page /Parent " + getObjRef(t) + " /Resources " + getObjRef(o) + " /Contents " + getObjRef(n) + " /MediaBox [0 0 " + i._width + " " + i._height + "] /Rotate " + i._rotation + ">>\nendobj\n"
	}

	function getContentString(e, t) {
		for (var o = "", n = t._texts.length, i = 0; i < n; i++) {
			var a = t._texts[i];
			o += "BT /" + a._fontName + " " + a._fontSize + " Tf " + a._x + " " + a._y + " Td (" + a._fontText + ")Tj ET\n"
		}
		var r = new Array;
		return EcmaPDF.writeBytes(o, r), getObjStart(e) + " <</Length " + r.length + ">>\nstream\n" + o + "\nendstream\nendobj\n"
	}

	function getResourceString(e, t, o) {
		for (var n = getObjStart(e) + " <</Font <<", i = 0; i < t; i++) {
			var a;
			n += "/" + o._fontResources[i]._name + " " + getObjRef(e + 1 + i) + " "
		}
		return n += ">> >>\nendObj\n"
	}

	function getFontDefString(e, t) {
		return getObjStart(e) + t._stream + "\nendobj\n"
	}

	function getZeroLead(e) {
		for (var t = "" + e, o = 10 - t.length, n = 0; n < o; n++) t = "0" + t;
		return t
	}

	function getXrefString(e) {
		for (var t = e.length, o = "xref\n0 " + (t + 1) + "\n0000000000 65535 f \n", n = 0; n < t; n++) o += getZeroLead(e[n]) + " 00000 n \n";
		return o
	}

	function getXFADefinitionString(e, t) {
		return getObjStart(e) + "\n<</XFA " + getObjRef(t) + "/Fields[]>>\nendobj\n"
	}

	function getXFATemplateString(e, t, o) {
		return getObjStart(e) + "\n<</Length " + t + ">>stream\n" + o + "\nendstream\nendobj\n"
	}
	PdfDocument.prototype.addPage = function(e) {
		this._pages.push(e)
	}, PdfDocument.prototype.addXFAStream = function(e) {
		this._xfaStreams.push(e)
	}, PdfPage.prototype.setWidth = function(e) {
		this._width = e
	}, PdfPage.prototype.setHeight = function(e) {
		this._height = e
	}, PdfPage.prototype.addText = function(e) {
		e._y = this._height - e._y - e._fontSize, this._texts.push(e)
	}, PdfPage.prototype.setRotation = function(e) {
		this._rotation = e
	}, PdfPage.prototype.addImage = function(e) {
		this._images.push(e)
	}, PdfDocument.prototype.createPdfString = function(e) {
		var t = new Array
			, o = new Array
			, n = this._pages.length
			, i = 1
			, a = 2
			, r = 3
			, s = 3 + n
			, c = s + n
			, l = c + 1
			, d = this._fontResources.length
			, u = l + d
			, h = u;
		EcmaPDF.writeBytes("%PDF-1.7\n", o);
		var f = null;
		f = e ? getXFACatalogString(1, h, u) : getCatalogString(1), t.push(o.length), EcmaPDF.writeBytes(f, o), f = getPageTreeString(2, n), t.push(o.length), EcmaPDF.writeBytes(f, o);
		for (var m = 0; m < n; m++) {
			var p, g, y;
			f = getPageString(3 + m, 2, c, y = s + m, p = this._pages[m]), t.push(o.length), EcmaPDF.writeBytes(f, o)
		}
		for (var m = 0; m < n; m++) {
			var p, y;
			f = getContentString(y = s + m, p = this._pages[m]), t.push(o.length), EcmaPDF.writeBytes(f, o)
		}
		f = getResourceString(c, d, this), t.push(o.length), EcmaPDF.writeBytes(f, o);
		for (var m = 0; m < d; m++) f = getFontDefString(l + m, this._fontResources[m]), t.push(o.length), EcmaPDF.writeBytes(f, o);
		if (e) {
			var S = h + 1;
			f = getXFADefinitionString(h, S), t.push(o.length), EcmaPDF.writeBytes(f, o);
			var O = new Array;
			EcmaPDF.writeBytes(e, O), f = getXFATemplateString(S, O.length, e), t.push(o.length), EcmaPDF.writeBytes(f, o)
		}
		var E = o.length;
		return f = getXrefString(t), EcmaPDF.writeBytes(f, o), f = "trailer <</Size " + (t.length + 1) + " /Root 1 0 R>>\nstartxref\n" + E + "\n%%EOF", EcmaPDF.writeBytes(f, o), EcmaPDF.bytesToString(o)
	}
}
var app = idrform.app;
