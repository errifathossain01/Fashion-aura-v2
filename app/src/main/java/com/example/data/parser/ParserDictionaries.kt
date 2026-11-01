package com.example.data.parser

/**
 * Extensible, testable dictionary collections and regex patterns
 * supporting Bengali, English, and Banglish input variations.
 */
object ParserDictionaries {

    val BANGLA_TO_ENG_DIGITS = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    // All 64 Districts of Bangladesh in canonical English and Bangla representation
    val BANGLADESH_DISTRICTS: Map<String, String> = mapOf(
        "Dhaka" to "ঢাকা", "Chittagong" to "চট্টগ্রাম", "Chattogram" to "চট্টগ্রাম",
        "Sylhet" to "সিলেট", "Rajshahi" to "রাজশাহী", "Khulna" to "খুলনা",
        "Barisal" to "বরিশাল", "Barishal" to "বরিশাল", "Rangpur" to "রংপুর",
        "Mymensingh" to "ময়মনসিংহ", "Sunamganj" to "সুনামগঞ্জ", "Comilla" to "কুমিল্লা",
        "Cumilla" to "কুমিল্লা", "Gazipur" to "গাজীপুর", "Narayanganj" to "নারায়ণগঞ্জ",
        "Tangail" to "টাঙ্গাইল", "Bogra" to "বগুড়া", "Bogura" to "বগুড়া",
        "Kushtia" to "কুষ্টিয়া", "Jessore" to "যশোর", "Jashore" to "যশোর",
        "Dinajpur" to "দিনাজপুর", "Faridpur" to "ফরিদপুর", "Pabna" to "পাবনা",
        "Noakhali" to "নোয়াখালী", "Feni" to "ফেনী", "Brahmanbaria" to "ব্রাহ্মণবাড়িয়া",
        "Habiganj" to "হবিগঞ্জ", "Moulvibazar" to "মৌলভীবাজার", "Cox's Bazar" to "কক্সবাজার",
        "Coxs Bazar" to "কক্সবাজার", "Chandpur" to "চাঁদপুর", "Lakshmipur" to "লক্ষ্মীপুর",
        "Sirajganj" to "সিরাজগঞ্জ", "Natore" to "নাটোর", "Naogaon" to "নওগাঁ",
        "Chapainawabganj" to "চাঁপাইনবাবগঞ্জ", "Joypurhat" to "জয়পুরহাট", "Gaibandha" to "গাইবান্ধা",
        "Kurigram" to "কুড়িগ্রাম", "Lalmonirhat" to "লালমনিরহাট", "Nilphamari" to "নীলফামারী",
        "Panchagarh" to "পঞ্চগড়", "Thakurgaon" to "ঠাকুরগাঁও", "Sherpur" to "শেরপুর",
        "Jamalpur" to "জামালপুর", "Netrokona" to "নেত্রকোনা", "Kishoreganj" to "কিশোরগঞ্জ",
        "Manikganj" to "মানিকগঞ্জ", "Munshiganj" to "মুন্সিগঞ্জ", "Narsingdi" to "নরসিংদী",
        "Gopalganj" to "গোপালগঞ্জ", "Madaripur" to "মাদারীপুর", "Rajbari" to "রাজবাড়ী",
        "Shariatpur" to "শরীয়তপুর", "Bagerhat" to "বাগেরহাট", "Chuadanga" to "চুয়াডাঙ্গা",
        "Jhenaidah" to "ঝিনাইদহ", "Magura" to "মাগুরা", "Meherpur" to "মেহেরপুর",
        "Narail" to "নড়াইল", "Satkhira" to "সাতক্ষীরা", "Barguna" to "বরগুনা",
        "Bhola" to "ভোলা", "Jhalokati" to "ঝালকাঠি", "Patuakhali" to "পটুয়াখালী",
        "Pirojpur" to "পিরোজপুর", "Bandarban" to "বান্দরবান", "Khagrachhari" to "খাগড়াছড়ি",
        "Rangamati" to "রাঙ্গামাটি"
    )

    // Dhaka Suburban locations mapped to ৳120 delivery
    val DHAKA_SUBURBAN_KEYWORDS = listOf(
        "savar", "gazipur", "keraniganj", "tongi", "dhamrai", "ashulia", "narayanganj",
        "সাভার", "কেরানীগঞ্জ", "টঙ্গী", "ধামরাই", "আশুলিয়া", "নারায়ণগঞ্জ"
    )

