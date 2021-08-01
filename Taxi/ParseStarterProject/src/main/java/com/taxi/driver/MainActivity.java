/*
 * Copyright (c) 2015-present, Parse, LLC.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.taxi.driver;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Switch;

import com.parse.LogInCallback;
import com.parse.ParseAnalytics;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.starter.R;


public class MainActivity extends AppCompatActivity {

     public void redirectActivity()
     {
         if(ParseUser.getCurrentUser().get("riderOrDriver").equals("rider"))
         {
             Intent intent=new Intent(getApplicationContext(),RiderActivity.class);
               startActivity(intent);
         }
         else
         {
               Log.i("status ","we are redirecting to ViewRequestActivity.class");
             Intent intent=new Intent(getApplicationContext(),ViewRequestActivity.class);
             startActivity(intent);

         }
     }

  public void getStarted(View view)
  {
      Switch userTypeSwitch= (Switch) findViewById(R.id.userTypeSwitch);
      Log.i("Switch value",String.valueOf(userTypeSwitch.isChecked()));
      String userType="rider";
      if(userTypeSwitch.isChecked())
      {
          userType="driver";
      }
      ParseUser.getCurrentUser().put("riderOrDriver",userType);
      ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
          @Override
          public void done(ParseException e) {
              redirectActivity();
          }
      });
      Log.i("INFO ","Redirecting as"+userType);

  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    getSupportActionBar().hide();
     if(ParseUser.getCurrentUser()==null)
     {
         // we are not taking any details from user we are just get him login
         ParseAnonymousUtils.logIn(new LogInCallback() {
             @Override
             public void done(ParseUser user, ParseException e) {
                  if(e==null)
                  {
                      Log.i("INFO","annomous login successful");

                  }
                  else
                  {
                      Log.i("INFO","annomous login failed");
                  }
             }
         });
     }
     else
     {
          if(ParseUser.getCurrentUser().get("riderOrDriver")!=null)
          {
              Log.i("INFO","Redirecting as "+ParseUser.getCurrentUser().get("riderOrDriver"));
               redirectActivity();
          }
     }
    ParseAnalytics.trackAppOpenedInBackground(getIntent());
  }

}