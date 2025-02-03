package com.rekuta.mashi.libraries.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

import com.rekuta.mashi.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class WaveformView extends View {
    private Paint paint;
    private List<Float> amplitudes;

    public WaveformView(Context context) {
        super(context);
        initialize(context, null);
    }

    public WaveformView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs);
    }

    public WaveformView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs);
    }

    private void initialize(Context context, AttributeSet attrs) {
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        amplitudes = new ArrayList<>();

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WaveformView);
            paint.setColor(ta.getColor(R.styleable.WaveformView_waveColor, 0xFF2196F3));
            paint.setStrokeWidth(ta.getDimension(R.styleable.WaveformView_waveStrokeWidth, 2f));

            ta.recycle();
        } else {
            paint.setColor(0xFF2196F3);
            paint.setStrokeWidth(2f);
        }
    }

    public byte[] exportDataToBytes() {
        if (amplitudes.isEmpty()) return null;

        ByteBuffer buffer = ByteBuffer.allocate(amplitudes.size() * 4);

        for (float f : amplitudes) {
            buffer.putFloat(f);
        }

        return buffer.array();
    }

    public void setAmplitudes(List<Float> newAmplitudes) {
        amplitudes.clear();
        amplitudes = newAmplitudes;
        invalidate();
    }
    public void resetAmplitudes() {
        amplitudes.clear();
        invalidate();
    }

    public void addAmplitude(float amplitude) {
        amplitudes.add(amplitude);
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float centerY = getHeight() / 2f;

        if (amplitudes.isEmpty()) {
            canvas.drawLine(0, centerY , getWidth(), centerY, paint);
            return;
        }

        float rectWidth = getWidth() / (float) amplitudes.size();

        for (int i = 0; i < amplitudes.size(); i++) {
            float amplitude = amplitudes.get(i);
            float scaledHeight = amplitude * getHeight() / 2;
            float top = centerY - scaledHeight;
            float bottom = centerY + scaledHeight;
            float left = i * rectWidth;
            float right = left + rectWidth;

            canvas.drawRect(left, top, right, bottom, paint);
        }
    }
}