    // Address landmark and structure keywords
    val ADDRESS_STRUCTURAL_KEYWORDS = listOf(
        "রোড", "রোড নং", "বাড়ি", "বাসা", "বাসা নং", "হোল্ডিং", "হোল্ডিং নং", "লেন", "সেক্টর", "সেক্টর নং",
        "থানা", "মডেল থানা", "জেলা", "উপজেলা", "পোস্ট", "পোস্ট অফিস", "ডাকঘর", "ফ্ল্যাট", "গ্রাম", "মহল্লা",
        "বাজার", "মোড়", "কমপ্লেক্স", "হাসপাতাল", "স্কুল", "কলেজ", "মডেল টাউন", "ওয়ার্ড", "ওয়ার্ড", "ব্লক",
        "road", "house", "sector", "block", "lane", "thana", "dist", "district", "village",
        "flat", "floor", "holding", "bazar", "market", "mor", "complex", "hospital", "nagar", "housing", "ward"
    )

    // Curated list of known Thanas / Upazilas across Bangladesh for contextual matching without guessing
    val KNOWN_UPAZILAS_THANAS = listOf(
        "Puthia", "Shantigonj", "Shahrasti", "Mirpur", "Uttara", "Dhanmondi", "Gulshan", "Banani",
        "Mohammadpur", "Badda", "Tejgaon", "Motijheel", "Khilgaon", "Jatrabari", "Paltan", "Ramna",
        "Keraniganj", "Savar", "Dhamrai", "Tongi", "Hathazari", "Patiya", "Sitakunda", "Rangunia",
        "Boalkhali", "Mirsharai", "Kotwali", "Chandgaon", "Panchlaish", "Double Mooring", "Sreemangal",
        "Kamalganj", "Kulaura", "Beanibazar", "Golapganj", "Zakiganj", "Kanaighat", "Fenchuganj",
        "Baniachong", "Nabiganj", "Madhabpur", "Chhatak", "Jagannathpur", "Derai", "Tahirpur",
        "Durgapur", "Kendua", "Madan", "Barhatta", "Kalmakanda", "Bhaluka", "Trishal", "Muktagacha",
        "Gafargaon", "Ishwarganj", "Nandail", "Phulpur", "Haluaghat", "Bhairab", "Bajitpur", "Katiadi",
        "Pakundia", "Shahbagh", "New Market", "Lalbagh", "Chawkbazar", "Kamrangirchar", "Wari",
        "Gandaria", "Sutrapur", "Demra", "Kadamtali", "Shyampur", "Sabujbagh", "Mugda", "Rampura",
        "Shah Ali", "Darus Salam", "Kafrul", "Cantonment", "Vatara", "Bhashantek",
        "পুঠিয়া", "শাহরাস্তি", "শান্তিগঞ্জ", "মিরপুর", "উত্তরা", "ধানমন্ডি", "গুলশান", "বনানী",
        "মোহাম্মদপুর", "বাড্ডা", "তেজগাঁও", "মতিঝিল", "খিলগাঁও", "যাত্রাবাড়ী", "পল্টন", "রমনা",
        "কেরানীগঞ্জ", "সাভার", "ধামরাই", "টঙ্গী", "হাটহাজারী", "পটিয়া", "সীতাকুণ্ড", "রাঙ্গুনিয়া",
        "বোয়ালখালী", "মীরসরাই", "কোতোয়ালী", "শ্রীমঙ্গল", "কমলগঞ্জ", "কুলাউড়া", "বিয়ানীবাজার",
        "গোলাপগঞ্জ", "জকিগঞ্জ", "কানাইঘাট", "ফেঞ্চুগঞ্জ", "বানিয়াচং", "নবীগঞ্জ", "মাধবপুর",
        "ছাতক", "জগন্নাথপুর", "দিরাই", "তাহিরপুর", "ভৈরব", "বাজিতপুর", "কটিয়াদী", "পাকুন্দিয়া", "কাফরুল"
    )

