package com.apache.cordova.plugins.zebra;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.Looper;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.BluetoothConnectionInsecure;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.discovery.DeviceFilter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.device.MagCardReaderFactory;
import com.zebra.sdk.device.MagCardReader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;



public class ZebraLink extends CordovaPlugin {

	public class ZebraLinkException extends Exception
	{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public ZebraLinkException(String msg) { super(msg); }
	}
	
	public class NSError
	{
		public String message = null;
		public boolean isOK() { return message == null; }
	}
	
	static String lastPrinterStatus;
	static JSONArray lastDiscoveredPrinters;
	static BluetoothConnection printerConnection;
	static String printerAddress;
	static Thread swiperThread;
	
	static final String lock = "ZebraLinkLock";
	
	static final String DISCOVER = "discover";
	static final String CONNECT = "connect";
	static final String DISCONNECT = "disconnect";
	static final String SWIPE = "swipe";
	static final String PRINT = "print";
	static final String CHECK = "check";
	
	@Override
	public PluginResult execute(String action, JSONArray inargs, String cid) 
	{				
		System.err.println("ZebraLink." + action + "("+cid+")");
		
		PluginResult async = new PluginResult(PluginResult.Status.NO_RESULT,"");
		async.setKeepCallback(true);
		
		if(action.equals(DISCONNECT)) // run synchronously
		{
			try
			{
				this.disconnect(inargs,cid);
			}
			catch(Throwable t)
			{
				t.printStackTrace();
			}
			return async;
		}
		
		
		
		if(action.equals(DISCOVER))
		{
			class Discover implements Runnable
			{
				ZebraLink z;
				JSONArray arguments;
				String callbackId;
			
				public Discover(ZebraLink z, JSONArray args, String cbid) { this.z = z; this.arguments = args; this.callbackId = cbid; }
				public void run() { Looper.prepare(); z.discover(this.arguments,this.callbackId); Looper.myLooper().quit(); } 
				
			}
		
			new Discover(this,inargs,cid).run();	
			//new Thread(new Discover(this,inargs,cid)).start();
			return async;
		}
		if (action.equals(CONNECT))
		{
			class Connect implements Runnable
			{
				ZebraLink z;
				JSONArray arguments;
				String callbackId;
			
				public Connect(ZebraLink z, JSONArray args, String cbid) { this.z = z; this.arguments = args; this.callbackId = cbid; }
				public void run() { Looper.prepare(); z.connect(this.arguments,this.callbackId); Looper.myLooper().quit(); } 
				
			}
			//this.connect(inargs,cid);
			new Connect(this,inargs,cid).run();
			//this.test(inargs,cid);
			return async;
		}
		if(action.equals(SWIPE))
		{			
			class Swipe implements Runnable
			{
				ZebraLink z;
				JSONArray arguments;
				String callbackId;
			
				public Swipe(ZebraLink z, JSONArray args, String cbid) { this.z = z; this.arguments = args; this.callbackId = cbid; }
				public void run() { z.swipe(this.arguments,this.callbackId); } 
			}
			if(swiperThread == null)
			{
				//new Swipe(this,inargs,cid).run();
				swiperThread = new Thread(new Swipe(this, inargs, cid));
				swiperThread.start();
			}
			return async;
		}
		if (action.equals(PRINT))
		{
			class Print implements Runnable
			{
				ZebraLink z;
				JSONArray arguments;
				String callbackId;
			
				public Print(ZebraLink z, JSONArray args, String cbid) { this.z = z; this.arguments = args; this.callbackId = cbid; }
				public void run() {z.print(this.arguments,this.callbackId); }
			}
			new Print(this,inargs,cid).run();
			//new Thread(new Print(this,inargs,cid)).start();
			return async;
		}
		if (action.equals(CHECK))
		{
			class Check implements Runnable
			{
				ZebraLink z;
				JSONArray arguments;
				String callbackId;
			
				public Check(ZebraLink z, JSONArray args, String cbid) { this.z = z; this.arguments = args; this.callbackId = cbid; }
				public void run() {z.check(this.arguments,this.callbackId); }
			}

			new Check(this,inargs,cid).run();	
			//new Thread(new Check(this,inargs,cid)).start();	
			return async;
		}

		// TODO Auto-generated method stub
		async = new PluginResult(PluginResult.Status.ERROR,"No Such Method: "+action);
		
		return async;
	}
		
	
	public void check(JSONArray arguments, String cid) 
	{
		String resultString = new PluginResult(PluginResult.Status.OK, "Success").toSuccessCallbackString(cid);
		synchronized(ZebraLink.lock)
		{
			try
			{
		        this.printerIsConnectedAndReady();
		    }
		    catch(Exception ex)
		    {
		        resultString = new PluginResult(PluginResult.Status.ERROR, ex.getLocalizedMessage()).toErrorCallbackString(cid);
		    }
		}
	    this.sendJavascript(resultString );
	}
	
