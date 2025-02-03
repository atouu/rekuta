package com.rekuta.mashi.preference;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;

public class IntegerEditTextPreference extends EditTextPreference {

    public IntegerEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public IntegerEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setText(@Nullable String text) {
        assert text != null;
        super.setText(text.isEmpty() ? "0" : text);
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.parseInt(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int intValue;

        if (defaultReturnValue != null) {
            intValue = getPersistedInt(Integer.parseInt(defaultReturnValue));
        } else if (getPersistedInt(0) == getPersistedInt(1)) {
            intValue = getPersistedInt(0);
        } else {
            throw new IllegalArgumentException("Cannot get an int without a default return value");
        }

        return Integer.toString(intValue);
    }

}