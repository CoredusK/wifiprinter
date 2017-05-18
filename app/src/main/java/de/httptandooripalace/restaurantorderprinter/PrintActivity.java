package de.httptandooripalace.restaurantorderprinter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.SettingInjectorService;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;
import entities.Product;
import entities.Settings;
import helpers.PrintAdapter;
import helpers.RequestClient;
import helpers.Rounder;
import helpers.SharedPrefHelper;
import helpers.StringHelper;

public class PrintActivity extends AppCompatActivity {
    private List<Product> products = new ArrayList<Product>();
    private final int CHARCOUNT_BIG = 48; // Amount of characters fit on one printed line, using $big$ format
    private final int CHARCOUNT_BIGW = 24; // Amount of characters fit on one printed line, using $bigw$ format

    private final String INITIATE = "·27··64·"; // ESC @
    private final String CHAR_TABLE_EURO = "·27··116··19·"; // ESC t 19 -- 19 for euro table
    private final String EURO = "·213·";
    private final String DOT = "·46·";
    private final String BR = "$intro$"; // Line break
    private final String u = "·129·";
    private final String U = "·154·";
    Context context;
    int bill_nr = 0;

    private entities.Settings settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.print_activity);

        // Get products
//        products = SharedPrefHelper.getPrintItems(getApplicationContext());
        settings = SharedPrefHelper.loadSettings(getApplicationContext());

        if(settings == null) {
            settings = new Settings();
            SharedPrefHelper.saveSettings(getApplicationContext(), settings);
        }

        //TODO : get requestclient method to display the product of this bill

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            bill_nr = extras.getInt("bill_nr");
            Log.d("RESPONSE",  "numero d'addition : "+ bill_nr);
        }else{
            Log.d("RESPONSE", "extras est null :::::::::::::::" + bill_nr);
        }

        try {
            StringEntity entity;

            JSONObject jsonParams = new JSONObject();
            Log.d("RESPONSE", "trying to get the bill products");
            RequestParams params = new RequestParams();
            jsonParams.put("bill_id", bill_nr);
            entity = new StringEntity(jsonParams.toString());
            RequestClient.post(context,"products/getforbill/", entity, "application/json", new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray
                    try {
                        Log.d("RESPONSE", response.getJSONArray("products").toString()); // RESPONSE: {"success":"true","products":[{"id_cat":"18","name_cat":" Dienstag","id_prod":"371","name_prod":"Chicken Curry","reference_prod":"512,","price_prod_excl":"4.03","price_prod_incl":"4.32","description_prod":"","bill_id":"1"},
                        JSONArray jsonarray = response.getJSONArray("products"); // error is here !!
                        //JSONArray jsonarray = new JSONArray();
                        Log.d("RESPONSE", jsonarray.length()+""); //2
                        //List<Product> products = new ArrayList<Product>();
                        for (int i = 0; i < jsonarray.length(); i++) {
                            JSONObject jsonobject = jsonarray.getJSONObject(i);
                            String name = jsonobject.getString("name_prod");
                            int id = jsonobject.getInt("id_prod");
                            double price_excl = jsonobject.getDouble("price_prod_incl");
                            double price_incl = jsonobject.getDouble("price_prod_excl");
                            String reference = jsonobject.getString("reference_prod");
                            String category = jsonobject.getString("name_cat");
                            Product p = new Product(id, name, price_excl, price_incl, reference, category);
                            products.add(p);
                        }
                        Log.d("RESPONSE", products.toString());
                    }
                    catch(Exception e) {
                        Log.d("Exception HTTP", e.getMessage());
                    }
                }

                @Override
                public void onFailure(int c, Header[] h, String r, Throwable t) {
                    try {
                        Log.d("RESPONSE", r.toString());
                    }
                    catch(Exception e) {
                        Log.d("Exception HTTP", e.getMessage());
                    }
                }
            });
        }
        catch(Exception e) {
            Log.d("Ex", e.getMessage());

        }

        // Bind products to print overview
        ListView view = (ListView) findViewById(R.id.listingLayout);
        PrintAdapter adapter = new PrintAdapter(getApplicationContext(), products);
        view.setAdapter(adapter);

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    // Add settings menu icon to toolbar
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.print, menu);
        return true;
    }

    //@Override
    // Open settings menu
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                onBackPressed();
                return true;
            case R.id.print_bill:
                printBill(item);
                return true;
            case R.id.print_tax:
                printTaxBill(item);
                return true;
            case R.id.print_drink:
                printDrinkBill(item);
                return true;
            case R.id.print_kitchen:
                printKitchenBill(item);
                return true;
