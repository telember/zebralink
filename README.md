# zebralink
A Cordova/Phonegap plugin for Zebra bluetooth printers
### original form https://github.com/tblanchard/zebralink

## Install
### Ionic2

```
ionic plugin add https://github.com/telember/zebralink.git
``` 

## Usage
You can send data in ZPL Zebra Programing Language:

### simple
```
declare var ZebraLink: any

export class MyApp {
  constructor(private platform: Platform) {
   	this.platform.ready().then(() => {
		if (this.platform.is('cordova')) {	
		        let zplData = "^XA^FO20,20^A0N,25,25^FDThis is a ZPL test.^FS^XZ ";
			ZebraLink.test(function (result) {
        			console.log(result)
        		}, function (err) {
        			console.log(err)
        		}, { address: "AC:3F:A4:5D:7B:24", message: zplData })
		
		}
	})
  }
}
```


### Discover Printers
Discover for Zebra bluetooth printer devices.  
```
ZebraLink.discover = function(
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
### Connect
Options should contain 'printer' set to one of the printers returned from discover.
```
ZebraLink.connect(success(){},fail(error){},options = {printer: printer});
```

### Disconnect
Disconnect the current printer.
```
ZebraLink.disconnect(success(){},fail(error){});
```

### Print
Expects a template and optionally a formValues dictionary of values to substitute into the template. Template variables look like @variable@.
```
ZebraLink.print(success(){},fail(error){},options{template: "^XA^FO10,10^AFN,26,13^FD@message@^FS^XZ", formValues: {message: "Hello, World!"}});
```

### Swipe
Activates the printer's mag card reader until it receives a swipe or times out.  Default timeout is 20 seconds (timeout specified in ms).
```
ZebraLink.swipe(success(){},fail(error){},options={timeout: 20000});
```



##ZPL - Zebra Programming Language
For more information about ZPL please see the  [PDF Official Manual](https://support.zebra.com/cpws/docs/zpl/zpl_manual.pdf)
