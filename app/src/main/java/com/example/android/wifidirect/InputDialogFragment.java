package com.example.android.wifidirect;


import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class InputDialogFragment extends DialogFragment {

    private EditText textView;
    private InputDialogListener callback;

    public interface InputDialogListener {
        public void onInputSubmit(String inputSize);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            callback = (InputDialogListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling Fragment must implement OnAddFriendListener");
        }
    }

    public InputDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_input_dialog, container, false);
        textView = (EditText) view.findViewById(R.id.input_dialog_text_view);
        Button submitButton = (Button) view.findViewById(R.id.input_dialog_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String inputString = textView.getText().toString();
                if (inputString.equals("") || inputString.equals("0")) {
                    inputString = "1";
                }
                if (Integer.parseInt(inputString) > 0 && Integer.parseInt(inputString) < 4) {
                    callback.onInputSubmit(inputString);
                }
                getDialog().dismiss();
            }
        } );
        return view;
    }
}
