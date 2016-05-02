var exec = require('cordova/exec');

//-------------------------------------------------------------------
var ZebraLink = function() {
};

ZebraLink.prototype.pgclass = function() { return "ZebraLink"; };

//-------------------------------------------------------------------
ZebraLink.prototype.discover = function(success, fail, options) {

	console.log("ZebraLink.prototype.discover");

    if (!fail) { fail = function() {};}

    if (typeof fail != "function")  {
        console.log("ZebraLink.discover failure: failure parameter not a function");
        return;
    }

    if (typeof success != "function") {
        fail("success callback parameter must be a function");
        return;
    }

	if(!options || !options.range)
	{
		options = {range: "172.20.10.*"};
	}

    return cordova.exec(
		success,fail, this.pgclass(), "discover", [options]);
};

//-------------------------------------------------------------------
ZebraLink.prototype.connect = function(success, fail, options) {

	console.log("ZebraLink.prototype.connect");

    if (!fail) { fail = function(error) { alert("ZebraLink.Connect failed: " + error); }; }

    if (typeof fail != "function")  {
        console.log("ZebraLink.connect failure: failure parameter not a function");
        return;
    }

    if (typeof success != "function") {
        fail("success callback parameter must be a function");
        return;
    }

	var label = options.address;
	if(options.printer && options.printer.name) { label = options.printer.name; }

    return cordova.exec(
		success,fail,
		this.pgclass(), "connect", [options]);
};

ZebraLink.prototype.reconnect = function(success,fail,options)
{
	console.log("ZebraLink.prototype.disconnect");

    if (!fail) { fail = function(error) { console.log('ZebraLink.reconnect failed: ' + error);};}

	if(!success) { success = function() {}; }
	
	if(!options) { options = {}; }
	
	return cordova.exec(success, fail, this.pgclass(), "connect", [options]);
};

ZebraLink.prototype.disconnect = function(success, fail, options)
{
	console.log("ZebraLink.prototype.disconnect");

    if (!fail) { fail = function(error) { console.log('ZebraLink.disconnect failed: ' + error) }}

	if(!success) { success = function() {} }

	if(!options) { options = {}; }
	
	// super skanky but until I figure out why PG doesn't call me back this works around the problem.
	setTimeout(function(){success();},500);
	
	return cordova.exec(function(){}, fail, this.pgclass(), "disconnect", [options]);
};

ZebraLink.prototype.print = function(success,fail,options) 
{
    if (!fail) { fail = function() {}}

    if (typeof fail != "function")  {
        console.log("ZebraLink.print failure: failure parameter not a function")
        return
    }

    if (typeof success != "function") {
        fail("success callback parameter must be a function")
        return
    }
	
    return cordova.exec(success,fail,
		this.pgclass(), "print", [options])
	
};



ZebraLink.prototype.swipe = function(success,fail,options)
{
    if (!fail) { fail = function() {};}

    if (typeof fail != "function")  {
        console.log("ZebraLink.currentPrinter failure: failure parameter not a function");
        return;
    }

    if (typeof success != "function") {
        fail("success callback parameter must be a function");
        return;
    }

	if(!options) { options = {} };
	if(!options.timeout) { options.timeout = 20000; }

    return cordova.exec(success,fail,
		this.pgclass(), "swipe", [options])
	
};


ZebraLink.prototype.watchPrinter = function(watchFunc)
{
	if(!this._watchers)
	{
		this._watchers = {};
		this._watchIds = 1;
	}
	var ident = 'watch'+this._watchIds++;
	this._watchers[ident] = watchFunc;
};

ZebraLink.prototype.cancelPrinterWatch = function(identifier)
{
	if(this._watchers && this._watchers.identifier)
	{
		this._watchers.identifier = null;
	}
};

ZebraLink.prototype.setPrinterStatus = function(status)
{
	this._status = status;
	if(this._watchers)
	{
		for(var k in this._watchers)
		{
			if(this._watchers.hasOwnProperty(k) && typeof(this._watchers[k]) == 'function')
			{
				this._watchers[k](status);
			}
		}
	}
};

ZebraLink.prototype.check = function(success,fail)
{
	if(!fail)
	{
		fail = function(msg) { setTimeout(function(){ alert("Printer not ready: "+msg); },1) }
	}
	
	if(!success)
	{
		success = function() { setTimeout(function(){alert("Printer is ready.");},1); }
	}
	
	return cordova.exec(success,fail,
		this.pgclass(), "check", []);
};

//-------------------------------------------------------------------


document.addEventListener('deviceready', function() {

    if (!window.plugins) window.plugins = {};

    if (!window.plugins.zebralink) {
        window.plugins.zebralink = new ZebraLink();
    }
    else {
        console.log("Not installing zebraLink: window.plugins.zebraLink already exists");
    }
}, false);


//})();