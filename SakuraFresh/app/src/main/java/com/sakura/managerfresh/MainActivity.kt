package com.sakura.managerfresh

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class MainActivity : Activity() {
    private val baseUrl = "https://qumtsbxysauhujggdqjk.supabase.co"
    private val anonKey = "sb_publishable_GaWDWm3CXY_p1I45hWohbQ_qIux32ow"
    private val http = OkHttpClient()
    private var accessToken: String? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView

    private fun request(path: String, method: String = "GET", json: String? = null): Request {
        val builder = Request.Builder()
            .url(baseUrl + path)
            .header("apikey", anonKey)
            .header("Authorization", "Bearer " + (accessToken ?: anonKey))
        if (json != null) {
            builder.header("Content-Type", "application/json")
                .method(method, json.toRequestBody("application/json; charset=utf-8".toMediaType()))
        } else builder.method(method, null)
        return builder.build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    private fun setup(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(Color.rgb(255, 246, 249))
        }
        val heading = TextView(this).apply {
            text = title
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(158, 48, 91))
            setPadding(0, 8, 0, 20)
        }
        root.addView(heading)
        status = TextView(this).apply { textSize = 14f; setTextColor(Color.DKGRAY) }
        root.addView(status)
        setContentView(root)
    }

    private fun showLogin() {
        setup("🌸 Sakura Manager")
        val email = EditText(this).apply { hint = "Email"; setSingleLine(true) }
        val password = EditText(this).apply { hint = "Password"; inputType = 129; setSingleLine(true) }
        root.addView(email)
        root.addView(password)
        val loginButton = Button(this).apply { text = "Login" }
        root.addView(loginButton)
        loginButton.setOnClickListener {
            status.text = "جاري تسجيل الدخول..."
            val payload = JSONObject().put("email", email.text.toString()).put("password", password.text.toString()).toString()
            val req = Request.Builder().url("$baseUrl/auth/v1/token?grant_type=password")
                .header("apikey", anonKey).header("Authorization", "Bearer $anonKey")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
            http.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { runOnUiThread { status.text = "تعذر الاتصال: ${e.message}" } }
                override fun onResponse(call: Call, response: Response) {
                    val text = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        accessToken = JSONObject(text).optString("access_token")
                        runOnUiThread { home() }
                    } else runOnUiThread { status.text = "فشل الدخول: $text" }
                }
            })
        }
    }

    private fun home() {
        setup("🌸 Sakura Manager")
        val menu = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        root.addView(menu)
        addButton(menu, "المنتجات") { loadProducts() }
        addButton(menu, "الطلبات") { loadOrders() }
        addButton(menu, "خروج") { accessToken = null; showLogin() }
        loadProducts()
    }

    private fun addButton(parent: LinearLayout, label: String, action: () -> Unit) {
        val button = Button(this).apply { text = label }
        parent.addView(button, LinearLayout.LayoutParams(0, -2, 1f))
        button.setOnClickListener { action() }
    }

    private fun contentList(title: String): LinearLayout {
        setup(title)
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        return list
    }

    private fun loadProducts() {
        val list = contentList("المنتجات والمخزون")
        status.text = "تحميل المنتجات..."
        http.newCall(request("/rest/v1/products?select=id,name,price,cost,stock,active,store_visible&order=name.asc")).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { status.text = e.message ?: "خطأ اتصال" } }
            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string().orEmpty()
                runOnUiThread {
                    if (!response.isSuccessful) { status.text = "خطأ: $text"; return@runOnUiThread }
                    val arr = JSONArray(text)
                    status.text = "عدد المنتجات: ${arr.length()}"
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        val row = TextView(this@MainActivity).apply {
                            textSize = 16f
                            setTextColor(Color.BLACK)
                            setPadding(10, 14, 10, 14)
                            this.text = "${p.optString("name")}\nالسعر: ${p.optString("price")} | التكلفة: ${p.optString("cost")} | المخزون: ${p.optString("stock")}\n" + if (p.optBoolean("store_visible")) "ظاهر بالمتجر" else "مخفي"
                        }
                        list.addView(row)
                        list.addView(View(this@MainActivity).apply { setBackgroundColor(Color.LTGRAY) }, LinearLayout.LayoutParams(-1, 1))
                    }
                }
            }
        })
    }

    private fun loadOrders() {
        val list = contentList("طلبات المتجر")
        status.text = "تحميل الطلبات..."
        http.newCall(request("/rest/v1/orders?select=*&order=created_at.desc")).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { status.text = e.message ?: "خطأ اتصال" } }
            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string().orEmpty()
                runOnUiThread {
                    if (!response.isSuccessful) { status.text = "خطأ: $text"; return@runOnUiThread }
                    val arr = JSONArray(text)
                    status.text = "عدد الطلبات: ${arr.length()}"
                    for (i in 0 until arr.length()) {
                        val order = arr.getJSONObject(i)
                        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 12, 12, 12) }
                        row.addView(TextView(this@MainActivity).apply {
                            textSize = 16f
                            setTextColor(Color.BLACK)
                            this.text = "طلب ${order.optString("id")}\n${order.optString("customer_name")} | ${order.optString("phone")}\n${order.optString("total")} ج.م | الحالة: ${order.optString("status")}"
                        })
                        val actions = LinearLayout(this@MainActivity)
                        row.addView(actions)
                        if (order.optString("status") == "new") addButton(actions, "تأكيد") { updateOrder(order.optString("id"), "confirmed") }
                        addButton(actions, "إلغاء") { updateOrder(order.optString("id"), "cancelled") }
                        list.addView(row)
                    }
                }
            }
        })
    }

    private fun updateOrder(id: String, state: String) {
        val payload = JSONObject().put("p_order_id", id).put("p_status", state).toString()
        http.newCall(request("/rest/v1/rpc/set_store_order_status", "POST", payload)).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { runOnUiThread { status.text = e.message ?: "فشل التحديث" } }
            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string().orEmpty()
                runOnUiThread {
                    status.text = if (response.isSuccessful) "تم تحديث حالة الطلب والمخزون" else "فشل التحديث: $text"
                    loadOrders()
                }
            }
        })
    }
}
