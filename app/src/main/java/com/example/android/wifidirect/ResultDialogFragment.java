package com.example.android.wifidirect;

import android.app.DialogFragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ResultDialogFragment extends DialogFragment {

    private static final String ARG_PARAM1 = "type";
    private static final String ARG_PARAM2 = "time";
    private static final String ARG_PARAM3 = "result";

    TextView rType;
    TextView rTime;
    TextView rResult;

    public ResultDialogFragment() {
        // Required empty public constructor
    }

    public static ResultDialogFragment newInstance(String param1, String param2, String param3) {
        ResultDialogFragment fragment = new ResultDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        args.putString(ARG_PARAM3, param3);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_result_dialog, container,
                false);
        String string_type = getArguments().getString(ARG_PARAM1);
        getDialog().setTitle(string_type+" Result");
//        rType = (TextView) view.findViewById(R.id.output_type);
//        rType.setText(string_type);
        String string_time = getArguments().getString(ARG_PARAM2);
        rTime = (TextView) view.findViewById(R.id.output_time);
        rTime.setText(string_time+" mili seconds");
        String string_result = getArguments().getString(ARG_PARAM3);
        rResult = (TextView) view.findViewById(R.id.output_result);
        rResult.setText(string_result);
        return view;
    }
}