	// utility function
	public JSONObject printerStatusAsDictionary(PrinterStatus status)
	{
		JSONObject dict = new JSONObject();
		try
		{
			dict.put("ready", status.isReadyToPrint);
			dict.put("open", status.isHeadOpen);
			dict.put("cold", status.isHeadCold);
			dict.put("too_hot", status.isHeadTooHot);
			dict.put("paper_out", status.isPaperOut);
			dict.put("ribbon_out", status.isRibbonOut);
			dict.put("buffer_full", status.isReceiveBufferFull);
			dict.put("paused", status.isPaused);
			dict.put("partial_format_in_progress", status.isPartialFormatInProgress);
			dict.put("labels_remaining_in_batch", status.labelsRemainingInBatch);
			dict.put("label_length_in_dots", status.labelLengthInDots);
			dict.put("number_of_formats_in_receive_buffer", status.numberOfFormatsInReceiveBuffer);
		}
		catch(JSONException ex)
		{
			System.err.println(""+ex);
			ex.printStackTrace(System.err);
		}
	    return dict;
	}


	public String printerStatusMessageForStatus(JSONObject dict)
	{
	    if(dict == null)
	    {
	        return "Printer Not Responding";
	    }
	    String msg = "";
	    String json = dict.toString();
	    
	    System.err.println(json);
	    
	    if(dict.optBoolean("ready"))
	    {
	        msg = "Printer Ready";
	    }
	    else if(dict.optString("error") != null && dict.optString("error").trim().length() > 0)
	    {
	    		msg = dict.optString("error");
	    }
	    else if(dict.optBoolean("open"))
	    {
	        msg = "Printer Door Open";
	    }
	    else if(dict.optBoolean( "paper_out"))
	    {
	        msg = "Printer Out Of Paper";
	    }
	    else if(dict.optBoolean( "ribbon_out"))
	    {
	        msg = "Printer Ribbon Out";
	    }
	    else if(dict.optBoolean( "buffer_full"))
	    {
	        msg = "Printer Buffer Full";
	    }
	    else if(dict.optBoolean( "too_hot"))
	    {
	        msg = "Printer Too Hot";
	    }
	    else if(dict.optBoolean( "cold"))
	    {
	        msg = "Printer Warming Up";
	    }
	    else if(dict.optBoolean("paused"))
	    {
	        msg = "Printer Is Paused";
	    }
	    else
	    {
	        msg = "Check Printer";
	    }
	    return msg;
	}
	
