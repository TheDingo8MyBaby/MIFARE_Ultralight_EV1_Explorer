package de.androidcrypto.mifare_ultralight_ev1_explorer;

import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.authenticateUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.customPack;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.customPassword;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.defaultPack;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.defaultPassword;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.identifyUltralightEv1Tag;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.identifyUltralightFamily;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.pagesToRead;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.MIFARE_Ultralight_EV1.writePageMifareUltralightEv1;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.bytesToHexNpe;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.doVibrate;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.getTimestampShort;
import static de.androidcrypto.mifare_ultralight_ev1_explorer.Utils.printData;

import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class WriteFragment extends Fragment implements NfcAdapter.ReaderCallback {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteFragment";

    private String mParam1;
    private String mParam2;

    private com.google.android.material.textfield.TextInputEditText dataToSend, resultNfcWriting;
    private SwitchMaterial addTimestampToData;
    private AutoCompleteTextView startPageTextView, endPageTextView;
    private com.google.android.material.textfield.TextInputLayout dataToSendLayout;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth, rbHex, rbAscii;
    private RadioGroup rgDataFormat;
    private View loadingLayout;
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private boolean isTagUltralight = false;
    private int storageSize = 0;
    private int startPage, endPage;
    private String outputString = "";

    public WriteFragment() {
        // Required empty public constructor
    }

    public static WriteFragment newInstance(String param1, String param2) {
        WriteFragment fragment = new WriteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        dataToSend = getView().findViewById(R.id.etWriteData);
        dataToSendLayout = getView().findViewById(R.id.etWriteDataLayout);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        addTimestampToData = getView().findViewById(R.id.swMainAddTimestampSwitch);
        addTimestampToData.setChecked(false);
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        loadingLayout = getView().findViewById(R.id.loading_layout);
        rgDataFormat = getView().findViewById(R.id.rgDataFormat);
        rbHex = getView().findViewById(R.id.rbHex);
        rbAscii = getView().findViewById(R.id.rbAscii);

        startPageTextView = getView().findViewById(R.id.startPage);
        endPageTextView = getView().findViewById(R.id.endPage);

        String[] pages = new String[]{"4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
                getView().getContext(),
                R.layout.drop_down_item,
                pages);

        startPageTextView.setText(pages[0]);
        startPageTextView.setAdapter(arrayAdapter);
        endPageTextView.setText(pages[0]);
        endPageTextView.setAdapter(arrayAdapter);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        startPageTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                startPage = Integer.parseInt(startPageTextView.getText().toString());
                validatePages();
            }
        });

        endPageTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                endPage = Integer.parseInt(endPageTextView.getText().toString());
                validatePages();
            }
        });

        addTimestampToData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    dataToSendLayout.setEnabled(false);
                } else {
                    dataToSendLayout.setEnabled(true);
                    dataToSendLayout.setCounterMaxLength(16);
                    setEditTextMaxLength(dataToSend, 16);
                }
            }
        });

        rgDataFormat.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbHex) {
                dataToSendLayout.setCounterMaxLength(32);
                setEditTextMaxLength(dataToSend, 32);
            } else {
                dataToSendLayout.setCounterMaxLength(16);
                setEditTextMaxLength(dataToSend, 16);
            }
        });
    }

    private void validatePages() {
        if (startPage < 4 || endPage > 15 || startPage > endPage) {
            showMessage("Invalid start or end page.");
        } else {
            int numPages = endPage - startPage + 1;
            if (rbHex.isChecked()) {
                dataToSendLayout.setCounterMaxLength(numPages * 8);
                setEditTextMaxLength(dataToSend, numPages * 8);
            } else {
                dataToSendLayout.setCounterMaxLength(numPages * 4);
                setEditTextMaxLength(dataToSend, numPages * 4);
            }
        }
    }

    private void setEditTextMaxLength(EditText et, int maxLength) {
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        System.out.println("NFC tag discovered");
        playSinglePing();

        boolean success;
        boolean authSuccess = false;

        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.white));
            resultNfcWriting.setText("");
        });

        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(resultNfcWriting);
            setLoadingLayoutVisibility(false);
            returnOnNotSuccess();
            return;
        }

        byte[] tagId = nfcA.getTag().getId();
        int maxTransceiveLength = nfcA.getMaxTransceiveLength();
        String[] techList = nfcA.getTag().getTechList();
        StringBuilder sb = new StringBuilder();
        sb.append("Technical Data of the Tag").append("\n");
        sb.append("Tag ID: ").append(bytesToHexNpe(tagId)).append("\n");
        sb.append("maxTransceiveLength: ").append(maxTransceiveLength).append(" bytes").append("\n");
        sb.append("Tech-List:").append("\n");
        sb.append("Tag TechList: ").append(Arrays.toString(techList)).append("\n");
        if (identifyUltralightFamily(nfcA)) {
            sb.append("The Tag seems to be a MIFARE Ultralight Family tag").append("\n");
            isTagUltralight = true;
        } else {
            sb.append("The Tag IS NOT a MIFARE Ultralight tag").append("\n");
            sb.append("** End of Processing **").append("\n");
            isTagUltralight = false;
        }
        writeToUiAppend(sb.toString());

        if (!isTagUltralight) {
            returnOnNotSuccess();
            return;
        }

        try {
            nfcA.connect();

            if (nfcA.isConnected()) {
                storageSize = identifyUltralightEv1Tag(nfcA);
                sb = new StringBuilder();
                if (storageSize == 0) {
                    sb.append("The Tag IS NOT a MIFARE Ultralight EV1 tag").append("\n");
                    sb.append("** End of Processing **").append("\n");
                    isTagUltralight = false;
                } else if (storageSize == 48) {
                    sb.append("The Tag is a MIFARE Ultralight EV1 tag with 48 bytes user memory size").append("\n");
                    pagesToRead = 20;
                    isTagUltralight = true;
                } else if (storageSize == 128) {
                    sb.append("The Tag is a MIFARE Ultralight EV1 tag with 128 bytes user memory size").append("\n");
                    pagesToRead = 41;
                    isTagUltralight = true;
                } else {
                    sb.append("The Tag IS NOT a MIFARE Ultralight EV1 tag").append("\n");
                    sb.append("** End of Processing **").append("\n");
                    isTagUltralight = false;
                }
                writeToUiAppend(sb.toString());
                if (!isTagUltralight) {
                    returnOnNotSuccess();
                    return;
                }

                String sendData = dataToSend.getText().toString();
                if (addTimestampToData.isChecked()) sendData = getTimestampShort() + " ";
                if (TextUtils.isEmpty(sendData)) {
                    writeToUiAppend("Please enter some data to write on tag. Aborted");
                    writeToUiFinal(resultNfcWriting);
                    return;
                }

                int numPages = endPage - startPage + 1;
                int requiredLength = rbHex.isChecked() ? numPages * 8 : numPages * 4;
                if (sendData.length() != requiredLength) {
                    writeToUiAppend("Invalid data length. Expected " + requiredLength + " characters.");
                    writeToUiFinal(resultNfcWriting);
                    return;
                }

                byte[] dtw = new byte[numPages * 4];
                if (rbHex.isChecked()) {
                    dtw = hexStringToByteArray(sendData);
                } else {
                    System.arraycopy(sendData.getBytes(StandardCharsets.UTF_8), 0, dtw, 0, sendData.getBytes(StandardCharsets.UTF_8).length);
                }

                writeToUiAppend(printData("data to write", dtw));

                if (rbNoAuth.isChecked()) {
                    writeToUiAppend("No Authentication requested");
                    authSuccess = true;
                } else if (rbDefaultAuth.isChecked()) {
                    writeToUiAppend("Authentication with Default Password requested");
                    int authResult = authenticateUltralightEv1(nfcA, defaultPassword, defaultPack);
                    if (authResult == 1) {
                        writeToUiAppend("authentication with Default Password and Pack: SUCCESS");
                        authSuccess = true;
                    } else {
                        writeToUiAppend("authentication with Default Password and Pack: FAILURE " + authResult);
                        authSuccess = false;
                    }
                } else {
                    writeToUiAppend("Authentication with Custom Password requested");
                    int authResult = authenticateUltralightEv1(nfcA, customPassword, customPack);
                    if (authResult == 1) {
                        writeToUiAppend("authentication with Custom Password and Pack: SUCCESS");
                        authSuccess = true;
                    } else {
                        writeToUiAppend("authentication with Custom Password and Pack: FAILURE " + authResult);
                        authSuccess = false;
                    }
                }

                if (!authSuccess) {
                    writeToUiAppend("The authentication was not successful, operation aborted.");
                    returnOnNotSuccess();
                    return;
                }

                for (int i = 0; i < numPages; i++) {
                    byte[] pageData = Arrays.copyOfRange(dtw, i * 4, (i + 1) * 4);
                    success = writePageMifareUltralightEv1(nfcA, startPage + i, pageData);
                    writeToUiAppend("Tried to write data to tag on page " + (startPage + i) + ", success ? : " + success);
                }
                nfcA.close();
            }
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }

        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        reconnect(nfcA);
    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        mNfcAdapter.disableReaderMode(this.getActivity());
    }

    private void reconnect(NfcA nfcA) {
        try {
            nfcA.close();
            Log.d(TAG, "Close NfcA");
        } catch (Exception e) {
            Log.e(TAG, "Exception on Close NfcA: " + e.getMessage());
        }
        try {
            Log.d(TAG, "Reconnect NfcA");
            nfcA.connect();
        } catch (Exception e) {
            Log.e(TAG, "Exception on Reconnect NfcA: " + e.getMessage());
        }
    }

    private void playSinglePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_02);
        mp.start();
    }

    private void playDoublePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_01);
        mp.start();
    }

    private void writeToUiAppend(String message) {
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) resultNfcWriting) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(outputString);
                    System.out.println(outputString);
                }
            });
        }
    }

    private void setLoadingLayoutVisibility(boolean isVisible) {
        getActivity().runOnUiThread(() -> {
            if (isVisible) {
                loadingLayout.setVisibility(View.VISIBLE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });
    }

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            resultNfcWriting.setText(message);
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            mNfcAdapter.enableReaderMode(getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}