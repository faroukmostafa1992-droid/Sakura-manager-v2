package com.sakura.managerfresh

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException


class MainActivity : Activity() {
 private val supabaseUrl="https://qumtsbxysauhujggdqjk.supabase.co"
 private val anonKey="sb_publishable_GaWDWm3CXY_p1I45hWohbQ_qIux32ow"
 private val client=OkHttpClient()
 private var token:String?=null
 private lateinit var root:LinearLayout
 private lateinit var status:TextView
 private fun req(path:String, method:String="GET", body:String?=null, auth:String?=token):Request {
  val b=Request.Builder().url(supabaseUrl+path).header("apikey",anonKey).header("Authorization","Bearer "+(auth?:anonKey))
  if(body!=null) b.header("Content-Type","application/json").method(method,body.toRequestBodyCompat()) else b.method(method,null)
  return b.build()
 }
 private fun String.toRequestBodyCompat()=RequestBody.create(MediaType.parse("application/json; charset=utf-8"),this)
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); showLogin()}
 private fun base(title:String){ root=LinearLayout(this).apply{orientation=1;setPadding(28,24,28,24);setBackgroundColor(Color.rgb(255,246,249))}; val t=TextView(this).apply{text=title; textSize=24f;setTextColor(Color.rgb(158,48,91));gravity=Gravity.CENTER;setPadding(0,10,0,22)};root.addView(t); status=TextView(this).apply{textSize=14f;setTextColor(Color.DKGRAY)};root.addView(status);setContentView(root) }
 private fun showLogin(){base("🌸 Sakura Manager");val email=EditText(this).apply{hint="Email"};val pass=EditText(this).apply{hint="Password";inputType=129};root.addView(email);root.addView(pass);val btn=Button(this).apply{text="Login"};root.addView(btn);btn.setOnClickListener{status.text="جاري تسجيل الدخول…";login(email.text.toString(),pass.text.toString())} }
 private fun login(e:String,p:String){val payload=JSONObject().put("email",e).put("password",p).toString();val request=req("/auth/v1/token?grant_type=password","POST",payload,anonKey);client.newCall(request).enqueue(object:Callback{override fun onFailure(c:Call,x:IOException){runOnUiThread{status.text="تعذر الاتصال: "+x.message}};override fun onResponse(c:Call,r:Response){val s=r.body()?.string().orEmpty();if(r.isSuccessful){token=JSONObject(s).optString("access_token");runOnUiThread{home()}}else runOnUiThread{status.text="Login failed: $s"}}})}
 private fun home(){base("🌸 Sakura Manager");val bar=LinearLayout(this);bar.orientation=0;root.addView(bar);button(bar,"المنتجات"){loadProducts()};button(bar,"الطلبات"){loadOrders()};button(bar,"خروج"){token=null;showLogin()};loadProducts()}
 private fun button(parent:LinearLayout,label:String,action:()->Unit){val b=Button(this).apply{text=label};parent.addView(b,LinearLayout.LayoutParams(0,-2,1f));b.setOnClickListener{action()}}
 private fun listScreen(title:String){base(title);val scroll=ScrollView(this);val list=LinearLayout(this).apply{orientation=1};scroll.addView(list);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));return list}
 private fun loadProducts(){val list=listScreen("المنتجات والمخزون");status.text="تحميل…";client.newCall(req("/rest/v1/products?select=id,name,price,cost,stock,active,store_visible&order=sort_order.asc")).enqueue(object:Callback{override fun onFailure(c:Call,e:IOException){runOnUiThread{status.text=e.message}};override fun onResponse(c:Call,r:Response){val s=r.body()?.string().orEmpty();runOnUiThread{if(!r.isSuccessful){status.text="خطأ: $s";return@runOnUiThread};val a=JSONArray(s);status.text="عدد المنتجات: "+a.length();for(i in 0 until a.length()){val p=a.getJSONObject(i);val row=TextView(this@MainActivity).apply{textSize=16f;setTextColor(Color.BLACK);setPadding(8,12,8,12);text=p.optString("name")+"\nالسعر: "+p.optString("price")+" | التكلفة: "+p.optString("cost")+" | المخزون: "+p.optString("stock")+if(p.optBoolean("store_visible"))"\nظاهر بالمتجر" else "\nمخفي"};list.addView(row);list.addView(View(this@MainActivity).apply{setBackgroundColor(Color.LTGRAY;)} ,LinearLayout.LayoutParams(-1,1))}}})}
 private fun loadOrders(){val list=listScreen("الطلبات");status.text="تحميل الطلبات…";client.newCall(req("/rest/v1/orders?select=*&order=created_at.desc")).enqueue(object:Callback{override fun onFailure(c:Call,e:IOException){runOnUiThread{status.text=e.message}};override fun onResponse(c:Call,r:Response){val s=r.body()?.string().orEmpty();runOnUiThread{if(!r.isSuccessful){status.text="خطأ: $s";return@runOnUiThread};val a=JSONArray(s);status.text="عدد الطلبات: "+a.length();for(i in 0 until a.length()){val o=a.getJSONObject(i);val row=LinearLayout(this@MainActivity).apply{orientation=1;setPadding(12,14,12,14)};row.addView(TextView(this@MainActivity).apply{textSize=17f;setTextColor(Color.BLACK);text="طلب "+o.optString("id")+"\n"+o.optString("customer_name")+" | "+o.optString("phone")+"\n"+o.optString("total")+" ج.م | الحالة: "+o.optString("status")});val actions=LinearLayout(this@MainActivity);row.addView(actions);if(o.optString("status")=="new")button(actions,"تأكيد") {setOrderStatus(o.optString("id"),"confirmed")};button(actions,"إلغاء") {setOrderStatus(o.optString("id"),"cancelled")};list.addView(row)}}})}
 private fun setOrderStatus(id:String,state:String){val payload=JSONObject().put("p_order_id",id).put("p_status",state).toString();client.newCall(req("/rest/v1/rpc/set_store_order_status","POST",payload)).enqueue(object:Callback{override fun onFailure(c:Call,e:IOException){runOnUiThread{status.text=e.message}};override fun onResponse(c:Call,r:Response){val s=r.body()?.string().orEmpty();runOnUiThread{status.text=if(r.isSuccessful)"تم تحديث الطلب والمخزون" else "فشل: $s";loadOrders()}}})}
}