//            case R.id.print_kitchen:
//                Log.d("DOT TEST SHOULD BE ONE", checkCount("One time euro " + DOT, DOT) + "");
//                Log.d("DOT TEST SHOULD BE TWO", checkCount("One time " + DOT + "euro " + DOT, DOT) + "");
//                Log.d("SHOULD BE FIVE", checkCount(DOT + DOT + "One t " + DOT + "ime euro " + DOT + DOT, DOT) + "");
//                return true;
            case R.id.bills_overview:
                Intent i2 = new Intent(this, OverviewActivity.class);
                startActivity(i2);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Delete the print overview and refresh activity
    public void deletePrintOverview(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Delete overview");
        builder.setMessage("Overview will be cleared.\nAre you sure?");

        builder.setPositiveButton(getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPrefHelper.deleteSharedPrefs(getApplicationContext());
                finish();
                startActivity(getIntent());
                dialog.dismiss();

            }
        });

        builder.setNegativeButton(getText(R.string.no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void test(MenuItem item){
        // Do print job button clicked

            //doWebViewPrint(ids, names, prices);
            //String dataToPrint="$big$This is a printer test$intro$posprinterdriver.com$intro$$intro$$cut$$intro$";

//
//
//        //String textToSend="$intro$$big$Test test 123 test$intro$$intro$$intro$";
//        String textToSend="$intro$$intro$$intro$$big$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA$intro$$intro$$intro$$intro$$intro$$small$BBBBBBBBBBBBBBBBBBBB$intro$$intro$$intro$";
//        Intent intentPrint = new Intent();
//        intentPrint.setAction(Intent.ACTION_SEND);
//        intentPrint.putExtra(Intent.EXTRA_TEXT, textToSend);
//        intentPrint.putExtra("printer_type_id", "1");// For IP
//        intentPrint.putExtra("printer_ip", "192.168.178.105");
//        intentPrint.putExtra("printer_port", "9100");
//
//        intentPrint.setType("text/plain");
//        Log.i("Print job log: ", "sendDataToIPPrinter Start Intent");
//
//        this.startActivity(intentPrint);
//

            StringBuilder strb = new StringBuilder();

            strb.append("<BIG>Bill<BR><BR>"); // Todo table number and other info


            strb.append("testestestestestestse");
            strb.append("<BR><BR>");

            Intent intent = new Intent("pe.diegoveloper.printing");
            //intent.setAction(android.content.Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(android.content.Intent.EXTRA_TEXT,strb.toString());
            startActivity(intent);


    }

    public void printTaxBill(MenuItem item) {
        if(products == null) return;
        if(products.size() <= 0) return;

        sendPrintJob(getBillContent() + getTaxFooter());
    }

    public void printBill(MenuItem item) {
        if(products == null) return;
        if(products.size() <= 0) return;

        sendPrintJob(getBillContent() + getBillFooter());
    }

    public void printDrinkBill(MenuItem item){
        if(products == null) return;
        if(products.size() <= 0) return;

        String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");
        int number_drinks = 0;

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        if(!tableNr.equals("")) {
            strb.append("$bighw$");
            strb.append(getString(R.string.table_nr).toUpperCase() + tableNr);
            strb.append(BR);
        }

        strb.append("$big$");
        strb.append(BR);
        strb.append(alignCenter(getString(R.string.drink).toUpperCase()));


        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf('=', CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for(int i = 0; i < products.size(); i++) {
            if (products.get(i).getDrink()){

            }
        }

        for(int i = 0; i < products.size(); i++) {
            if (products.get(i).getCount() < 1) continue;

            if (products.get(i).getDrink()){
                // Don't print it when it is the first time and number_drinks == 0
                if (!(number_drinks == 0))
                    strb.append(getLineOf('-', CHARCOUNT_BIG));

                number_drinks++;
                double priceEx = products.get(i).getPrice_excl();
                double priceInc = products.get(i).getPrice_incl();

                strb.append(BR);

                if(products.get(i).getCount()!=1) {
                    // 2 x 2.15
                    strb.append("$bighw$");
                    s = products.get(i).getCount() + " x ";
                    strb.append(s);
                    strb.append("$big$");
                    s = EURO + Rounder.round(products.get(i).getPrice_excl());
                    strb.append(s);
                    strb.append(BR);
                }
                s = "#"+products.get(i).getReference()+" ";
                strb.append(s);
                strb.append(BR);
                // All Star Product                 4.30
                strb.append("$bighw$");
                s = StringHelper.swapU(products.get(i).getName().toUpperCase());
                strb.append(s);
                strb.append("$big$");
                String totalPriceForThisProduct = Rounder.round(products.get(i).getCount() * products.get(i).getPrice_excl());
                s = alignRightSpecial((EURO + totalPriceForThisProduct), products.get(i).getName().length());
                strb.append(s);
                strb.append(BR);



                totalPriceExcl += (priceEx * products.get(i).getCount());
                totalPriceIncl += (priceInc * products.get(i).getCount());
            }
        }
        strb.append(getLineOf('=', CHARCOUNT_BIG));
        strb.append(BR);

        // Date
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        strb.append("$big$" + BR + BR);
        strb.append(currentDateTimeString);
        strb.append(" " + getString(R.string.waiter) + " " + settings.getWaiter());
        strb.append(BR);
        //strb.append("Bondrucker1"); //Don't need that :O

        for(int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");

        sendPrintJob(strb.toString());
    }

    public void printKitchenBill(MenuItem item){
        if(products == null) return;
        if(products.size() <= 0) return;

        String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");
        int number_kitchen = 0;

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        if(!tableNr.equals("")) {
            strb.append("$bighw$");
            strb.append(getString(R.string.table_nr).toUpperCase() + tableNr);
            strb.append(BR);
        }

        strb.append("$big$");
        strb.append(BR);
        strb.append(alignCenter(StringHelper.swapU(getString(R.string.kitchen).toUpperCase())));


        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf('=', CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for(int i = 0; i < products.size(); i++) {
            if (products.get(i).getCount() < 1) continue;

            if (!products.get(i).getDrink()){
                if (!(number_kitchen == 0))
                    strb.append(getLineOf('-', CHARCOUNT_BIG));

                number_kitchen++;

                double priceEx = products.get(i).getPrice_excl();
                double priceInc = products.get(i).getPrice_incl();

                strb.append(BR);

                if(products.get(i).getCount()!=1) {
                    // 2 x 2.15
                    strb.append("$bighw$");
                    s = products.get(i).getCount() + " x ";
                    strb.append(s);
                    strb.append("$big$");
                    s = EURO + Rounder.round(products.get(i).getPrice_excl());
                    strb.append(s);
                    strb.append(BR);
                }
                s = "#"+products.get(i).getReference()+" ";
                strb.append(s);
                strb.append(BR);
                // All Star Product                 4.30
                strb.append("$bighw$");
                s = StringHelper.swapU(products.get(i).getName().toUpperCase());
                strb.append(s);
                strb.append("$big$");
                String totalPriceForThisProduct = Rounder.round(products.get(i).getCount() * products.get(i).getPrice_excl());
                s = alignRightSpecial((EURO + totalPriceForThisProduct), products.get(i).getName().length());
                strb.append(s);
                strb.append(BR);
     


                totalPriceExcl += (priceEx * products.get(i).getCount());
                totalPriceIncl += (priceInc * products.get(i).getCount());
            }
        }
        strb.append(getLineOf('=', CHARCOUNT_BIG));
        strb.append(BR);

        // Date
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        strb.append("$big$" + BR + BR);
        strb.append(currentDateTimeString);
        strb.append(" " + getString(R.string.waiter) + " " + settings.getWaiter());
        strb.append(BR);
        //strb.append("Bondrucker1"); //Don't need that :O

        for(int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");

        sendPrintJob(strb.toString());
    }

    // sendPrintJob bill layout
    public String getBillContent() {
        String tableNr = SharedPrefHelper.getString(getApplicationContext(), "tableNr");
        String s;
        StringBuilder strb = new StringBuilder("");

        strb.append(INITIATE);
        strb.append(CHAR_TABLE_EURO);
        strb.append(BR);

        strb.append("$bigw$");
        if(!settings.getNameLine1().equals("")) {
            strb.append(alignCenterBigw(settings.getNameLine1().toUpperCase()));
            strb.append(BR);
        }

        if(!settings.getNameLine2().equals("")) {
            strb.append(alignCenterBigw(settings.getNameLine2().toUpperCase()));
            strb.append(BR);
        }

        if(!settings.getAddrLine1().equals("")) {
            strb.append(alignCenterBigw(settings.getAddrLine1().toUpperCase()));
            strb.append(BR);
        }

        if(!settings.getAddrLine2().equals("")) {
            strb.append(alignCenterBigw(settings.getAddrLine2().toUpperCase()));
            strb.append(BR);
        }

        if(!settings.getTelLine().equals("")) {
            strb.append(alignCenterBigw(settings.getTelLine().toUpperCase()));
            strb.append(BR);
        }

        if(!settings.getTaxLine().equals("")) {
            strb.append(alignCenterBigw(settings.getTaxLine().toUpperCase()));
            strb.append(BR);
        }

        if(!settings.getExtraLine().equals("")) {
            strb.append(alignCenterBigw(settings.getExtraLine().toUpperCase()));
            strb.append(BR);
        }

        s = "$bighw$" + BR + alignCenterBigw(getString(R.string.bill)) + BR;
        strb.append(s);

        if(!tableNr.equals("")) {
            strb.append("$bighw$");
            strb.append(alignCenterBigw(getString(R.string.table_nr).toUpperCase() + tableNr));
            strb.append(BR);
        }

        strb.append(BR + "$big$" + BR);
        strb.append(getLineOf('=', CHARCOUNT_BIG));

        double totalPriceExcl = 0;
        double totalPriceIncl = 0;

        for(int i = 0; i < products.size(); i++) {
            if(products.get(i).getCount() < 1) continue;

            double priceEx = products.get(i).getPrice_excl();
            double priceInc = products.get(i).getPrice_incl();

            strb.append(BR);

            if(products.get(i).getCount()!=1) {
                // 2 x 2.15
                s = products.get(i).getCount() + " x " + EURO + Rounder.round(products.get(i).getPrice_excl());
                strb.append(s);
                strb.append(BR);
            }
            // All Star Product                 4.30
            String totalPriceForThisProduct = Rounder.round(products.get(i).getCount() * products.get(i).getPrice_excl());
            s = StringHelper.swapU(products.get(i).getName().toUpperCase())
                    + alignRight((EURO + totalPriceForThisProduct), products.get(i).getName().length());
            strb.append(s);
            strb.append(BR);

            // Not on last line
            if(i != products.size() - 1)
                strb.append(getLineOf('-', CHARCOUNT_BIG));

            totalPriceExcl += (priceEx * products.get(i).getCount());
            totalPriceIncl += (priceInc * products.get(i).getCount());

        }

        strb.append(getLineOf('=', CHARCOUNT_BIG));
        strb.append(BR);

        // Total excl
        s = getString(R.string.price_excl) +
                alignRight((EURO + Rounder.round(totalPriceExcl)), (getString(R.string.price_excl)).length());
        strb.append(s);
        strb.append(BR);

        // Tax
        String tax = Rounder.round(totalPriceIncl - totalPriceExcl);
        s = getString(R.string.tax) +
                alignRight((EURO + tax), (getString(R.string.tax)).length());
        strb.append(s);

        // Total incl
        strb.append(BR);
        strb.append("$bigw$");
        String totalPriceInc = Rounder.round(totalPriceIncl);

        s = getString(R.string.total) +
                alignRightBigw((EURO + totalPriceInc), (getString(R.string.total)).length());
        strb.append(s);

        // Date
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

        strb.append("$big$" + BR + BR);
        strb.append(currentDateTimeString);
        s = " " + getString(R.string.waiter) + " " + settings.getWaiter();
        strb.append(s);
        strb.append(BR + BR);

        return strb.toString();
    }

    private String getBillFooter() {
        StringBuilder strb = new StringBuilder("");
        strb.append("$big$");

        // Served by waiter
        if(!settings.getWaiter().equals(""))
            strb.append(alignCenter(getString(R.string.served_by) + " " + settings.getWaiter())  + "$intro$");

        strb.append(alignCenter(getString(R.string.tyvm)) + "$intro$");
        strb.append(alignCenter(getString(R.string.visit_again)));

        for(int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");
        return strb.toString();
    }

    private String getTaxFooter() {
        StringBuilder strb = new StringBuilder("");
        strb.append("$big$");

        strb.append(getLineOf('*', CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(alignCenter("Bewirtungsaufwand-Angaben"));
        strb.append(BR);
        strb.append(alignCenter("(Par.4 Abs.5 Ziff.2 EstG)"));
        strb.append(BR);
        strb.append(getLineOf('*', CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);

        strb.append("Bewirtete Person(en):");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf('-', CHARCOUNT_BIG));
        strb.append(BR);

        strb.append("Anlass der Bewirtung:");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf('-', CHARCOUNT_BIG));
        strb.append(BR);

        strb.append("Höhe der Aufwendungen:");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append("bei Bewirtung im Restaurant");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append("in anderen Fällen");
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf('-', CHARCOUNT_BIG));
        strb.append(BR);
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append(BR);
        strb.append("Ort      Datum");
        strb.append(BR);
        strb.append(BR);
        strb.append(BR);
        strb.append(BR);
        strb.append(getLineOf(DOT, CHARCOUNT_BIG));
        strb.append("Unterschrift");
        strb.append(BR);

        for(int i = 0; i < 8; i++) {
            strb.append(BR);
        }
        strb.append("$cut$");
        return strb.toString();
    }

    private String getLineOf(char c, int lineSize) {
        StringBuilder strb = new StringBuilder("");
        for(int i = 0; i < lineSize; i++) {
            strb.append(c);
        }
        return strb.toString();
    }

    private String getLineOf(String s, int lineSize) {
        StringBuilder strb = new StringBuilder("");
        for(int i = 0; i < lineSize; i++) {
            strb.append(s);
        }
        return strb.toString();
    }

    private void sendPrintJob(String dataToPrint) {
        Intent intentPrint = new Intent();
        intentPrint.setAction(Intent.ACTION_SEND);
        intentPrint.putExtra(Intent.EXTRA_TEXT, dataToPrint);
//        intentPrint.putExtra("printer_type_id", "1");// For IP
//        intentPrint.putExtra("printer_ip", settings.getPrinterIp());
//        intentPrint.putExtra("printer_port", "9100");
        intentPrint.setType("text/plain");
        /*this.*/startActivity(intentPrint);
    }


    private String alignRight(String s) {
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIG - length;
        String newstr = "";
        for(int i = 0; i < paddingLeft; i++) {
            newstr += " ";
        }
        newstr += s;
        return newstr;
    }

    private String alignRight(String s, int offsetLeft) {
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIG - length;
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i< StringHelper.checkCount(s,EURO); i++){
            paddingLeft += EURO.length() -1;
        }
        for (int i = 0; i< StringHelper.checkCount(s,u); i++){
            paddingLeft += u.length() -1;
        }
        for (int i = 0; i< StringHelper.checkCount(s,U); i++){
            paddingLeft += U.length() -1;
        }
        String newstr = "";
        int j = (offsetLeft +s.length())/CHARCOUNT_BIG;
        if((offsetLeft +s.length())< CHARCOUNT_BIG*(j+1)) {
            for (int i = 0; i < (paddingLeft - offsetLeft + CHARCOUNT_BIG * j); i++) {
                newstr += " ";
            }
        }
        newstr += s;
        return newstr;
    }

    private String alignRightBigw(String s, int offsetLeft) {
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIGW - length;
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i< StringHelper.checkCount(s,EURO); i++){
            paddingLeft += EURO.length() -1;
        }
        for (int i = 0; i< StringHelper.checkCount(s,u); i++){
            paddingLeft += u.length() -1;
        }
        for (int i = 0; i< StringHelper.checkCount(s,U); i++){
            paddingLeft += U.length() -1;
        }
        String newstr = "";
        int j = (offsetLeft +s.length())/CHARCOUNT_BIGW;
        if((offsetLeft +s.length())< CHARCOUNT_BIGW*(j+1)) {
            for (int i = 0; i < (paddingLeft - offsetLeft + CHARCOUNT_BIGW * j); i++) {
                newstr += " ";
            }
        }
        newstr += s;
        return newstr;
    }


    private String alignRightSpecial(String s, int offsetLeft) {// because there is 2 different size of text on the line
        int length = s.length();
        int paddingLeft = CHARCOUNT_BIG - length;
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i< StringHelper.checkCount(s,EURO); i++){
            paddingLeft += EURO.length() -1;
        }
        for (int i = 0; i< StringHelper.checkCount(s,u); i++){
            paddingLeft += u.length() -1;
        }
        for (int i = 0; i< StringHelper.checkCount(s,U); i++){
            paddingLeft += U.length() -1;
        }
        String newstr = "";
        int j = (offsetLeft*2 +s.length())/CHARCOUNT_BIG;
        if((offsetLeft*2 +s.length())< CHARCOUNT_BIG*(j+1)) {
            for (int i = 0; i < (paddingLeft - offsetLeft * 2 + CHARCOUNT_BIG * j); i++) {
                newstr += " ";
            }
        }
        newstr += s;
        return newstr;
    }

    private String alignCenter(String s) {
        int length = s.length();
        int totalSpaceLeft = CHARCOUNT_BIG - length;
        int spaceOnBothSides = totalSpaceLeft / 2;
        String newstr = "";
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i< StringHelper.checkCount(s,EURO); i++){
            for(int j=0; j < (EURO.length() -1)/2; j++ ){
                newstr += " ";
            }
        }
        for (int i = 0; i< StringHelper.checkCount(s,u); i++){
            for(int j=0; j < (u.length() -1)/2; j++ ){
                newstr += " ";
            }
        }
        for (int i = 0; i< StringHelper.checkCount(s,U); i++){
            for(int j=0; j < (U.length() -1)/2; j++ ){
                newstr += " ";
            }
        }
        for(int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }
        newstr += s;
        for(int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }

        return newstr;
    }

    private String alignCenterBigw(String s) {
        int length = s.length();
        int totalSpaceLeft = CHARCOUNT_BIGW - length;
        int spaceOnBothSides = totalSpaceLeft / 2;
        String newstr = "";
        // EURO length counts as more than 1 character and bugs alignment
        for (int i = 0; i< StringHelper.checkCount(s,EURO); i++){
            for(int j=0; j < (EURO.length() -1)/2; j++ ){
                newstr += " ";
            }
        }
        for (int i = 0; i< StringHelper.checkCount(s,u); i++){
            for(int j=0; j < (u.length() -1)/2; j++ ){
                newstr += " ";
            }
        }
        for (int i = 0; i< StringHelper.checkCount(s,U); i++){
            for(int j=0; j < (U.length() -1)/2; j++ ){
                newstr += " ";
            }
        }
        for(int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }
        newstr += s;
        for(int i = 0; i < spaceOnBothSides; i++) {
            newstr += " ";
        }

        return newstr;
    }



}
