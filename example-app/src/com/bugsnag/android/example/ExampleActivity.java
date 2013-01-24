package com.bugsnag.android.example;

import android.os.Bundle;

import com.bugsnag.android.BugsnagActivity;

/** If you make your activity inherit from BugsnagActivity, like this one,
    you will get see the correct context of the exception in Bugsnag automatically. **/
public class ExampleActivity extends BugsnagActivity
{
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Cause a RuntimeException
		throw new RuntimeException("It broke");
	}
}
