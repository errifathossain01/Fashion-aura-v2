package com.example.ui.theme

import com.example.data.model.AppLanguage

object AppStrings {

    fun get(language: AppLanguage, bn: String, en: String): String {
        return if (language == AppLanguage.BANGLA) bn else en
    }

    // Common
    fun appName(l: AppLanguage) = "Fashion Aura"
    fun orderManagement(l: AppLanguage) = get(l, "অর্ডার ম্যানেজমেন্ট", "Order Management")
    fun searchPlaceholder(l: AppLanguage) = get(l, "নাম, ফোন, ইনভয়েস দিয়ে খুঁজুন...", "Search by name, phone, order id...")
    fun total(l: AppLanguage) = get(l, "মোট", "Total")
    fun cod(l: AppLanguage) = get(l, "COD:", "COD:")
    fun size(l: AppLanguage) = get(l, "Size:", "Size:")
    fun qty(l: AppLanguage) = get(l, "Qty:", "Qty:")

    // Actions
    fun markAsShipped(l: AppLanguage) = get(l, "শিপ করুন ➔", "Ship Order ➔")
    fun stockArrivedReady(l: AppLanguage) = get(l, "স্টক এসেছে ➔ রেডি করুন", "Stock In ➔ Ready")
    fun bookCourier(l: AppLanguage) = get(l, "বুক কুরিয়ার", "Book Courier")
    fun enterIdOrBookCourier(l: AppLanguage) = get(l, "বুক কুরিয়ার", "Book Courier")
    fun newOrder(l: AppLanguage) = get(l, "নতুন অর্ডার", "New Order")
    fun smartPaste(l: AppLanguage) = get(l, "স্মার্ট পেস্ট", "Smart Paste")
    fun bulkSync(l: AppLanguage) = get(l, "বাল্ক সিঙ্ক", "Bulk Sync")
    fun undo(l: AppLanguage) = get(l, "পূর্বাবস্থায় ফিরুন", "Undo")
    fun saveOrder(l: AppLanguage) = get(l, "অর্ডার সেভ করুন", "Save Order")
    fun updateOrder(l: AppLanguage) = get(l, "অর্ডার আপডেট করুন", "Update Order")

    // Filter Chips & Labels
    fun allOrders(l: AppLanguage) = get(l, "সব অর্ডার", "All")
    fun preOrder(l: AppLanguage) = get(l, "প্রি-অর্ডার", "Pre-Order")
    fun regular(l: AppLanguage) = get(l, "রেগুলার", "Regular")
    fun readyToShip(l: AppLanguage) = get(l, "রেডি টু শিপ", "Ready to Ship")
    fun shipped(l: AppLanguage) = get(l, "শিপড", "Shipped")
    fun urgent(l: AppLanguage) = get(l, "জরুরি", "Urgent")
    fun scheduled(l: AppLanguage) = get(l, "শিডিউলড", "Scheduled")
    fun pendingSync(l: AppLanguage) = get(l, "কুরিয়ার সিঙ্ক বাকি", "Pending Sync")
    fun pendingCourier(l: AppLanguage) = get(l, "পেন্ডিং কুরিয়ার", "Pending Courier")

    // Section Headers & Common Buttons
    fun catalog(l: AppLanguage) = get(l, "ক্যাটালগ", "Catalog")
    fun addDress(l: AppLanguage) = get(l, "ড্রেস যোগ করুন", "Add Dress")
    fun select(l: AppLanguage) = get(l, "মার্ক করুন", "Select")
    fun filter(l: AppLanguage) = get(l, "ফিল্টার", "Filter")
    fun allDresses(l: AppLanguage) = get(l, "সব ড্রেস", "All Dresses")
    fun allSizes(l: AppLanguage) = get(l, "সব সাইজ", "All Sizes")
    fun inStock(l: AppLanguage) = get(l, "ইন স্টক", "In Stock")
    fun clearAll(l: AppLanguage) = get(l, "সব মুছুন", "Clear All")
    fun active(l: AppLanguage) = get(l, "সক্রিয়", "Active")
    fun recentOrders(l: AppLanguage) = get(l, "সাম্প্রতিক অর্ডারসমূহ", "Recent Orders")
    fun ordersCount(l: AppLanguage, count: String) = get(l, "$count টি অর্ডার", "$count orders")
    fun groupByDress(l: AppLanguage) = get(l, "ড্রেস অনুযায়ী সাজান", "Group by Dress")