	public boolean printerIsConnectedAndReady() throws ZebraLinkException
	{
	    // this should only be called within an @synchronized(lock) block
		// in a background thread
		// if the connection is closed - open it
		if((ZebraLink.printerConnection == null || !ZebraLink.printerConnection.isConnected()) && ZebraLink.printerAddress != null)
		{
			ZebraLink.printerConnection = new BluetoothConnectionInsecure(ZebraLink.printerAddress);
            try
            {
            		ZebraLink.printerConnection.open();
            }
            catch(Exception ex)
            {
            		// open failed - clean up
            		try {
						ZebraLink.printerConnection.close();
					} catch (ConnectionException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
            		ZebraLink.printerConnection = null;
            }
		}		
		
		// no connection and we couldn't open one
		if(ZebraLink.printerConnection == null)
		{
			throw new ZebraLinkException("Printer Not Responding");
		}
		
	    JSONObject dict = this.getStatusForPrinterConnection(ZebraLink.printerConnection);

	    if(dict != null)
        {
            if(dict.optBoolean("ready"))
            {
                return true;
            }  
            throw new ZebraLinkException(this.printerStatusMessageForStatus(dict));
        }
        
	    // close the connection - it is broken
	    try {
			ZebraLink.printerConnection.close();
		} catch (ConnectionException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		ZebraLink.printerConnection = null;
		
    		throw new ZebraLinkException("Printer Not Responding");
	}
	
	void watchPrinterConnection(BluetoothConnection connection)
	{
	    while(connection == ZebraLink.printerConnection)
	    {
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
	        NSError error = null;
	        JSONObject dict = null;
	        
	        synchronized(ZebraLink.lock)
	        {
	            dict = this.getStatusForPrinterConnection(connection);
	        
	            if(dict != null && dict.optString("error") == null)
	            {
	                String msg = this.printerStatusMessageForStatus(dict);
	                
	                if(!ZebraLink.lastPrinterStatus.equals(msg))
	                {
	                    ZebraLink.lastPrinterStatus = msg;
	                    String alert = "plugins.zebralink.setPrinterStatus('"+msg+"');";
	                    System.err.println("PRINTER STATUS: "+alert);
	                    this.sendJavascript(alert );
	                }
	                
	            }
	            else
	            {
	                System.err.println("Printer Sanity Gone - reconnecting..."+error);
	                try {
						connection.close();
					} catch (ConnectionException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
	                try
	                {
		                connection = new BluetoothConnectionInsecure(ZebraLink.printerAddress);
		                ZebraLink.printerConnection = connection;
		                connection.open();
		                dict = this.getStatusForPrinterConnection(connection);
		                    
		    	            if(dict != null && dict.optString("error") == null)
		    	            {
		    	                String msg = this.printerStatusMessageForStatus(dict);
		    	                
		    	                if(!ZebraLink.lastPrinterStatus.equals(msg))
		    	                {
		    	                	ZebraLink.lastPrinterStatus = msg;
		    	                    String alert = "plugins.zebralink.setPrinterStatus('"+msg+"');";
		    	                    System.err.println("PRINTER STATUS: "+alert);
		    	                    this.sendJavascript(alert);
		    	                }		
		                }
		                else
		                {
		                    break;
		                }
	                }
	                catch(Exception ex)
	                {
	                		// should fall out of the loop anyhow
	                		break;
	                }
	            }
	        }
	    }
	    System.err.println("Printer Connection Lost");
	    ZebraLink.lastPrinterStatus = "Not Connected";
	    String alert = "plugins.zebralink.setPrinterStatus('"+ZebraLink.lastPrinterStatus+"');";
	    this.sendJavascript(alert);
	    // fall out and terminate watch
	}

	public void discover(JSONArray arguments, String cid)
	{
		final ZebraLink self = this;
		String resultString = null;
		
		class BTDiscoveryHandler implements DiscoveryHandler
		{
			ZebraLink self;
			JSONArray printers = new JSONArray();
			JSONArray pairedPrinters;
			String cid;
			
			public BTDiscoveryHandler(ZebraLink s, JSONArray paired, String cid) { self = s; this.cid = cid; this.pairedPrinters = paired; }
			
            public void discoveryError(String message) 
            {
            		String msg = new PluginResult(PluginResult.Status.ERROR, message).toErrorCallbackString(this.cid);
            		self.sendJavascript(msg);
            }

            public void discoveryFinished() 
            {
            		String r = new PluginResult(PluginResult.Status.OK, printers).toSuccessCallbackString(this.cid);
            		self.sendJavascript(r);
            }

			public void foundPrinter(DiscoveredPrinter printer) 
			{
				DiscoveredPrinterBluetooth pr = (DiscoveredPrinterBluetooth) printer;
				
				try 
				{
                    Map<String,String> map = pr.getDiscoveryDataMap();

                    for (String settingsKey : map.keySet()) {
                        System.out.println("Key: " + settingsKey + " Value: " + printer.getDiscoveryDataMap().get(settingsKey));
                    }

					String name = pr.friendlyName;
					String mac = pr.address;
					JSONObject p = new JSONObject();
					p.put("name",name);
					p.put("address", mac);
                    for (String settingsKey : map.keySet()) {
                        System.out.println("Key: " + settingsKey + " Value: " + map.get(settingsKey));
                        p.put(settingsKey,map.get(settingsKey));
                    }
                    /*
					switch(device.getBondState())
					{
						case BluetoothDevice.BOND_BONDED:
						{
							p.put("bond_state", "BOND_BONDED");
							break;
						}
						case BluetoothDevice.BOND_BONDING:
						{
							p.put("bond_state", "BOND_BONDING");
							break;
						}
						case BluetoothDevice.BOND_NONE:
						{
							p.put("bond_state ", "BOND_NONE");
							break;
						}
						default: 
						{
							p.put("bond_state ", "BOND_NONE");
							break;
						}
					}
					*/
					printers.put(p);

					for(int i = 0; i < pairedPrinters.length(); ++i)
					{
						
						JSONObject paired = pairedPrinters.getJSONObject(i);
						if(p.getString("address").equals(paired.optString("address")))
						{
							printers.put(paired);
							break;
						}
					}

				} catch (Exception e)
				{

				}
			}
        };
		
		try 
		{
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			Set<BluetoothDevice> devices = adapter.getBondedDevices();
			//adapter.cancelDiscovery();
			
			JSONArray array = new JSONArray();
			
			for(Iterator<BluetoothDevice> it = devices.iterator(); it.hasNext();)
			{
				BluetoothDevice device = it.next();
				BluetoothClass cls = device.getBluetoothClass();
				String name = device.getName();
				String major = String.format("0x%X",cls.getMajorDeviceClass());
				String minor = String.format("0x%X",cls.getDeviceClass());
				String mac = device.getAddress();
				JSONObject p = new JSONObject();
				p.put("name",name);
				p.put("address", mac);
				p.put("major_device_class", major);
				p.put("device_class", minor);
				
				
				switch(device.getBondState())
				{
					case BluetoothDevice.BOND_BONDED:
					{
						p.put("bond_state", "BOND_BONDED");
						break;
					}
					case BluetoothDevice.BOND_BONDING:
					{
						p.put("bond_state", "BOND_BONDING");
						break;
					}
					case BluetoothDevice.BOND_NONE:
					{
						p.put("bond_state ", "BOND_NONE");
						break;
					}
					default: 
					{
						break;
					}
				}
				
				array.put(p); 
				System.err.println(p.toString(2));
			}
			//resultString = new PluginResult(PluginResult.Status.OK, array).toSuccessCallbackString(cid);

			class PrinterDeviceFilter implements DeviceFilter
			{

				@Override
				public boolean shouldAddPrinter(BluetoothDevice device)
				{
					BluetoothClass cls = device.getBluetoothClass();
					String name = device.getName();
					if(cls.getMajorDeviceClass() == 0x600) return true;
					String major = String.format("0x%X",cls.getMajorDeviceClass());
					String minor = String.format("0x%X",cls.getDeviceClass());
					String mac = device.getAddress();
					return false;
				}
			}

            BluetoothDiscoverer.findPrinters(this.ctx, new BTDiscoveryHandler(this,array,cid), new PrinterDeviceFilter());
        }
		catch(Exception ex)
		{
			resultString = new PluginResult(PluginResult.Status.ERROR, ex.getLocalizedMessage()).toErrorCallbackString(cid);
		}
		if(resultString != null)
		{
			self.sendJavascript(resultString);
		}
	}

	public JSONObject getStatusForPrinterConnection(BluetoothConnection connection)
	{
		// must already be in a synchronized block
		try
		{
			ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
	    
		    if(printer != null)
		    {
		        PrinterStatus status = printer.getCurrentStatus();        
		        JSONObject statusDict = this.printerStatusAsDictionary(status);
		        return statusDict;
		    }
		}
		catch(Exception ex)
		{
			JSONObject error = new JSONObject();
			try {
				error.put("error", ex.getMessage());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			try {
				error.put("ready", false);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return error;
		}
		return null;
	}

	public void print(JSONArray arguments, String cid) 
	{
	    String resultString = null;
	    String template = null;
	    JSONObject values = null;
	    
	    try
	    {
		    	BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		    	if (mBluetoothAdapter == null) 
		    	{
		    	    // Device does not support Bluetooth
		    		throw new ZebraLinkException("Bluetooth Not Supported On Device");
		    	} 
		    	else 
		    	{
		    	    if (!mBluetoothAdapter.isEnabled()) 
		    	    {
		    	        // Bluetooth is not enabled :)
		    	    		throw new ZebraLinkException("Bluetooth Not Enabled");
		    	    }
		    	}
	    	
		    //	printerIsConnectedAndReady();
		    	
	    		JSONObject argDict = arguments.getJSONObject(0);
	    		template = argDict.getString("template");
	    		values = argDict.getJSONObject("formValues");
	    		
	    		for(Iterator<String> it = values.keys(); it.hasNext();)
		    {
		    		String k = it.next();
		    		String value = values.getString(k);
		        String token = "@"+k+"@";
		        template = template.replaceAll(token,value);
		    }
	    		template = template.replaceAll("\n", "\r\n").replaceAll("\r\r","\r");

		    synchronized(ZebraLink.lock)
		    {
		        ZebraLink.printerConnection.write(template.getBytes());
		    }
	        resultString = new PluginResult(PluginResult.Status.OK, "Success").toSuccessCallbackString(cid);
	    }
	    catch(Exception ex)
        {
	    		resultString = new PluginResult(PluginResult.Status.ERROR, ex.getLocalizedMessage()).toErrorCallbackString(cid);
        }
	    // Try closing the connection after each print because - fuck
	    if(false)
	    {
			try {
				 ZebraLink.printerConnection.close();
			} catch (ConnectionException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			ZebraLink.printerConnection = null;
	    }
	    this.sendJavascript(resultString);
	}
    
	public void test(JSONArray arguments, String cid)
	{
		String resultString = resultString = new PluginResult(PluginResult.Status.OK, "Success").toSuccessCallbackString(cid);
		try
		{         
			  JSONObject argDict = arguments.getJSONObject(0);
			  String macAdd = argDict.getString("address");
			  BluetoothConnection myConn = new BluetoothConnectionInsecure(macAdd);
			  myConn.open();
			  ZebraPrinter myPrinter = ZebraPrinterFactory.getInstance(myConn);
			  myPrinter.printConfigurationLabel();
			  myConn.close();
		}
		catch(Exception ex)
		{
			resultString = new PluginResult(PluginResult.Status.ERROR, ex.getLocalizedMessage()).toErrorCallbackString(cid);
		}
		this.sendJavascript(resultString);
	}

	
	public void connect(JSONArray arguments, String cid)
	{      
	    String address = null;    
	    String resultString = null;
	            
	    try
	    {
	    	JSONObject argDict = arguments.getJSONObject(0);
	    	address = argDict.getString("address");
	    	if(address != null && address.trim().length() > 0) { ZebraLink.printerAddress = address; }
		    BluetoothConnection connection = new BluetoothConnectionInsecure(ZebraLink.printerAddress);
		    
		    synchronized(ZebraLink.lock)
		    {
		        connection.open();

                if(!connection.isConnected())
                {
                    throw new Exception("Failed to connect");
                }

                //connection.write("{}{\\\"device.languages\\\":\\\"zpl\\\"}".getBytes(Charset.defaultCharset()));

	            JSONObject dict = this.getStatusForPrinterConnection(connection);
	            if(dict.optBoolean("ready"))
	            {
	                ZebraLink.printerConnection = connection;
	                final BluetoothConnection watched = ZebraLink.printerConnection;
		            final ZebraLink self = this;
		            /*
		            new Thread(new Runnable() {
		                public void run() {
		                    self.watchPrinterConnection(watched);
		                }
		            }).start();
		            */
	            }
	            else
	            {
	            		if(false)
	            		{
			        		try {
			        			connection.close();
			        		} catch (ConnectionException e) {
			        			// TODO Auto-generated catch block
			        			//e.printStackTrace();
			        		}
			        		ZebraLink.printerConnection = null;
			        		throw new ZebraLinkException(this.printerStatusMessageForStatus(dict));
	            		}
	            }
	            resultString = new PluginResult(PluginResult.Status.OK, this.printerStatusMessageForStatus(dict)).toSuccessCallbackString(cid);
		    }
	    }
	    catch(Exception ex)
	    {
	        resultString = new PluginResult(PluginResult.Status.ERROR, ex.getLocalizedMessage()).toErrorCallbackString(cid);
	    }
	    this.sendJavascript(resultString );
	}

public void disconnect(JSONArray arguments, String cid)
{
	if(swiperThread != null)
	{
		swiperThread.interrupt();
	}
    synchronized(ZebraLink.lock)
    {
        try {
			ZebraLink.printerConnection.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
        ZebraLink.printerConnection = null;
    }
}

public void swipe(JSONArray arguments,String cid)
{    
    String resultString = null;
    String tracks[] = null;

    try
    {
		JSONObject argDict = arguments.getJSONObject(0);
		int timeout = argDict.getInt("timeout");
		if(timeout == 0) { timeout = 20000; }
		ZebraPrinter printer = ZebraPrinterFactory.getInstance(ZebraLink.printerConnection);
		MagCardReader reader = MagCardReaderFactory.create(printer);
		// Drop our priority
		Thread.currentThread().setPriority(Thread.currentThread().getPriority()-1);
		while(!Thread.currentThread().isInterrupted())
	    {
	        int tries = 0;
            int max_tries = 1;

			synchronized(ZebraLink.lock)
			{
				tracks = reader.read(2000);
				if(tracks == null || tracks.length == 0)
				{
					continue;
				}
				JSONArray jsonTracks = new JSONArray();
				boolean hasData = false;
				for(int i = 0; i < tracks.length; ++i)
				{
					jsonTracks.put(tracks[i]);
					if(tracks[i].length() > 0)
					{
						hasData = true;
					}
				}
				if(hasData)
				{
					PluginResult result = new PluginResult(PluginResult.Status.OK, jsonTracks);
					result.setKeepCallback(true);
					resultString = result.toSuccessCallbackString(cid);
					this.sendJavascript(resultString);
				}
				else
				{
					Thread.yield();
				}
			}
	    }
    }
    catch(Exception ex)
    {
		if(!Thread.currentThread().isInterrupted())
		{
			resultString = new PluginResult(PluginResult.Status.ERROR, ex.getLocalizedMessage()).toErrorCallbackString(cid);
			this.sendJavascript(resultString);
		}
    }
	finally
	{
		swiperThread = null;
	}
}

public void onDestroy()
{
	if(ZebraLink.printerConnection != null)
	{
		try
		{
			ZebraLink.printerConnection.close();
		} catch(Exception ex){} // who cares?
	}
	ZebraLink.printerConnection = null;
}

public void sendJavascript(String resultString)
{
	System.err.println("RESULT: " + resultString);
	super.sendJavascript(resultString);
}

/*
	public void sendJavascript(final String statement) 
	{
		System.err.println(statement);
		final PhonegapActivity localctx = this.ctx;
		this.ctx.runOnUiThread(new Runnable(){
			public void run() {
				localctx.sendJavascript(statement);
			}
		});
	}
*/
}
