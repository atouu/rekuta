package com.rekuta.mashi.adapter;

import static com.rekuta.mashi.utilities.Constants.HIRAGANA;
import static com.rekuta.mashi.utilities.Constants.ROMAJI;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.rekuta.mashi.R;
import com.rekuta.mashi.databinding.SampleListItemBinding;
import com.rekuta.mashi.utilities.FileUtil;

import java.util.ArrayList;

public class ReclistViewAdapter extends BaseAdapter {

    private final ArrayList<String[]> data;
    private final String voicebankDir;
    private int saveAs;

    public ReclistViewAdapter(ArrayList<String[]> data, String voicebankDir, int saveAs) {
        this.data = data;
        this.voicebankDir = voicebankDir;
        this.saveAs = saveAs;
    }

    public void setSaveAs(int saveAs) {
        this.saveAs = saveAs;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public String[] getItem(int index) {
        return data.get(index);
    }

    @Override
    public long getItemId(int index) {
        return index;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup container) {
        SampleListItemBinding binding;

        if (convertView == null) {
            binding = SampleListItemBinding.inflate(LayoutInflater.from(container.getContext()), container, false);
            convertView = binding.getRoot();
            convertView.setTag(R.id.viewBinding, binding);
        } else {
            binding = ((SampleListItemBinding) convertView.getTag(R.id.viewBinding));
        }

        binding.textview1.setText(data.get(position)[HIRAGANA]);

        if (data.get(position)[ROMAJI] == null) {
            binding.textview2.setVisibility(View.GONE);
        } else {
            binding.textview2.setVisibility(View.VISIBLE);
            binding.textview2.setText(data.get(position)[ROMAJI]);
        }

        if (FileUtil.isExistFile(String.format("%s/%s.wav", voicebankDir, data.get(position)[saveAs]))) {
            binding.imageview1.setVisibility(View.VISIBLE);
        } else {
            binding.imageview1.setVisibility(View.INVISIBLE);
        }

        return convertView;
    }
}