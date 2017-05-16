/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Todd Blanchard 2010
 * Copyright (c) 2010, iNET Inc.
 */

var classname = "ZebraLink";
var exec = require('cordova/exec');
var ZebraLink = {
  discover: function (success, fail, options) {
    console.log("ZebraLink.prototype.discover");

    if (!fail) {
      fail = function () {};
    }

    if (typeof fail != "function") {
      console.log("ZebraLink.discover failure: failure parameter not a function");
      return;
    }

    if (typeof success != "function") {
      fail("success callback parameter must be a function");
      return;
    }

    if (!options || !options.range) {
      options = {
        range: "172.20.10.*"
      };
    }

    setTimeout(function () {
      //progress('Finding Printers...');
    }, 500);

    return exec(success, fail, classname, "discover", [options]);

  },

  connect: function (success, fail, options) {
    console.log("ZebraLink.prototype.connect");

    if (!fail) {
      fail = function (error) {
        alert("ZebraLink.Connect failed: " + error);
      };
    }

    if (typeof fail != "function") {
      console.log("ZebraLink.connect failure: failure parameter not a function");
      return;
    }

    if (typeof success != "function") {
      fail("success callback parameter must be a function");
      return;
    }

    var label = options.address;
    if (options.printer && options.printer.name) {
      label = options.printer.name;
    }

    return exec(success, fail, classname, "connect", [options]);

  },

  test: function (success, fail, options) {
    console.log("ZebraLink.prototype.connect");

    if (!fail) {
      fail = function (error) {
        alert("ZebraLink.Connect failed: " + error);
      };
    }

    if (typeof fail != "function") {
      console.log("ZebraLink.connect failure: failure parameter not a function");
      return;
    }

    if (typeof success != "function") {
      fail("success callback parameter must be a function");
      return;
    }

    var label = options.address;
    if (options.printer && options.printer.name) {
      label = options.printer.name;
    }

    return exec(success, fail, classname, "test", [options]);

  },

  reconnect: function (success, fail, options) {
    console.log("ZebraLink.prototype.disconnect");

    if (!fail) {
      fail = function (error) {
        console.log('ZebraLink.reconnect failed: ' + error);
      };
    }

    if (!success) {
      success = function () {};
    }

    if (!options) {
      options = {};
    }

    return exec(success, fail, classname, "reconnect", [options]);

  },

  print: function (success, fail, options) {
    if (!fail) {
      fail = function () {};
    }

    if (typeof fail != "function") {
      console.log("ZebraLink.print failure: failure parameter not a function");
      return;
    }

    if (typeof success != "function") {
      fail("success callback parameter must be a function");
      return;
    }


    if (this.platform() !== 'droid') {
      alert('Print: ' + JSON.stringify(options));
      setTimeout(success, 400);
    } else {
      return exec(success, fail, classname, "print", [options]);
    }
  },


  disconnect: function (success, fail, options) {
    console.log("ZebraLink.prototype.disconnect");

    if (!fail) {
      fail = function (error) {
        console.log('ZebraLink.disconnect failed: ' + error);
      };
    }

    if (!success) {
      success = function () {};
    }

    if (!options) {
      options = {};
    }

    // super skanky but until I figure out why PG doesn't call me back this works around the problem.
    setTimeout(function () {
      success();
    }, 500);

    return exec(function () {}, fail, classname, "disconnect", [options]);
  },

  watchPrinter: function (watchFunc) {
    if (!this._watchers) {
      this._watchers = {};
      this._watchIds = 1;
    }
    var ident = 'watch' + this._watchIds++;
    this._watchers[ident] = watchFunc;
  },

  cancelPrinterWatch: function (identifier) {
    if (this._watchers && this._watchers.identifier) {
      this._watchers.identifier = null;
    }
  },

  setPrinterStatus: function (status) {
    this._status = status;
    if (this._watchers) {
      for (var k in this._watchers) {
        if (this._watchers.hasOwnProperty(k) && typeof (this._watchers[k]) == 'function') {
          this._watchers[k](status);
        }
      }
    }
  },

  platform: function () {

    if (navigator.userAgent.match(/Android/i)) {
      return "droid";
    }
    if ((navigator.userAgent.match(/iPhone/i)) || (navigator.userAgent.match(/iPad/i)) || (navigator.userAgent.match(/iPod/i))) // iPhone, iPod, iPad
    {
      return "ios";
    }
    alert("Platform detection failed - please file a bug");
  },

  check: function (success, fail) {
    if (!fail) {
      fail = function (msg) {
        setTimeout(function () {
          alert("Printer not ready: " + msg);
        }, 1);
      };
    }

    if (!success) {
      success = function () {
        setTimeout(function () {
          alert("Printer is ready.");
        }, 1);
      };
    }

    if (platform() !== 'droid') {
      if (confirm("Check: Printer is OK?")) {
        setTimeout(function () {
          success();
        }, 1000);
      } else {
        setTimeout(function () {
          fail('printer connection failed.');
        }, 1000);
      }
    } else {
      return exec(success, fail, classname, "check", []);
    }
  }
}
module.exports = ZebraLink;
