// Runs before the AETHER page in the Android app. Android's WebView has no speech features,
// so this provides the standard speechSynthesis and SpeechRecognition on top of the phone's own
// text-to-speech and speech recognition (window.AetherNative). The web app code stays the same.
(function () {
  if (!window.AetherNative || window.__aetherBridge) return;
  window.__aetherBridge = true;

  // ---------- speaking ----------
  var utterances = {}, uttCount = 0;
  function fire(target, type, ev) {
    try { if (typeof target["on" + type] === "function") target["on" + type](ev); } catch (e) {}
    (target._listeners && target._listeners[type] || []).forEach(function (f) { try { f(ev); } catch (e) {} });
  }
  function on(type, f) { (this._listeners[type] = this._listeners[type] || []).push(f); }

  function Utterance(text) {
    this.text = text || ""; this.lang = "en-US"; this.rate = 1; this.pitch = 1; this.volume = 1; this.voice = null;
    this.onstart = null; this.onend = null; this.onerror = null; this._listeners = {};
  }
  Utterance.prototype.addEventListener = on;
  Utterance.prototype.removeEventListener = function () {};
  window.SpeechSynthesisUtterance = Utterance;

  var synth = {
    speaking: false, pending: false, paused: false, _listeners: {},
    speak: function (u) {
      var id = "u" + (++uttCount);
      utterances[id] = u;
      synth.speaking = true;
      fire(u, "start", {});
      AetherNative.speak(String(u.text || ""), Number(u.rate) || 1, id);
    },
    cancel: function () { utterances = {}; synth.speaking = false; AetherNative.stopSpeaking(); },
    pause: function () {}, resume: function () {},
    getVoices: function () { return []; },
    addEventListener: on, removeEventListener: function () {}
  };
  window.__aetherTtsDone = function (id) {
    var u = utterances[id];
    delete utterances[id];
    synth.speaking = Object.keys(utterances).length > 0;
    if (u) fire(u, "end", {});
  };
  try { Object.defineProperty(window, "speechSynthesis", { value: synth, configurable: true }); } catch (e) { window.speechSynthesis = synth; }

  // ---------- listening ----------
  var recognizers = {}, recCount = 0;
  function Recognition() {
    this.lang = "en-US"; this.continuous = false; this.interimResults = false; this.maxAlternatives = 1;
    this.onstart = this.onresult = this.onerror = this.onend = null; this._listeners = {};
  }
  Recognition.prototype.addEventListener = on;
  Recognition.prototype.removeEventListener = function () {};
  Recognition.prototype.start = function () {
    this._id = "r" + (++recCount);
    this._stopped = false;
    this._finals = [];
    recognizers[this._id] = this;
    fire(this, "start", {});
    AetherNative.startListening(String(this.lang || "en-US"), !!this.interimResults || !!this.continuous, this._id);
  };
  Recognition.prototype.stop = Recognition.prototype.abort = function () {
    this._stopped = true;
    AetherNative.stopListening();
  };
  function result(text, isFinal) { var r = [{ transcript: text, confidence: 0.9 }]; r.isFinal = isFinal; r.item = function (i) { return r[i]; }; return r; }

  window.__aetherSR = function (id, ev) {
    var r = recognizers[id];
    if (!r) return;
    if (ev.type === "partial" || ev.type === "final") {
      if (ev.type === "final") r._finals.push(result(ev.text, true));
      var results = r._finals.slice();
      if (ev.type === "partial") results.push(result(ev.text, false));
      fire(r, "result", { results: results, resultIndex: results.length - 1 });
    } else if (ev.type === "error") {
      if (r.continuous && !r._stopped && ev.error === "no-speech") return; // keep listening
      fire(r, "error", { error: ev.error });
    } else if (ev.type === "end") {
      if (r.continuous && !r._stopped) {
        AetherNative.startListening(String(r.lang || "en-US"), true, id); // keep going until stop()
        return;
      }
      delete recognizers[id];
      fire(r, "end", {});
    }
  };
  window.SpeechRecognition = Recognition;
  window.webkitSpeechRecognition = Recognition;
})();
