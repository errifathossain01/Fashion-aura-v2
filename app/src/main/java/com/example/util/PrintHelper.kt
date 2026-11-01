package com.example.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.OrderEntity

object PrintHelper {

    fun generateBulkSlipsText(orders: List<OrderEntity>, useBanglaDigits: Boolean): String {
        val sb = StringBuilder()
        sb.append("================================\n")
        sb.append("📦 FASHION AURA SHIPPING SLIPS\n")
        sb.append("মোট অর্ডার: ${BanglaFormatter.formatNumber(orders.size, useBanglaDigits)} টি\n")
        sb.append("================================\n\n")

        orders.forEachIndexed { index, order ->
            val numStr = BanglaFormatter.formatNumber(index + 1, useBanglaDigits)
            val codStr = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)
            val tracking = if (order.courierTrackingId.isNotBlank()) order.courierTrackingId else "অপেক্ষারত (Pending)"

            sb.append("[$numStr] INVOICE: ${order.invoiceNo}\n")
            sb.append("কুরিয়ার ট্র্যাকিং: $tracking\n")
            sb.append("গ্রাহক: ${order.customerName}\n")
            sb.append("ফোন: ${order.customerPhone}\n")
            sb.append("ঠিকানা: ${order.customerAddress}, ${order.district}\n")
            if (order.items.isNotEmpty()) {
                val itemStr = order.items.joinToString(", ") { item ->
                    "${item.name} (${item.size}${if (item.color.isNotBlank()) ", " + item.color else ""})"
                }
                sb.append("আইটেম: $itemStr\n")
            }
            sb.append("ক্যাশ অন ডেলিভারি (COD): $codStr\n")
            if (order.specialInstructions.isNotBlank()) {
                sb.append("নোট: ${order.specialInstructions}\n")
            }
            sb.append("--------------------------------\n\n")
        }

