package com.piccoli.hello;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by d.piccoli on 8/01/2016.
 */
public class CallMakerService extends IntentService {

    public CallMakerService(){
        super(CallMakerService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {

    }
}