    // Common Apparel types for boutique
    val DRESS_KEYWORDS = listOf(
        "কটন সালোয়ার কামিজ", "সালোয়ার কামিজ", "সালোয়ার কামিজ", "লং গাউন ড্রেস", "লং গাউন", "জর্জেট গাউন",
        "থ্রি পিস", "থ্রিপিস", "কুর্তি প্লাজো", "কুর্তি সেট", "সারারা সেট", "গারারা", "আনারকলি",
        "সিল্ক কুর্তি", "লন কুর্তি", "কটন কুর্তি", "কুর্তি", "লেহেঙ্গা", "শাড়ি", "শাড়ি", "আবায়া", "বোরকা",
        "টপস",
        "Salwar Kameez", "Georgette Gown", "Long Gown", "Three Piece", "Kurti Set", "Kurti", "Lehenga", "Saree", "Abaya", "Borkha", "Dress"
    )

    // Colors in Bangla & English
    val COLORS = listOf(
        "মেরুন", "পিস্তা", "লাল", "কালো", "নীল", "সবুজ", "হলুদ", "গোলাপী", "গোলাপি", "সাদা",
        "বেগুনি", "ল্যাভেন্ডার", "মাস্টার্ড", "মিষ্টি", "মেজেন্টা", "বাসন্তী", "আকাশী", "কমলা", "বাদামী",
        "কফি", "ছাই", "অ্যাশ", "অলিভ", "টিয়া", "রয়েল ব্লু", "নেভি ব্লু",
        "Maroon", "Red", "Black", "Blue", "Green", "Yellow", "Pink", "White", "Purple",
        "Lavender", "Mustard", "Pista", "Magenta", "Orange", "Navy Blue", "Royal Blue",
        "Bottle Green", "Sky Blue", "Olive", "Peach", "Coffee", "Chocolate", "Ash", "Gray", "Grey",
        "Off White", "Cream", "Beige", "Teal"
    )

    // Chest / Body sizes supported
    val STANDARD_BODY_SIZES = listOf(
        "34", "36", "38", "40", "42", "44", "46", "48", "50",
        "Free", "ফ্রি", "XS", "S", "M", "L", "XL", "XXL", "2XL", "3XL", "4XL"
    )

