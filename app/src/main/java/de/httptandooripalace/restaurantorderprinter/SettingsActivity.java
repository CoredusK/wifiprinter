package de.httptandooripalace.restaurantorderprinter;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import entities.Settings;
import helpers.HttpHandler;
import helpers.SharedPrefHelper;

public class SettingsActivity extends AppCompatActivity {
    private boolean somethingChanged;

    private TextView name_line_1, name_line_2, addr_line_1, addr_line_2, tel_line,
        tax_nr, extra_line, waiter_name;

    private TextWatcher textwatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        somethingChanged = false;

        // Add back button
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        name_line_1 = (TextView) findViewById(R.id.name_line_1);
        name_line_2 = (TextView) findViewById(R.id.name_line_2);
        addr_line_1 = (TextView) findViewById(R.id.addr_line_1);
        addr_line_2 = (TextView) findViewById(R.id.addr_line_2);
        tel_line = (TextView) findViewById(R.id.tel_line);
        tax_nr = (TextView) findViewById(R.id.tax_nr);
        extra_line = (TextView) findViewById(R.id.extra_line);
        waiter_name = (TextView) findViewById(R.id.waitername);

        Settings settings = SharedPrefHelper.loadSettings(getApplicationContext());
        if(settings != null) {
            name_line_1.setText(settings.getNameLine1());
            name_line_2.setText(settings.getNameLine2());
            addr_line_1.setText(settings.getAddrLine1());
            addr_line_2.setText(settings.getAddrLine2());
            tel_line.setText(settings.getTelLine());
            tax_nr.setText(settings.getTaxLine());
            extra_line.setText(settings.getExtraLine());
            waiter_name.setText(settings.getWaiter());
        }

        waiter_name.requestFocus();

        textwatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                somethingChanged = true;
            }
        };

        name_line_1.addTextChangedListener(textwatcher);
        name_line_2.addTextChangedListener(textwatcher);
        addr_line_1.addTextChangedListener(textwatcher);
        addr_line_2.addTextChangedListener(textwatcher);
        tel_line.addTextChangedListener(textwatcher);
        tax_nr.addTextChangedListener(textwatcher);
        extra_line.addTextChangedListener(textwatcher);
        waiter_name.addTextChangedListener(textwatcher);

    }

    @Override
    public boolean onSupportNavigateUp() {
        // Nothing changed
        if(!somethingChanged) {
            onBackPressed();
            return true;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getText(R.string.save_settings));
        builder.setMessage(getText(R.string.want_to_save));

        builder.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                entities.Settings settings = new Settings(
                        name_line_1.getText().toString(),
                        name_line_2.getText().toString(),
                        addr_line_1.getText().toString(),
                        addr_line_2.getText().toString(),
                        tel_line.getText().toString(),
                        tax_nr.getText().toString(),
                        extra_line.getText().toString(),
                        waiter_name.getText().toString()
                );

                SharedPrefHelper.saveSettings(getApplicationContext(), settings);
                dialog.dismiss();
                onBackPressed();
            }
        });

        builder.setNeutralButton(getText(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                onBackPressed();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

        return true;
    }

    // Todo: Save api address in configuration
    public void loadItemsFromDb(View view) {
        new HttpHandler(this.getApplicationContext(), (TextView) findViewById(R.id.load_items_status)).execute("http://print.nepali.mobi/printer/api.php");
    }



}