    // Navigation
    fun navOrders(l: AppLanguage) = get(l, "অর্ডার", "Orders")
    fun navProducts(l: AppLanguage) = get(l, "ক্যাটালগ", "Catalog")
    fun navCatalog(l: AppLanguage) = get(l, "ক্যাটালগ", "Catalog")
    fun navAddOrder(l: AppLanguage) = get(l, "নতুন অর্ডার", "Add Order")
    fun navReports(l: AppLanguage) = get(l, "রিপোর্ট", "Reports")
    fun navSettings(l: AppLanguage) = get(l, "সেটিংস", "Settings")

    // Greeting
    fun greeting(l: AppLanguage, hour: Int): String {
        return if (l == AppLanguage.BANGLA) {
            when (hour) {
                in 5..11 -> "শুভ সকাল"
                in 12..15 -> "শুভ দুপুর"
                in 16..17 -> "শুভ বিকেল"
                in 18..20 -> "শুভ সন্ধ্যা"
                else -> "শুভ রাত্রি"
            }
        } else {
            when (hour) {
                in 5..11 -> "Good Morning"
                in 12..16 -> "Good Afternoon"
                in 17..20 -> "Good Evening"
                else -> "Good Night"
            }
        }
    }

    // Authentication & Login
    fun loginTitle(l: AppLanguage) = get(l, "লগইন", "Sign In")
    fun loginSubtitle(l: AppLanguage) = get(l, "ফ্যাশন অরা ওএমএস ম্যানেজমেন্ট", "Fashion Aura OMS Management")
    fun email(l: AppLanguage) = get(l, "ইমেইল", "Email")
    fun emailPlaceholder(l: AppLanguage) = get(l, "ইমেইল লিখুন", "Enter your email")
    fun password(l: AppLanguage) = get(l, "পাসওয়ার্ড", "Password")
    fun passwordPlaceholder(l: AppLanguage) = get(l, "পাসওয়ার্ড লিখুন", "Enter your password")
    fun signInButton(l: AppLanguage) = get(l, "লগইন করুন", "Sign In")
    fun forgotPassword(l: AppLanguage) = get(l, "পাসওয়ার্ড ভুলে গেছেন?", "Forgot Password?")
    fun resetPasswordTitle(l: AppLanguage) = get(l, "পাসওয়ার্ড রিসেট", "Reset Password")
    fun resetPasswordDescription(l: AppLanguage) = get(l, "আপনার একাউন্টের ইমেইল ঠিকানা দিন। আমরা পাসওয়ার্ড রিসেট লিংক পাঠাব।", "Enter your account email. We will send you a password reset link.")
    fun sendResetLink(l: AppLanguage) = get(l, "রিসেট লিংক পাঠান", "Send Reset Link")
    fun cancel(l: AppLanguage) = get(l, "বাতিল", "Cancel")
    fun emailVerificationPendingTitle(l: AppLanguage) = get(l, "ইমেইল ভেরিফিকেশন বাকি", "Email Verification Pending")
    fun emailVerificationPendingDesc(l: AppLanguage, email: String) = get(l, "$email ঠিকানায় ভেরিফিকেশন লিংক পাঠানো হয়েছে। দয়া করে ইনবক্স চেক করে লিংকে ক্লিক করুন।", "A verification link was sent to $email. Please check your inbox and verify your email.")
    fun resendVerificationEmail(l: AppLanguage) = get(l, "পুনরায় ভেরিফিকেশন লিংক পাঠান", "Resend Verification Link")
    fun resendCooldown(l: AppLanguage, seconds: Int) = get(l, "$seconds সেকেন্ড পর চেষ্টা করুন", "Resend in ${seconds}s")
    fun checkVerificationStatus(l: AppLanguage) = get(l, "ভেরিফিকেশন চেক করুন", "Check Verification Status")
    fun backToLogin(l: AppLanguage) = get(l, "লগইনে ফিরে যান", "Back to Login")
    fun errorInvalidEmail(l: AppLanguage) = get(l, "সঠিক ইমেইল ঠিকানা লিখুন", "Please enter a valid email address")
    fun errorEmptyPassword(l: AppLanguage) = get(l, "পাসওয়ার্ড খালি রাখা যাবে না", "Password cannot be empty")
    fun errorInvalidCredentials(l: AppLanguage) = get(l, "ভুল ইমেইল বা পাসওয়ার্ড। অনুগ্রহ করে পুনরায় চেষ্টা করুন।", "Invalid email or password. Please try again.")
    fun errorUserDisabled(l: AppLanguage) = get(l, "এই একাউন্টটি সাময়িকভাবে বন্ধ করা হয়েছে।", "This account has been disabled.")
    fun errorTooManyRequests(l: AppLanguage) = get(l, "অনেক বেশি ব্যর্থ চেষ্টা হয়েছে। সাময়িকভাবে স্থগিত করা হয়েছে, কিছুক্ষণ পর চেষ্টা করুন।", "Too many failed attempts. Access temporarily blocked. Please try again later.")
    fun errorNetwork(l: AppLanguage) = get(l, "ইন্টারনেট সংযোগ নেই অথবা নেটওয়ার্ক সমস্যা।", "Network connection error. Please check your internet connection.")
    fun errorGeneric(l: AppLanguage) = get(l, "লগইন ব্যর্থ হয়েছে। পুনরায় চেষ্টা করুন।", "Authentication failed. Please try again.")
    fun verificationEmailSent(l: AppLanguage) = get(l, "ভেরিফিকেশন লিংক পাঠানো হয়েছে! ইনবক্স চেক করুন।", "Verification email sent! Please check your inbox.")
    fun passwordResetSent(l: AppLanguage) = get(l, "পাসওয়ার্ড রিসেট লিংক পাঠানো হয়েছে! ইনবক্স চেক করুন।", "Password reset link sent! Please check your inbox.")
    fun emailVerifiedSuccess(l: AppLanguage) = get(l, "ইমেইল সফলভাবে ভেরিফাই হয়েছে!", "Email verified successfully!")
    fun emailNotYetVerified(l: AppLanguage) = get(l, "ইমেইল এখনও ভেরিফাই হয়নি। দয়া করে লিংকে ক্লিক করুন।", "Email not yet verified. Please click the link in your email.")

