package com.bus.busstopblind;

import android.app.Application;
import android.content.Context;


/**
 * Created by Sangsu on 2016-10-21.
 */
public class MyApplication  extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

    }

    public  static Context getContext(){
        return  context;
    }

}
