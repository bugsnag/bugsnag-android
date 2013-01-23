package com.bugsnag.android.example;

import java.util.HashMap;
import java.util.Map;

import android.app.Application;

import com.bugsnag.android.Bugsnag;

public class ExampleApplication extends Application
{
	/** Called when the application is first created, before any activities or services are created. */
	@Override
	public void onCreate()
	{
		super.onCreate();

		// Register for automatic exception catching
		// In most apps, this is all you will need to do
		Bugsnag.register(this, "89796ce420b9449134db69d973193724");

		// Example of setting the release stage, and which release stages we should notify for
		Bugsnag.setReleaseStage("development");
		Bugsnag.setNotifyReleaseStages("production", "development");

		// Example of setting the context to be the name of the activity
		// If you make your activity inherit from BugsnagActivity, you will get this for free
		Bugsnag.setContext(this.getClass().getSimpleName());

		// Example of setting global extra data to send with every exception
		Map<String, String> extraData = new HashMap<String,String>();
		extraData.put("users name", "bob hoskins");
		extraData.put("users email", "test@example.com");
		extraData.put("password", "should be filtered");
		Bugsnag.setExtraData(extraData);

		// Manual notification with metadata example
		Map<String, String> metaData = new HashMap<String,String>();
		metaData.put("example", "metadata");
		metaData.put("more example", "more metadata");
		Bugsnag.notify(new RuntimeException("Bugsnag Android Test Exception"), metaData);
	}
}
