package com.joostfunkekupper.nfcwriterandreader;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;


public class MainActivity extends Activity {

    TextView label, output;

    Button writeTag;

    EditText input;

    Tag tag;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

        label = (TextView) findViewById(R.id.label);
        output = (TextView) findViewById(R.id.output);
        writeTag = (Button) findViewById(R.id.writeTag);
        input = (EditText) findViewById(R.id.input);

        writeTag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!input.getText().toString().isEmpty()) {
                    NdefRecord[] records = { createTextRecord(input.getText().toString(), Locale.ENGLISH, true),
                                             NdefRecord.createApplicationRecord(getApplicationContext().getPackageName())};

                    NdefMessage message = new NdefMessage(records);

                    try {
                        if (tag != null) {
                            Ndef ndef = Ndef.get(tag);

                            ndef.connect();

                            ndef.writeNdefMessage(message);

                            ndef.close();
                        } else
                            Toast.makeText(getApplicationContext(), "No tag found!", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("WriteTag", e.toString());
                    } catch (FormatException e) {
                        e.printStackTrace();
                        Log.e("WriteTag", e.toString());
                    } catch (Exception e) {
                        Log.e("WriteTag", e.toString());
                    }
                }
            }
        });
    }

    private String readTag() {

        String result = "";
        try {
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (rawMsgs != null) {
                    for (int i = 0; i < rawMsgs.length; i++) {
                        NdefRecord[] records = ((NdefMessage) rawMsgs[i]).getRecords();

                        for (int j = 0; j < records.length; j++) {
                            if (records[j].getTnf() == NdefRecord.TNF_WELL_KNOWN &&
                                    Arrays.equals(records[j].getType(), NdefRecord.RTD_TEXT)) {
                                byte[] payload = records[j].getPayload();
                                String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
                                int langCodeLen = payload[0] & 0077;

                                result += (new String(payload, langCodeLen + 1, payload.length - langCodeLen - 1, textEncoding));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e("TagDispatch", e.toString());
        }

        return result;
    }

    @Override
    protected void onResume() {
        super.onResume();

        output.setText("Tags: " + readTag());
    }

    public NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = payload.getBytes(utfEncoding);
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], data);
        return record;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
