package com.rekuta.mashi.audio;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WavReader {

    private int bitsPerSample;
    private short[] audioData;

    public void readWav(File file) throws IOException {
        //noinspection IOStreamConstructor
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            // Read RIFF header
            byte[] riffId = new byte[4];
            dis.readFully(riffId);
            if (!Arrays.equals(riffId, "RIFF".getBytes())) {
                throw new IOException("Not a valid RIFF file");
            }

            // Skip file size (we don't need it)
            dis.skipBytes(4);

            // Read WAVE format
            byte[] waveId = new byte[4];
            dis.readFully(waveId);
            if (!Arrays.equals(waveId, "WAVE".getBytes())) {
                throw new IOException("Not a valid WAVE file");
            }

            // Read "fmt " chunk
            byte[] fmtId = new byte[4];
            dis.readFully(fmtId);
            if (!Arrays.equals(fmtId, "fmt ".getBytes())) {
                throw new IOException("Expected 'fmt ' chunk");
            }

            // Read fmt chunk size
            int fmtSize = readLittleEndianInt(dis);
            byte[] fmtData = new byte[fmtSize];
            dis.readFully(fmtData);
            parseFmtChunk(fmtData);

            // Find "data" chunk
            while (true) {
                byte[] chunkId = new byte[4];
                dis.readFully(chunkId);
                if (Arrays.equals(chunkId, "data".getBytes())) {
                    break;
                } else {
                    // Skip other chunks
                    int chunkSize = readLittleEndianInt(dis);
                    dis.skipBytes(chunkSize);
                }
            }

            // Read data chunk size
            int dataSize = readLittleEndianInt(dis);
            byte[] audioBytes = new byte[dataSize];
            dis.readFully(audioBytes);

            // Convert bytes to short array (for 16-bit PCM)
            if (bitsPerSample == 16) {
                audioData = new short[audioBytes.length / 2];
                ByteBuffer.wrap(audioBytes)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()
                        .get(audioData);
            } else {
                throw new IOException("Unsupported bits per sample: " + bitsPerSample);
            }
        }
    }

    private void parseFmtChunk(byte[] fmtData) {
        ByteBuffer buffer = ByteBuffer.wrap(fmtData).order(ByteOrder.LITTLE_ENDIAN);
        short audioFormat = buffer.getShort();
        buffer.getShort(); // Skip channels
        buffer.getInt(); // Skip sample rate
        buffer.getInt(); // Skip byte rate
        buffer.getShort(); // Skip block align
        bitsPerSample = buffer.getShort();

        if (audioFormat != 1) {
            throw new UnsupportedOperationException("Unsupported audio format (not PCM)");
        }
    }

    private int readLittleEndianInt(DataInputStream dis) throws IOException {
        byte[] bytes = new byte[4];
        dis.readFully(bytes);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public List<Float> getAmplitudes() {
        short[] shorts = new short[1024];
        ArrayList<Float> floats = new ArrayList<>();
        ShortBuffer buffer = ShortBuffer.wrap(audioData);

        while (buffer.remaining() >= 1024) {
            buffer.get(shorts, 0, 1024);
            float amplitude = computePeak(shorts);
            if (amplitude > 0)  floats.add(amplitude);
        }

        return floats;
    }

    public static List<Float> getAmpsFromWavFile(String path) throws IOException {
        WavReader wavReader = new WavReader();
        wavReader.readWav(new File(path));
        return wavReader.getAmplitudes();
    }

    public static float computePeak(short[] data) {
        float max = 0;
        for (short sample : data) {
            float absSample = Math.abs(sample / 32768.0f);
            if (absSample > max) {
                max = absSample;
            }
        }
        return max;
    }

}