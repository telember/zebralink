# zebralink
A Cordova/Phonegap plugin for Zebra bluetooth printers

##Usage
You can send data in ZPL Zebra Programing Language:

###Discover Printers
Search for Zebra bluetooth printer devices.  
```
cordova.plugins.zebralink.discover = function(
	success(printers)
	{
		alert('Found Printers: ' + JSON.stringify(printers));
	}, 
	fail(error)
	{
		alert('Discover failed ' + JSON.stringify(error));
	}, 
	options={range: "172.20.10.*"});


```
###Connect
```
cordova.plugins.zebralink.connect(success(){},fail(error){},options = {printer: printer});
```

###Disconnect
```
cordova.plugins.zebralink.disconnect(success(){},fail(error){});
```

###Print
Expects a template and optionally a formValues dictionary of values to substitute into the template. Template variables look like @variable@.

```
cordova.plugins.zebralink.print(success(){},fail(error){},options{template: "^XA^FO10,10^AFN,26,13^FD@message@^FS^XZ" formValues: {message: "Hello, World!"}});
```


##Install
###Cordova

```
cordova plugin add https://github.com/tblanchard/zebralink.git
```

###Very important!


##ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)
