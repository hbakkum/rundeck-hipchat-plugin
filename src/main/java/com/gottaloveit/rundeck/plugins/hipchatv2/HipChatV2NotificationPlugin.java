package com.gottaloveit.rundeck.plugins.hipchatv2;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;


@Plugin(service= "Notification", name="HipChatV2Notification")
@PluginDescription(title="Rundeck HipchatV2 Notification", description="Uses the Hipchat V2 API for roomn notifications.")
public class HipChatV2NotificationPlugin implements NotificationPlugin {

	private static final String HIPCHATV2_GREEN = "green";
    private static final String HIPCHATV2_YELLOW = "yellow";
    private static final String HIPCHATV2_RED = "red";
    
    private static final String START = "start";
    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";    

    private static final Map<String, String> TRIGGER_COLORS = new HashMap<String, String>();
    static {
        TRIGGER_COLORS.put(START, HIPCHATV2_YELLOW);
        TRIGGER_COLORS.put(SUCCESS, HIPCHATV2_GREEN);
        TRIGGER_COLORS.put(FAILURE, HIPCHATV2_RED);
    }	
    
    private Map<String, String> TRIGGER_NOTIFY;
    
	@PluginProperty(name="roomNumber", title="Room Numer", description="Room that you want this set of notices sent to. You can get the ID from the room properties.", required=true)
	private String roomNumber;

	@PluginProperty(name="roomApiKey", title="Room API Key", description="Create a V2 room notifcation level API key.", required=true)
	private String roomApiKey;
	
	@PluginProperty(name="onStartDoNotify", title="Notify on Start", description="Set to enable Hipchat personal notifications for when this job starts.", defaultValue="No")
	@SelectValues(values={"Yes","No"})
	private String onStartDoNotify;
	
	@PluginProperty(name="onEndDoNotify", title="Notify on Success End", description="Set to enable Hipchat personal notifications for when this job successfully ends.", defaultValue="No")
	@SelectValues(values={"Yes","No"})
	private String onEndDoNotify;
	
	@PluginProperty(name="onFailedDoNotify", title="Notify on Failed job", description="Set to enable Hipchat personal notifications for when this job fails.", defaultValue="Yes")
	@SelectValues(values={"Yes","No"})
	private String onFailedDoNotify;	
	
	private String mTrigger;
	private Map mExecutionData;
	private Map mConfig;
	private String url;
	private String jsonBody;
	private JSONObject jObj;
	
	public boolean postNotification(String trigger, Map executionData, Map config) {
		mTrigger = trigger;
		mExecutionData = executionData;
		mConfig = config;
		jObj = new JSONObject();
		
		TRIGGER_NOTIFY = new HashMap<String,String>();
		TRIGGER_NOTIFY.put(START, onStartDoNotify);
		TRIGGER_NOTIFY.put(FAILURE, onFailedDoNotify);
		TRIGGER_NOTIFY.put(SUCCESS, onEndDoNotify);
		
		buildParams();
		buildURL();
		buildJson();
		Send();
		return true;
	}
	
	private void buildParams() {
		buildColor();
		buildNotify();
		buildMessageFormat();
		buildMessage();
	}
	private void buildColor() {
		String color = TRIGGER_COLORS.get(mTrigger);
		jObj.put("color", color);
	}
	private void buildNotify() {
		String notify = TRIGGER_NOTIFY.get(mTrigger);
		if (notify.equalsIgnoreCase("yes")) {
			jObj.put("notify", true);
		} else {
			jObj.put("notify", false);
		}
	}
	private void buildMessageFormat() {
		jObj.put("message_format", "text");
	}
	private void buildMessage() {
		Object job = mExecutionData.get("job");
		Map jobdata = (Map) job;
		
		Object execUrl = mExecutionData.get("href");
		Object jobUrl = jobdata.get("href");
		Object jobName = jobdata.get("name");
		
		String msg = null;
		switch (mTrigger) {
		case (START) :
			msg = String.format("%s (%s) has started.",jobName,jobUrl);	
			break;
		case (SUCCESS) :
			msg = String.format("%s (%s) has finished successfully. Output viewable: %s.",jobName,jobUrl,execUrl);
			break;
		case (FAILURE) :
			msg = String.format("%s (%s) did not finish. See log output at %s.",jobName,jobUrl,execUrl);
			break;
		}
		jObj.put("message", msg);
	}
	private void buildURL() {
		url = "https://api.hipchat.com/v2/room/"+roomNumber+"/notification?auth_token="+roomApiKey;
	}	
	private void buildJson() {
		jsonBody = jObj.toString();
	}
	private void Send() {
		URL req = makeURL(url);
		HttpURLConnection con = null;
		InputStream rs = null;
		try {
			con = openCon(req);
			con.setRequestMethod("POST");
			con.setRequestProperty("content-type", "application/json");
			con.setDoOutput(true);
		
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(jsonBody);
			wr.flush();
			wr.close();
			
			rs = getRS(con);
			int rc = getRC(con);
			
			if (rc != 204) {
				throw new MyException("Request not received. Response Code: "+rc);
			} 
		} catch (ProtocolException e) {
			throw new MyException("Bad Protocol: "+e.getMessage());
		} catch (IOException e) {
			throw new MyException("Bad outputstream: "+e.getMessage());
		} finally {
			close(rs);
			if (con !=null) {
				con.disconnect();
			}
		}
	}
    private URL makeURL(String url) {
    	try {
    		return new URL(url);
    	} catch (MalformedURLException m) {
    		throw new MyException("Bad url: [" + m.getMessage() + "].", m);
    	}
    }    
    private HttpURLConnection openCon(URL req) {
    	try {
    		return (HttpURLConnection) req.openConnection();
    	} catch (IOException i) {
    		throw new MyException("Error opening connection: [" + i.getMessage() + "].", i);
    	}
    }
    private InputStream getRS(HttpURLConnection con) {
    	InputStream i = null;
    	try {
    		i = con.getInputStream();
    	} catch (IOException e) {
    		i = con.getErrorStream();
    	}
    	return i;
    }
    private int getRC(HttpURLConnection con) {
    	try {
    		return con.getResponseCode();
    	} catch (IOException i) {
    		throw new MyException("Failed response: [" + i.getMessage() + "].", i);
    	}
    }
    private void close(InputStream i) {
    	if (i != null) {
    		try {
    			i.close();
    		} catch (IOException ie) {
    			// do nothing
    		}
    	}
    }
    private class MyException extends RuntimeException {
    	/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public MyException(String msg) {
    		super(msg);
    	}
    	public MyException(String msg, Throwable c) {
    		super(msg,c);
    	}
    }
}