    // Stage B.1 Redesign Strings
    fun welcomeBack(l: AppLanguage) = get(l, "স্বাগতম", "Welcome Back")
    fun welcomeBackSubtitle(l: AppLanguage) = get(l, "আপনার স্টোর ও প্রোডাক্ট পরিচালনায় লগইন করুন", "Sign in to manage your store and products")
    fun orderManagementSystem(l: AppLanguage) = get(l, "অর্ডার ম্যানেজমেন্ট সিস্টেম", "ORDER MANAGEMENT SYSTEM")
    fun secureAccessFooter(l: AppLanguage) = get(l, "সুরক্ষিত এক্সেস • ফ্যাশন অরা ওএমএস", "Secure Access • Fashion Aura OMS")

    // Stage C-C: Owner Authorization & Claims Strings
    fun accessDeniedTitle(l: AppLanguage) = get(l, "এক্সেস অনুমোদিত নয়", "Access Restricted")
    fun accessDeniedDesc(l: AppLanguage, email: String) = get(l, "$email একাউন্টটিতে Fashion Aura OMS-এর ওনার (Owner) অনুমতি নেই। সঠিক ওনার একাউন্ট দিয়ে লগইন করুন।", "The account ($email) does not have Owner permissions for Fashion Aura OMS. Please sign in with an authorized Owner account.")
    fun errorAccessDeniedNotOwner(l: AppLanguage) = get(l, "এই একাউন্টটিতে ফ্যাশন অরা ওএমএস-এর ওনার (Owner) পারমিশন নেই।", "This account does not have Owner permissions for Fashion Aura OMS.")
    fun switchAccount(l: AppLanguage) = get(l, "অন্য একাউন্টে লগইন করুন", "Switch Account")
    fun offlineModeBanner(l: AppLanguage) = get(l, "অফলাইন মোড — লোকাল ক্যাশ থেকে পরিচালিত", "Offline Mode — Working from Local Cache")
}
