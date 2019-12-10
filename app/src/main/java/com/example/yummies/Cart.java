package com.example.yummies;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.JsonReader;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.yummies.Common.Common;
import com.example.yummies.Common.Config;
import com.example.yummies.Database.Database;
import com.example.yummies.Model.Order;
import com.example.yummies.Model.Request;
import com.example.yummies.ViewHolder.CartAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Cart extends AppCompatActivity {

    private static final int PAYPAL_REQUEST_CODE=9999;
    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference requests;

    public  TextView txtTotalPrice;
    Button btnPlace;

    List<Order> cart = new ArrayList<>();

    CartAdapter adapter;

    //paypal payment
    static PayPalConfiguration config=new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(Config.PAYPAL_CLIENT_ID);

    String address;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        ///init paypal
        Intent intent = new Intent(this, PayPalService.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
        startService(intent);


        //Firebase
        database = FirebaseDatabase.getInstance();
        requests=database.getReference("Requests");

        //Init
        recyclerView=(RecyclerView)findViewById(R.id.listCart);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        txtTotalPrice =(TextView)findViewById(R.id.total);
        btnPlace=(Button)findViewById(R.id.btnPlaceOrder);



        btnPlace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(cart.size( )> 0)
                showAlertDialog();
                else
                    Toast.makeText(Cart.this,"Your cart is empty!!!",Toast.LENGTH_SHORT).show();
            }
        });


        loadListFood();
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog=new AlertDialog.Builder(Cart.this);
        alertDialog.setTitle("One more step!");
        alertDialog.setMessage("Enter your address: ");

        final EditText edtAddress = new EditText(Cart.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );

        edtAddress.setLayoutParams(lp);
        alertDialog.setView(edtAddress);
        alertDialog.setIcon(R.drawable.ic_shopping_cart_black_24dp);

        alertDialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                //show paypal to payment
                address  = edtAddress.getText().toString();


                String  formatAmount = txtTotalPrice.getText().toString()
                        .replace("$","")
                        .replace(",","");

                PayPalPayment payPalPayment= new PayPalPayment(new BigDecimal(formatAmount),
                        "USD",
                        "Yummies Order",
                        PayPalPayment.PAYMENT_INTENT_SALE);
                Intent intent=new Intent(getApplicationContext(), PaymentActivity.class);
                intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION,config);
                intent.putExtra(PaymentActivity.EXTRA_PAYMENT,payPalPayment);
                startActivityForResult(intent,PAYPAL_REQUEST_CODE);

            }
        });

        alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode,data);
       if(requestCode ==PAYPAL_REQUEST_CODE)
       {
           if(resultCode==RESULT_OK)
           {
               PaymentConfirmation confirmation=data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
               if(confirmation !=null)
               {
                   try{
                       String paymentDetail = confirmation.toJSONObject().toString(4);
                       JSONObject jsonObject=new JSONObject(paymentDetail);


                       //create new request
                       Request request=new Request(
                               Common.currentUser.getPhone(),
                               Common.currentUser.getName(),
                               address,
                               txtTotalPrice.getText().toString(),
                               "0",
                               jsonObject.getJSONObject("response").getString("state"),
                               cart
                       );

                       //submit to firebase
                       String order_number = String.valueOf(System.currentTimeMillis());
                       requests.child(order_number)
                               .setValue(request);
                       //delete cart
                       new Database(getBaseContext()).cleanCart();

                       Toast.makeText(Cart.this,"Thank you , your oder has been successfully placed", Toast.LENGTH_SHORT).show();
                       finish();

                   }catch (JSONException e){
                       e.printStackTrace();
                   }
               }
           }
           else if (resultCode==Activity.RESULT_CANCELED)
               Toast.makeText(this,"Payment cancel",Toast.LENGTH_SHORT).show();
           else if (resultCode==PaymentActivity.RESULT_EXTRAS_INVALID)
               Toast.makeText(this,"Invalid payment",Toast.LENGTH_SHORT).show();
       }
    }

    private void loadListFood() {
        cart = new Database(this).getCarts();
        adapter=new CartAdapter(cart,this);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        //calculate total price
        float total=0;
        for(Order order:cart)
            total+=(Float.parseFloat(order.getPrice()))*(Integer.parseInt(order.getQuantity()));
        Locale locale=new Locale("en","US");
        NumberFormat fmt = NumberFormat.getCurrencyInstance(locale);

        txtTotalPrice.setText(fmt.format(total));
    }

    @Override
    public boolean onContextItemSelected(MenuItem  item){
        if(item.getTitle().equals(Common.DELETE))
            deleteCart(item.getOrder());
        return true;
    }

    private void deleteCart(int position) {
        cart.remove(position);
        new Database(this).cleanCart();
        for(Order item:cart)
            new Database(this).addToCart(item);

        loadListFood();
    }
}
