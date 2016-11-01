package com.bus.busstopblind;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.bus.busstopblind.data.BusAPI.AcctoArvlPrearngeInfoList;
import com.bus.busstopblind.data.BusAPI.CrdntPrxmtSttnListResponse;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;

public class ArrivalActivity extends AppCompatActivity {

    TextView textView;
    CrdntPrxmtSttnListResponse crdntPrxmtSttnListResponse;
    TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arrival);

        textView = (TextView)findViewById(R.id.ment);
        Intent intent = getIntent();
        crdntPrxmtSttnListResponse = (CrdntPrxmtSttnListResponse) intent.getExtras().getSerializable("crdntPrxmtSttnListResponse");


        //음성 출력
        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                    tts.setSpeechRate(0.8f);

                }
            }
        });

        Timer timer = new Timer();

        timer.schedule(adTast, 0, 1000*60*5); // 3초후 첫실행, 5분마다 계속실행
    }


    TimerTask adTast = new TimerTask() {
        public void run() {

            // 버스 도착 정보
            NetworkManager.getInstance().getSttnAcctoArvlPrearngeInfoList(MyApplication.getContext(),
                    crdntPrxmtSttnListResponse.body.items.item.get(0).citycode, crdntPrxmtSttnListResponse.body.items.item.get(0).nodeid, 999, 1, new NetworkManager.OnResultListener<AcctoArvlPrearngeInfoList>() {

                        @Override
                        public void onSuccess(Request request, AcctoArvlPrearngeInfoList result) {
                            if (result.response == null) { // 실패

                                String toSpeak = "현재 버스도착정보가 없습니다.";

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    ttsGreater21(toSpeak.toString());
                                } else {
                                    ttsUnder20(toSpeak.toString());
                                }

                            } else {

                                StringBuilder toSpeak = new StringBuilder();
                                for (int i = 0; i < result.response.body.items.item.size(); i++) {
                                    if (result.response.body.items.item.get(i).arrtime<=600) {
                                        int arrSec = result.response.body.items.item.get(i).arrtime % 60;
                                        int arrMin = result.response.body.items.item.get(i).arrtime / 60;
                                        toSpeak.append(result.response.body.items.item.get(i).routeno + "번" + result.response.body.items.item.get(i).routetp + "가 " + arrMin + "분 " + arrSec + "초 후에 도착합니다.\n");
                                    }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    ttsGreater21(toSpeak.toString());
                                } else {
                                    ttsUnder20(toSpeak.toString());
                                }

                                textView.setText(toSpeak.toString());

                            }

                        }

                        @Override
                        public void onFail(Request request, IOException exception) {

                            Toast.makeText(ArrivalActivity.this, "실패 : "+exception.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                    });
        }
    };

    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId=this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);

    }

    @Override
    protected void onDestroy() {

        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }

        super.onDestroy();
    }


}