    // Field label aliases: Regex patterns to extract key-value on the same or next segment
    val NAME_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📍👤\t])\s*(?:নাম|গ্রাহকের\s*নাম|কাস্টমারের\s*নাম|name|customer\s*name|cust\s*name|client|apnar\s*nam|nam)\s*[:=\-–—~]?\s*([^\n\r,;|]+)"""
    )

    val PHONE_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📞📱\t])\s*(?:ফোন\s*(?:নাম্বার|নম্বর)?|মোবাইল\s*(?:নাম্বার|নম্বর)?|phone\s*(?:no|number)?|mobile\s*(?:no|number)?|cell|contact\s*(?:no|number)?|whatsapp|phn|ph|mob)\s*[:=\-–—~]?\s*([0-9\+\s\-\.–—]+)"""
    )

    val ADDRESS_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📍🏠\t])\s*(?:ঠিকানা|কাস্টমার\s*ঠিকানা|address|thikana|location|লোকেশন|delivery\s*address)\s*[:=\-–—~]?\s*([^\n\r|;]+)"""
    )

    val THANA_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📍🏠\t])\s*(?:থানা|উপজেলা|thana|upazila|p\.?s\.?)\s*[:=\-–—~]?\s*([^\n\r,;|]+)"""
    )

    val EMBEDDED_THANA_POSTFIX_REGEX = Regex(
        """(?i)(?:^|[^\w\u0980-\u09FF])([A-Za-z\u0980-\u09FF]+)\s+(?:থানা|উপজেলা|thana|upazila)(?:(?=[^\w\u0980-\u09FF])|$)"""
    )

    val EMBEDDED_THANA_PREFIX_REGEX = Regex(
        """(?i)(?:^|[^\w\u0980-\u09FF])(?:থানা|উপজেলা|thana|upazila)\s*[:=\-–—~]?\s*([A-Za-z\u0980-\u09FF]+)(?:(?=[^\w\u0980-\u09FF])|$)"""
    )

    val DRESS_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉👗\t])\s*(?:ড্রেস(?:\s*নাম)?|পোশাক|আইটেম|প্রোডাক্ট|dress(?:\s*name)?|item|product|design|model)\s*[:=\-–—~]?\s*([^\n\r,;|]+)"""
    )

    // Body size vs Length labels
    val BODY_SIZE_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📏\t])\s*(?:সাইজ|বডি(?:\s*সাইজ)?|বুকের\s*মাপ|size|body\s*size|sz|chest)\s*[:=\-–—~]?\s*([3-5][0-9]|Free|ফ্রি|XS|S|M|L|XL|XXL|2XL|3XL|4XL)(?:(?=[^\w])|$)"""
    )

    val LENGTH_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📏\t])\s*(?:লং|ঝুল|দৈর্ঘ্য|পায়জামা|pant(?:\s*length)?|long|length)\s*[:=\-–—~]?\s*([2-5][0-9])\b"""
    )

    val COLOR_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉🎨\t])\s*(?:কালার|রং|রঙ|color|colour|shade)\s*[:=\-–—~]?\s*([^\n\r,;|]+)"""
    )

    val QUANTITY_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📦\t])\s*(?:পরিমাণ|পিস|সংখ্যা|qty|quantity|pcs|piece)\s*[:=\-–—~]?\s*([0-9]+)"""
    )

    val ADVANCE_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉💳\t\s])(?:অগ্রিম(?:\s*টাকা)?|বিকাশ(?:\s*টাকা)?|নগদ(?:\s*টাকা)?|paid|advance|adv|bkash|bikash|nagad|deposit)\s*(?:disi|paid|diyechi|kora\s*hoise|korlam)?\s*[:=\-–—~]?\s*([0-9]+)"""
    )

    val DELIVERY_ADVANCE_REGEX = Regex(
        """(?i)(?:(?:ডেলিভারি\s*(?:চার্জ)?|delivery\s*(?:charge)?)\s*[:=\-–—~]?\s*([0-9]+)\s*(?:টাকা|tk)?\s*(?:অগ্রিম|advance|adv|paid|disi|দেওয়া\s*হয়েছে|দিয়েছি|kora\s*hoise)|(?:ডেলিভারি\s*(?:চার্জ)?|delivery\s*(?:charge)?)\s+(?:অগ্রিম|advance|adv|paid)\s*[:=\-–—~]?\s*([0-9]+))"""
    )

    val DELIVERY_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉🚚\t])\s*(?:ডেলিভারি(?:\s*চার্জ)?|কুরিয়ার(?:\s*চার্জ)?|delivery(?:\s*charge)?|shipping|courier|dc)\s*[:=\-–—~]?\s*([0-9]+)"""
    )

    val PRICE_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉💰\t])\s*(?:ড্রেসের\s*দাম|দাম|মূল্য|রেট|unit\s*price|price|dam|rate|cost)\s*[:=\-–—~]?\s*([0-9]+)"""
    )

    val TOTAL_BILL_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉🧾\t])\s*(?:মোট\s*(?:বিল|টাকা)?|টোটাল\s*(?:বিল)?|total(?:\s*bill|\s*amount)?|bill|net\s*amount)\s*[:=\-–—~]?\s*([0-9]+)"""
    )

    val DUE_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉💵\t\s])(?:বাকি(?:\s*টাকা)?|বকেয়া(?:\s*টাকা)?|due(?:\s*amount)?|baki(?:\s*taka)?|cod(?:\s*due)?)\s*[:=\-–—~]?\s*([0-9]+)"""
    )

    val NOTE_LABEL_REGEX = Regex(
        """(?i)(?:^|[\n\r•\*,|👉📝\t])\s*(?:নোট|বিশেষ\s*নির্দেশ|স্পেশাল\s*নোট|note|instructions?|special)\s*[:=\-–—~]?\s*([^\n\r]+)"""
    )

    val EQUATION_FULL_REGEX = Regex(
        """(\d+)\s*\+\s*(\d+)(?:\s*-\s*(\d+))?\s*=\s*(\d+)"""
    )

    val PHONE_REGEX = Regex(
        """(?:\+?880|880|0)?[\s\-\.–—]*(1[\s\-\.–—]*[3-9](?:[\s\-\.–—]*\d){8})\b"""
    )
}
