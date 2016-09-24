package com.meitu.mopi.activity;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

public class MyApplication extends Application {
  private static Application mBaseApplication = null;

  @Override public void onCreate() {
    super.onCreate();
    LeakCanary.install(this);
    mBaseApplication = this;

  }

  public static Application getBaseApplication() {
    return getApplication();
  }


  public MyApplication() {
  }

  public static Application getApplication() {
    return mBaseApplication;
  }

}