        return sb.toString()
    }

    fun printShippingLabels(context: Context, orders: List<OrderEntity>, useBanglaDigits: Boolean) {
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Fashion Aura Shipping Labels</title>
                <style>
                    @page {
                        size: 4in 6in;
                        margin: 4mm;
                    }
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        margin: 0;
                        padding: 0;
                        background: #ffffff;
                        color: #111827;
                    }
                    .label-page {
                        page-break-after: always;
                        border: 2px solid #000000;
                        border-radius: 8px;
                        padding: 14px;
                        margin-bottom: 20px;
                        box-sizing: border-box;
                    }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        border-bottom: 2px solid #000000;
                        padding-bottom: 8px;
                        margin-bottom: 10px;
                    }
                    .brand {
                        font-size: 18px;
                        font-weight: 900;
                        color: #8B1D3B;
                        letter-spacing: 1px;
                        text-transform: uppercase;
                    }
                    .invoice-pill {
                        font-size: 15px;
                        font-weight: bold;
                        background: #f3f4f6;
                        padding: 4px 10px;
                        border-radius: 6px;
                        border: 1px solid #d1d5db;
                    }
                    .tracking-section {
                        background: #f9fafb;
                        border: 1px dashed #9ca3af;
                        border-radius: 6px;
                        padding: 8px;
                        text-align: center;
                        margin-bottom: 12px;
                    }
                    .tracking-id {
                        font-family: monospace;
                        font-size: 20px;
                        font-weight: 800;
                        letter-spacing: 2px;
                        color: #15803d;
                    }
                    .barcode-mock {
                        font-family: monospace;
                        font-size: 24px;
                        letter-spacing: -2px;
                        margin-top: 2px;
                        user-select: none;
                    }
                    .section-title {
                        font-size: 11px;
                        font-weight: 700;
                        color: #6b7280;
                        text-transform: uppercase;
                        margin-bottom: 3px;
                    }
                    .customer-name {
                        font-size: 17px;
                        font-weight: 800;
                        margin-bottom: 4px;
                    }
                    .phone-number {
                        font-size: 16px;
                        font-weight: 800;
                        color: #8B1D3B;
                        font-family: monospace;
                        margin-bottom: 6px;
                    }
                    .address {
                        font-size: 13px;
                        line-height: 1.4;
                        margin-bottom: 10px;
                        color: #1f2937;
                    }
                    .items-box {
                        border-top: 1px solid #e5e7eb;
                        border-bottom: 1px solid #e5e7eb;
                        padding: 6px 0;
                        margin-bottom: 10px;
                        font-size: 12px;
                    }
                    .cod-box {
                        background: #fef2f2;
                        border: 2px solid #ef4444;
                        border-radius: 6px;
                        padding: 10px;
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                    }
                    .cod-title {
                        font-size: 14px;
                        font-weight: 800;
                        color: #991b1b;
                    }
                    .cod-amount {
                        font-size: 22px;
                        font-weight: 900;
                        color: #991b1b;
                    }
                    .footer {
                        margin-top: 8px;
                        text-align: center;
                        font-size: 10px;
                        color: #9ca3af;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        orders.forEach { order ->
            val codStr = BanglaFormatter.formatCurrency(order.codAmount, useBanglaDigits)
            val trackingText = if (order.courierTrackingId.isNotBlank()) order.courierTrackingId else "STEADFAST COURIER"
            val itemsFormatted = order.items.joinToString("<br>") { item ->
                "• <b>${item.name}</b> - সাইজ: ${item.size}${if (item.color.isNotBlank()) ", রঙ: ${item.color}" else ""}"
            }

            htmlBuilder.append("""
                <div class="label-page">
                    <div class="header">
                        <div class="brand">Fashion Aura</div>
                        <div class="invoice-pill">INV: ${order.invoiceNo}</div>
                    </div>
                    
                    <div class="tracking-section">
                        <div class="section-title">Courier Consignment / Tracking</div>
                        <div class="tracking-id">$trackingText</div>
                        <div class="barcode-mock">||||||||||||||||||||||||||||||||||||||||||</div>
                    </div>

                    <div class="section-title">Recipient / ডেলিভারি প্রাপক:</div>
                    <div class="customer-name">${order.customerName}</div>
                    <div class="phone-number">📞 ${order.customerPhone}</div>
                    <div class="address">📍 ${order.customerAddress}, <b>${order.district}</b></div>

                    <div class="items-box">
                        <div class="section-title">Package Contents:</div>
                        $itemsFormatted
                    </div>

                    <div class="cod-box">
                        <div class="cod-title">CASH ON DELIVERY (COD):</div>
                        <div class="cod-amount">$codStr</div>
                    </div>

                    <div class="footer">
                        Fashion Aura Boutique • Standard Delivery Partner: Steadfast
                    </div>
                </div>
            """.trimIndent())
        }

        htmlBuilder.append("""
            </body>
            </html>
        """.trimIndent())

        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter("FashionAura_Labels_${System.currentTimeMillis()}")
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager?.print("Fashion Aura Labels (${orders.size})", printAdapter, printAttributes)
                }
            }
            webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)
        } catch (_: Exception) {
            // Fallback gracefully if print service is unavailable on test runner
        }
    }

    fun generateRiderManifestText(orders: List<OrderEntity>, useBanglaDigits: Boolean): String {
        val totalCod = orders.sumOf { it.codAmount }
        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
        return buildString {
            appendLine("📄 FASHION AURA — কুরিয়ার রাইডার হ্যান্ডওভার ম্যানিফেস্ট")
            appendLine("তারিখ: $dateStr")
            appendLine("মোট পার্সেল: ${BanglaFormatter.formatNumber(orders.size, useBanglaDigits)} টি | মোট COD: ${BanglaFormatter.formatCurrency(totalCod, useBanglaDigits)}")
            appendLine("==================================================")
            orders.forEachIndexed { idx, o ->
                val sl = idx + 1
                val trk = if (o.courierTrackingId.isNotBlank()) o.courierTrackingId else "পেন্ডিং"
                val cod = BanglaFormatter.formatCurrency(o.codAmount, useBanglaDigits)
                appendLine("$sl. [${o.invoiceNo}] CID: $trk")
                appendLine("   গ্রাহক: ${o.customerName} (${o.customerPhone})")
                appendLine("   ঠিকানা: ${o.customerAddress}, ${o.district}")
                appendLine("   COD: $cod")
                appendLine("--------------------------------------------------")
            }
            appendLine("✍️ রাইডারের নাম: ____________________")
            appendLine("✍️ রাইডারের স্বাক্ষর: ____________________")
            appendLine("Fashion Aura Warehouse Management")
        }
    }

    fun printRiderManifest(context: Context, orders: List<OrderEntity>, useBanglaDigits: Boolean) {
        val totalCod = orders.sumOf { it.codAmount }
        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH).format(java.util.Date())
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("""
            <!DOCTYPE html><html><head><meta charset="utf-8">
            <title>Fashion Aura Rider Handover Manifest</title>
            <style>
                @page { size: A4 portrait; margin: 12mm; }
                body { font-family: -apple-system, sans-serif; font-size: 12px; color: #111; padding: 10px; }
                .header { border-bottom: 2px solid #8B1D3B; padding-bottom: 8px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: flex-end; }
                .brand { font-size: 22px; font-weight: bold; color: #8B1D3B; }
                .manifest-title { font-size: 14px; font-weight: bold; }
                .summary-bar { background: #fdf2f8; border: 1px solid #f472b6; border-radius: 6px; padding: 8px 14px; margin-bottom: 14px; display: flex; justify-content: space-between; font-weight: bold; }
                table { width: 100%; border-collapse: collapse; margin-bottom: 20px; font-size: 11px; }
                th { background: #8B1D3B; color: white; padding: 6px 8px; text-align: left; }
                td { border-bottom: 1px solid #e5e7eb; padding: 6px 8px; vertical-align: top; }
                .tr-even { background: #fafafa; }
                .sig-box { border: 1.5px dashed #9ca3af; border-radius: 6px; padding: 12px; margin-top: 20px; display: flex; justify-content: space-between; }
                .sig-item { width: 45%; }
                .sig-line { border-bottom: 1px solid #111; height: 30px; margin-top: 10px; }
            </style>
            </head><body>
            <div class="header">
                <div>
                    <div class="brand">Fashion Aura</div>
                    <div class="manifest-title">📄 কুরিয়ার রাইডার পিকআপ ও হ্যান্ডওভার শিট (Pickup Manifest)</div>
                </div>
                <div>তারিখ: $dateStr</div>
            </div>
            <div class="summary-bar">
                <span>📦 মোট পার্সেল: ${BanglaFormatter.formatNumber(orders.size, useBanglaDigits)} টি</span>
                <span>💰 সর্বমোট COD টাকা: ${BanglaFormatter.formatCurrency(totalCod, useBanglaDigits)}</span>
                <span>কুরিয়ার পার্টনার: Steadfast</span>
            </div>
            <table>
                <thead>
                    <tr>
                        <th style="width: 30px;">নং</th>
                        <th style="width: 75px;">ইনভয়েস</th>
                        <th style="width: 90px;">Consignment ID</th>
                        <th>গ্রাহকের নাম ও ফোন</th>
                        <th>ডেলিভারি ঠিকানা ও জেলা</th>
                        <th style="width: 80px; text-align: right;">COD টাকা</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())

        orders.forEachIndexed { i, o ->
            val sl = i + 1
            val cid = if (o.courierTrackingId.isNotBlank()) o.courierTrackingId else "-"
            val cod = BanglaFormatter.formatCurrency(o.codAmount, useBanglaDigits)
            val bg = if (i % 2 == 1) " class='tr-even'" else ""
            htmlBuilder.append("""
                <tr$bg>
                    <td>$sl</td>
                    <td><b>${o.invoiceNo}</b></td>
                    <td><code>$cid</code></td>
                    <td><b>${o.customerName}</b><br>${o.customerPhone}</td>
                    <td>${o.customerAddress}, <b>${o.district}</b></td>
                    <td style="text-align: right; font-weight: bold; color: #8B1D3B;">$cod</td>
                </tr>
            """.trimIndent())
        }

        htmlBuilder.append("""
                </tbody>
            </table>
            <div class="sig-box">
                <div class="sig-item">
                    <div><b>রাইডারের তথ্য (Steadfast Rider):</b></div>
                    <div style="margin-top: 6px;">রাইডারের নাম: _____________________________</div>
                    <div style="margin-top: 4px;">মোবাইল নম্বর: _____________________________</div>
                    <div class="sig-line"></div>
                    <div style="font-size: 10px; color: #666; margin-top: 4px;">রাইডারের স্বাক্ষর ও সিল</div>
                </div>
                <div class="sig-item">
                    <div><b>হস্তান্তরকারীর তথ্য (Fashion Aura Dispatcher):</b></div>
                    <div style="margin-top: 6px;">প্রেরকের নাম: _____________________________</div>
                    <div style="margin-top: 4px;">হ্যান্ডওভার সময়: _____________________________</div>
                    <div class="sig-line"></div>
                    <div style="font-size: 10px; color: #666; margin-top: 4px;">ডিসপ্যাচার স্বাক্ষর</div>
                </div>
            </div>
            </body></html>
        """.trimIndent())

        try {
            val webView = WebView(context)
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    val printAdapter = webView.createPrintDocumentAdapter("FashionAura_Manifest_${System.currentTimeMillis()}")
                    val printAttributes = PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build()
                    printManager?.print("Fashion Aura Manifest (${orders.size})", printAdapter, printAttributes)
                }
            }
            webView.loadDataWithBaseURL(null, htmlBuilder.toString(), "text/html", "UTF-8", null)
        } catch (_: Exception) {
        }
    }
}
