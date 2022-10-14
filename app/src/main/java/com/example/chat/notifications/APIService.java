package com.example.chat.notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAA1Dvjo38:APA91bFLW98o14erVyMTeAISmJjpZGBJ-WQkJNJNsAHxqD2tRlR01BEAqks6UwbHMoV_5D0jpd88xJ34bFliHFtfF41S1PxcOghtQ7IWXd5FWZAR4kYjBrW0usBvKAT2rk_UFCkgRwtg\t\n"
    })

    @POST("fcm/send")
    Call<Response> sendNotification(@Body Sender body);
}
