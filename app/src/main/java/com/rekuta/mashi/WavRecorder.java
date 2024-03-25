package com.rekuta.mashi;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class WavRecorder {
    private AudioRecord recorder;
    private boolean isRecording = false;
    private Thread recordingThread;

    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static final int RECORDER_CHANNELS = android.media.AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT;
    private static final short BITS_PER_SAMPLE = 16;
    private static final short NUMBER_CHANNELS = 1;
    private static final int BYTE_RATE = RECORDER_SAMPLE_RATE * NUMBER_CHANNELS * 16 / 8;
    private static final int BufferElements2Rec = 1024;

    private String filePath;

    public WavRecorder(String filePath) {
        this.filePath = filePath;
    }

    public void startRecording() {
        final String filePath = this.filePath;
        
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLE_RATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, 512);

        recorder.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile(filePath);
            }
        });
        
        recordingThread.start();
    }

    public void stopRecording() {
        if (recorder != null) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recordingThread = null;
            recorder = null;
        }
    }

    private byte[] short2byte(short[] sData) {
        int arrSize = sData.length;
        byte[] bytes = new byte[arrSize * 2];
        for (int i = 0; i < arrSize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[i * 2 + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void writeAudioDataToFile(String path) {
        short[] sData = new short[BufferElements2Rec];
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ArrayList<Byte> data = new ArrayList<>();

        for (byte aByte : wavFileHeader()) {
            data.add(aByte);
        }

        while (isRecording) {
            recorder.read(sData, 0, BufferElements2Rec);
            byte[] bData = short2byte(sData);
            for (byte aBData : bData) data.add(aBData);
        }

        updateHeaderInformation(data);

        try {
            if (os != null) {
                os.write(toByteArray(data));
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] wavFileHeader() {
        int headerSize = 44;
        byte[] header = new byte[headerSize];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';

        header[4] = 0; // Size of the overall file, 0 because unknown
        header[5] = 0;
        header[6] = 0;
        header[7] = 0;

        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';

        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';

        header[16] = 16; // Length of format data
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;

        header[20] = 1; // Type of format (1 is PCM)
        header[21] = 0;

        header[22] = (byte) NUMBER_CHANNELS;
        header[23] = 0;

        header[24] = (byte) (RECORDER_SAMPLE_RATE & 0xff); // Sampling rate
        header[25] = (byte) (RECORDER_SAMPLE_RATE >> 8 & 0xff);
        header[26] = (byte) (RECORDER_SAMPLE_RATE >> 16 & 0xff);
        header[27] = (byte) (RECORDER_SAMPLE_RATE >> 24 & 0xff);

        header[28] = (byte) (BYTE_RATE & 0xff); // Byte rate = (Sample Rate * BitsPerSample * Channels) / 8
        header[29] = (byte) (BYTE_RATE >> 8 & 0xff);
        header[30] = (byte) (BYTE_RATE >> 16 & 0xff);
        header[31] = (byte) (BYTE_RATE >> 24 & 0xff);

        header[32] = (byte) (NUMBER_CHANNELS * BITS_PER_SAMPLE / 8); //  16 Bits stereo
        header[33] = 0;

        header[34] = (byte) BITS_PER_SAMPLE; // Bits per sample
        header[35] = 0;

        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';

        header[40] = 0; // Size of the data section.
        header[41] = 0;
        header[42] = 0;
        header[43] = 0;

        return header;
    }

    private void updateHeaderInformation(ArrayList<Byte> data) {
        int fileSize = data.size();
        int contentSize = fileSize - 44;

        data.set(4, (byte) (fileSize & 0xff)); // Size of the overall file
        data.set(5, (byte) (fileSize >> 8 & 0xff));
        data.set(6, (byte) (fileSize >> 16 & 0xff));
        data.set(7, (byte) (fileSize >> 24 & 0xff));

        data.set(40, (byte) (contentSize & 0xff)); // Size of the data section.
        data.set(41, (byte) (contentSize >> 8 & 0xff));
        data.set(42, (byte) (contentSize >> 16 & 0xff));
        data.set(43, (byte) (contentSize >> 24 & 0xff));
    }

    private byte[] toByteArray(ArrayList<Byte> list) {
        byte[] array = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}