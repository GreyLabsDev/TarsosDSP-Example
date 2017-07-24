package com.example.greyson.audio_test;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.android.AndroidAudioPlayer;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.util.fft.FFT;

/**
 * Created by Greyson on 29.06.2017.
 */

public class musicProcessor {

    String audioPathThis;

    private String audioPathOut;
    private String metaArtist;
    private String metaTitle;
    private AudioDispatcher dispatcher;
    private int durationPercent;
    private File musicFile;
    private boolean dispatcherInit = false;

    public void StartBgAnalyzer(final Handler handler) {

        AudioProcessor FFTProc = new AudioProcessor() {
            FFT fft= new FFT(2048);
            final float[] amplitudes = new float[2048];

            float paramA = 0;
            float paramB = 0;
            float paramC = 0;

            //container for transporting data from audio processing thread
            ArrayList<Float> resultInternalA = new ArrayList<>();
            ArrayList<Float> resultInternalB = new ArrayList<>();
            ArrayList<Float> resultInternalC = new ArrayList<>();

            //up and down borders for resultA array - platforms height controller
            //upBorder require some value!
            int downBorder = 0;
            int upBorder = 200;

            Bundle bundle = new Bundle();

            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] audioBuffer = audioEvent.getFloatBuffer();
                fft.forwardTransform(audioBuffer);
                fft.modulus(audioBuffer,amplitudes);
                for (int i = 0; i < 682; i++) {
                    paramA = paramA + amplitudes[i];
                    paramB = paramB + amplitudes[i+683];
                    paramC = paramC + amplitudes[i+1365];
                }

                if ((downBorder + (int) Math.floor(10*paramA/682)) < upBorder) {
                    paramA = downBorder + (int) Math.floor(10*paramA/682);
                } else {
                    paramA = upBorder;
                }

                paramB = (int) Math.floor(100*paramB/682);
                paramC = (int) Math.floor(100*paramC/682);

                resultInternalA.add(paramA);
                resultInternalB.add(paramB);
                resultInternalC.add(paramC);

                paramA = 0;
                paramB = 0;
                paramC = 0;

                bundle.putInt("progress", (int) Math.abs((audioEvent.getTimeStamp()/durationPercent)*100));

                Message msg = handler.obtainMessage();
                msg.setData(bundle);
                handler.sendMessage(msg);

                return false;
            }
            @Override
            public void processingFinished() {
                Log.d("END", "END OF PROCESSING");
                Log.d("END - Stats", "resultA size " + resultInternalA.size() + " Item[5] " + resultInternalA.get(5));
                Log.d("END - Stats", "resultB size " + resultInternalB.size() + " Item[5] " + resultInternalB.get(5));
                Log.d("END - Stats", "resultC size " + resultInternalC.size() + " Item[5] " + resultInternalC.get(5));

                bundle.putString("status", "Background processing finished!\n" + "Array A count: " + resultInternalA.size() + "\n" + "Array B count: " + resultInternalB.size() + "\n" + "Array C count: " + resultInternalC.size());

                Message msg = handler.obtainMessage();
                msg.setData(bundle);
                handler.sendMessage(msg);
            }
        };
        dispatcher.addAudioProcessor(FFTProc);
        Thread audioThreadOne = new Thread(dispatcher);
        audioThreadOne.start();

    }

    public void StartRtAnalyzer(final Handler handler) {

        AudioProcessor FFTRealtimeProc = new AudioProcessor() {
            AndroidAudioPlayer audioPlayer = new AndroidAudioPlayer(dispatcher.getFormat(),4096, AudioManager.STREAM_MUSIC);

            FFT fft= new FFT(2048);
            final float[] amplitudes = new float[2048];

            float paramA = 0;
            float paramB = 0;
            float paramC = 0;

            //container for transporting data from audio processing thread
            ArrayList<Float> resultInternalA = new ArrayList<>();
            ArrayList<Float> resultInternalB = new ArrayList<>();
            ArrayList<Float> resultInternalC = new ArrayList<>();

            //up and down borders for resultA array - platforms height controller
            //upBorder require some value!
            int downBorder = 0;
            int upBorder = 200;

            Bundle bundle = new Bundle();

            @Override
            public boolean process(AudioEvent audioEvent) {
                audioPlayer.process(audioEvent);

                float[] audioBuffer = audioEvent.getFloatBuffer();
                fft.forwardTransform(audioBuffer);
                fft.modulus(audioBuffer,amplitudes);
                for (int i = 0; i < 682; i++) {
                    paramA = paramA + amplitudes[i];
                    paramB = paramB + amplitudes[i+683];
                    paramC = paramC + amplitudes[i+1365];
                }

                if ((downBorder + (int) Math.floor(10*paramA/682)) < upBorder) {
                    paramA = downBorder + (int) Math.floor(10*paramA/682);
                } else {
                    paramA = upBorder;
                }

                paramB = (int) Math.floor(100*paramB/682);
                paramC = (int) Math.floor(100*paramC/682);

                resultInternalA.add(paramA);
                resultInternalB.add(paramB);
                resultInternalC.add(paramC);

                Log.d("ParamA","" + paramA);
                Log.d("ParamB","" + paramB);
                Log.d("ParamC","" + paramC);

                bundle.putDouble("freq", (double) paramA);
                bundle.putDouble("freq1", (double) paramB);
                bundle.putDouble("freq2", (double) paramC);

                Message msg = handler.obtainMessage();
                msg.setData(bundle);
                handler.sendMessage(msg);

                paramA = 0;
                paramB = 0;
                paramC = 0;

                return false;
            }

            @Override
            public void processingFinished() {

            }
        };

        dispatcher.addAudioProcessor(FFTRealtimeProc);
        Thread audioThreadTwo = new Thread(dispatcher);
        audioThreadTwo.start();

    }

    public void initDispatcher(String audio) {

        this.setAudioPath(audio);
        Log.d("Path is "," = " + audioPathThis);

        //locating test audio file on ext. storage
        File externalStorage = Environment.getExternalStorageDirectory();
        musicFile = new File(externalStorage.getAbsolutePath(), audioPathThis);
        dispatcher = AudioDispatcherFactory.fromPipe(musicFile.getAbsolutePath(), 44100, 4096, 2048);

        //catching metadata from file
        MediaMetadataRetriever fileMeta = new MediaMetadataRetriever();
        fileMeta.setDataSource(musicFile.getAbsolutePath());
        durationPercent = Math.abs(Integer.parseInt(fileMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))/1000);
        metaArtist = fileMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        metaTitle = fileMeta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);


        //set init state variable to TRUE
        dispatcherInit = true;
    }

    public void stopDispatcher() {
        dispatcher.stop();
        dispatcherInit = false;
    }

    public void runDispatcher() {
        dispatcher.run();
    }

    public boolean dispatcherIsStopped() {
        return dispatcher.isStopped();
    }

    public boolean dispatcherIsInitialised() {
        return dispatcherInit;
    }

    public String getAudioPathOut() {
        return audioPathOut;
    }

    public String getAudioPath() {
        return audioPathThis;
    }

    public String getMetaTitle() { return metaTitle; }

    public String getMetaArtist() { return metaArtist; }

    public void setAudioPath(String audioPathInput) {

        audioPathThis = audioPathInput;
    }

    public void setAudioPathOut(String audioPathOut) {
        audioPathOut = audioPathOut;
    }
}
