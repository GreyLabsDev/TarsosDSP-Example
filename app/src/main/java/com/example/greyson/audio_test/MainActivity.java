package com.example.greyson.audio_test;


import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import be.tarsos.dsp.io.android.AndroidFFMPEGLocator;

public class MainActivity extends AppCompatActivity {

    //thread group to control audio threads
    ThreadGroup audioThreads = new ThreadGroup("AudioGroup");
    Thread audioThreadOne;
    Thread audioThreadTwo;

    Handler equalizer;
    Handler progressBarHandler;
    String pathToMusic;
    Uri uriMusic;


    TextView textMeta;
    TextView textInfo;


    musicProcessor mProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //
        mProcessor = new musicProcessor();
        //

        //find ffmpeg components (necessary for work with audio files)
        new AndroidFFMPEGLocator(this);

        //finding progress bars
        final ProgressBar soundBar = (ProgressBar)findViewById(R.id.soundBar);
        final ProgressBar soundBar2 = (ProgressBar)findViewById(R.id.soundBar2);
        final ProgressBar soundBar3 = (ProgressBar)findViewById(R.id.soundBar3);
        final ProgressBar soundBarProgress = (ProgressBar)findViewById(R.id.soundBarProgress);

        //finding textView on screen
        textMeta = (TextView)findViewById(R.id.textMeta);
        textInfo = (TextView)findViewById(R.id.textInfo);

        //setting up new handler to control progress bars
        equalizer = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.getData().getDouble("freq") < 100) {
                    soundBar.setProgressDrawable(getDrawable(R.drawable.soundbartheme));
                    soundBar.setProgress((int) msg.getData().getDouble("freq"));
                } else {
                    soundBar.setProgressDrawable(getDrawable(R.drawable.soundbarthemecrit));
                    soundBar.setProgress(100);
                }
                if (msg.getData().getDouble("freq1") < 100) {
                    soundBar2.setProgressDrawable(getDrawable(R.drawable.soundbartheme));
                    soundBar2.setProgress((int) msg.getData().getDouble("freq1"));
                } else {
                    soundBar2.setProgressDrawable(getDrawable(R.drawable.soundbarthemecrit));
                    soundBar2.setProgress(100);
                }
                if (msg.getData().getDouble("freq2") < 100) {
                    soundBar3.setProgressDrawable(getDrawable(R.drawable.soundbartheme));
                    soundBar3.setProgress((int) msg.getData().getDouble("freq2"));
                } else {
                    soundBar3.setProgressDrawable(getDrawable(R.drawable.soundbarthemecrit));
                    soundBar3.setProgress(100);
                }
            }
        };

        progressBarHandler = new Handler() {
           public void handleMessage(Message msg) {
                soundBarProgress.setProgress(msg.getData().getInt("progress"));
               if (msg.getData().getString("status") != null) {
                   Toast.makeText(getApplicationContext(),msg.getData().getString("status"),Toast.LENGTH_LONG).show();
                   soundBarProgress.setProgress(0);
               }
          }
        };

    }
    public void onStopClick(View view) {
        //
        //Thread th[] = new Thread[5];
        //audioThreads.enumerate(th);
        //th[0].interrupt();
        try {
            if (!mProcessor.dispatcherIsStopped()) {
                mProcessor.stopDispatcher();
                pathToMusic = null;
            } else {mProcessor.runDispatcher();}
        } catch (Exception e) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), "Dispatcher is not initialized.\nTap Open to choose audio and initialize!", Snackbar.LENGTH_SHORT)
                                        .setAction("Open", new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                Log.d("Path is "," = " + pathToMusic);
                                                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                                                intent.setType("*/*");
                                                startActivityForResult(intent, 1);
                                            }
                                        });
            snackbar.setActionTextColor(Color.CYAN);
            snackbar.show();
        }

    }
    public void onPlayClick(View view) {
        try {
            mProcessor.initDispatcher(pathToMusic);
            mProcessor.StartRtAnalyzer(equalizer);
        } catch (Exception e) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), "Open audio file to start analyzer!", Snackbar.LENGTH_SHORT)
                    .setAction("Open", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("Path is "," = " + pathToMusic);
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");
                            startActivityForResult(intent, 1);
                        }
                    });
            snackbar.setActionTextColor(Color.CYAN);
            snackbar.show();
        }

    }

    public void onBgAnalyzerClick(View view) {

        try {
            mProcessor.initDispatcher(pathToMusic);
            mProcessor.StartBgAnalyzer(progressBarHandler);
        } catch (Exception e) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), "Open audio file to start analyzer!", Snackbar.LENGTH_SHORT)
                    .setAction("Open", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("Path is "," = " + pathToMusic);
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("*/*");
                            startActivityForResult(intent, 1);
                        }
                    });
            snackbar.setActionTextColor(Color.CYAN);
            snackbar.show();
        }

    }

    public void onOpenClick(View view)
    {
        Log.d("Path is "," = " + pathToMusic);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        if(requestCode == 1)
            if(resultCode == RESULT_OK)
                uriMusic = data.getData();
        try {if (getAbsPath(getApplicationContext(), uriMusic) != null) {

                pathToMusic = getAbsPath(getApplicationContext(), uriMusic).substring(19);
                mProcessor.initDispatcher(pathToMusic);
                textMeta.setText(mProcessor.getMetaArtist() + " - " + mProcessor.getMetaTitle());
                textInfo.setText(pathToMusic);
                Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), mProcessor.getMetaArtist() + " - " + mProcessor.getMetaTitle(), Snackbar.LENGTH_SHORT);
                snackbar.setActionTextColor(Color.CYAN);
                snackbar.show();

            } else pathToMusic = getAbsPath(getApplicationContext(), uriMusic);} catch (Exception e) {pathToMusic = getAbsPath(getApplicationContext(), uriMusic);}

        super.onActivityResult(requestCode, resultCode, data);


    }

    public static String getAbsPath(Context context, Uri uri) {
        String picturePath = "";
        try {String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            picturePath = cursor.getString(columnIndex); // returns null
            cursor.close();}
        catch (Exception e) {

        }
        return picturePath;
    }
}