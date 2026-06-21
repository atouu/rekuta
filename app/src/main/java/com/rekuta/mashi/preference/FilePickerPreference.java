package com.rekuta.mashi.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.developer.filepicker.model.DialogProperties;
import com.developer.filepicker.view.FilePickerDialog;
import com.rekuta.mashi.R;

public class FilePickerPreference extends Preference {
    private String defaultValue;
    private String[] extensions = null;

    public FilePickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FilePickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        Button button = (Button) holder.findViewById(R.id.preference_widget_reset);
        button.setOnClickListener(view -> {
            if (callChangeListener(defaultValue)) {
                setText(defaultValue);
            }
        });
    }

    public String getText() {
        return getPersistedString(defaultValue);
    }

    public void setText(String text) {
        final boolean wasBlocking = shouldDisableDependents();

        persistString(text);

        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }

        notifyChanged();
    }

    public void setExtensions(String... ext) {
        extensions = ext;
    }

    @Override
    protected void onClick() {
        DialogProperties properties = new DialogProperties();
        properties.extensions = extensions;
        properties.show_hidden_files = true;

        FilePickerDialog dialog = new FilePickerDialog(getContext(), properties);
        dialog.setTitle(getContext().getString(R.string.select_a_file));
        dialog.setDialogSelectionListener(files -> {
            if (callChangeListener(files[0])) {
                setText(files[0]);
            }
        });
        dialog.show();
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = (String) defaultValue;
        super.setDefaultValue(defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        defaultValue = a.getString(index);
        return super.onGetDefaultValue(a, index);
    }
}
