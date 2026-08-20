package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AutonomousTask {
    data class SendSmsAndClose(
        val recipient: String,
        val message: String,
        val autoReturnHome: Boolean = true,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()

    data class SendWhatsAppAndClose(
        val recipient: String,
        val message: String,
        val autoReturnHome: Boolean = true,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()

    data class SendMessengerMessage(
        val recipient: String,
        val message: String,
        val autoReturnHome: Boolean = false,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()

    data class PublishFacebookPost(
        val postContent: String,
        val autoReturnHome: Boolean = false,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()

    data class CommentOnScreen(
        val commentText: String,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()

    data class ClickTarget(
        val targetKeywords: List<String>,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()

    data class TypeText(
        val text: String,
        val onFeedback: (String) -> Unit
    ) : AutonomousTask()
}

class AuraAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val handler = Handler(Looper.getMainLooper())

    private var activeTask: AutonomousTask? = null
    private var taskStep: Int = 0

    companion object {
        private const val TAG = "AuraAccessibility"

        @Volatile
        var instance: AuraAccessibilityService? = null
            private set

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected = _isServiceConnected.asStateFlow()

        fun isAccessibilityEnabled(context: Context): Boolean {
            val expectedServiceName = "${context.packageName}/${AuraAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServices)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                    return true
                }
            }
            return false
        }

        fun openAccessibilitySettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // fallback
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.d(TAG, "Aura Accessibility Service Connected and Ready for Autonomous Operations")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        _isServiceConnected.value = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceConnected.value = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val task = activeTask ?: return

        when (task) {
            is AutonomousTask.SendSmsAndClose -> {
                handleAutonomousSmsEvent(event, task)
            }
            is AutonomousTask.SendWhatsAppAndClose -> {
                handleAutonomousWhatsAppEvent(event, task)
            }
            is AutonomousTask.SendMessengerMessage -> {
                handleAutonomousMessengerEvent(event, task)
            }
            is AutonomousTask.PublishFacebookPost -> {
                handleAutonomousFacebookEvent(event, task)
            }
            is AutonomousTask.CommentOnScreen -> {
                handleAutonomousCommentEvent(event, task)
            }
            is AutonomousTask.ClickTarget -> {
                handleClickTargetEvent(event, task)
            }
            is AutonomousTask.TypeText -> {
                handleTypeTextEvent(event, task)
            }
        }
    }

    override fun onInterrupt() {
        activeTask = null
        taskStep = 0
    }

    // =========================================================================
    // Autonomous Execution Dispatches
    // =========================================================================

    fun dispatchAutonomousSms(recipient: String, message: String, autoReturnHome: Boolean = true, onFeedback: (String) -> Unit) {
        activeTask = AutonomousTask.SendSmsAndClose(recipient, message, autoReturnHome, onFeedback)
        taskStep = 0

        // Launch SMS Intent
        try {
            val sendIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = if (recipient.isNotBlank()) Uri.parse("smsto:${Uri.encode(recipient)}") else Uri.parse("smsto:")
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(sendIntent)
            onFeedback("جاري الانتقال لتطبيق الرسائل وتعبئة النص وإرساله تلقائياً... ✉️")
        } catch (e: Exception) {
            onFeedback("تعذر فتح تطبيق الرسائل")
        }

        // Schedule periodic scanning fallback in case window state event already fired
        serviceScope.launch {
            for (attempt in 1..8) {
                delay(400)
                if (activeTask is AutonomousTask.SendSmsAndClose) {
                    val root = rootInActiveWindow
                    if (root != null) {
                        tryProcessSmsWindow(root, activeTask as AutonomousTask.SendSmsAndClose)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun handleAutonomousSmsEvent(event: AccessibilityEvent, task: AutonomousTask.SendSmsAndClose) {
        val root = rootInActiveWindow ?: return
        tryProcessSmsWindow(root, task)
    }

    private fun tryProcessSmsWindow(root: AccessibilityNodeInfo, task: AutonomousTask.SendSmsAndClose) {
        // Step 0/1: Find text box and ensure message is entered
        if (taskStep == 0) {
            val editNode = findFirstEditableNode(root)
            if (editNode != null) {
                val currentText = editNode.text?.toString() ?: ""
                if (!currentText.contains(task.message)) {
                    val args = Bundle().apply {
                        putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.message)
                    }
                    editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                }
                taskStep = 1
            }
        }

        // Step 1: Find send button and click it
        if (taskStep == 1 || taskStep == 0) {
            val sendKeywords = listOf("إرسال", "ارسال", "Send", "Send SMS", "SMS", "send_message", "btn_send", "send_button", "send_sms")
            val sendButton = findNodeByKeywords(root, sendKeywords)

            if (sendButton != null) {
                val clicked = performSafeClick(sendButton)
                if (clicked) {
                    taskStep = 2
                    task.onFeedback("تم كتابة الرسالة \"${task.message}\" والضغط على زر الإرسال بنجاح ✅")

                    if (task.autoReturnHome) {
                        handler.postDelayed({
                            performGlobalAction(GLOBAL_ACTION_HOME)
                            task.onFeedback("تم إرسال الرسالة وإغلاق التطبيق والعودة للشاشة الرئيسية 🏠")
                            activeTask = null
                            taskStep = 0
                        }, 700)
                    } else {
                        activeTask = null
                        taskStep = 0
                    }
                }
            }
        }
    }

    fun dispatchAutonomousWhatsApp(recipient: String, message: String, autoReturnHome: Boolean = true, onFeedback: (String) -> Unit) {
        activeTask = AutonomousTask.SendWhatsAppAndClose(recipient, message, autoReturnHome, onFeedback)
        taskStep = 0

        try {
            val cleanPhone = recipient.replace(Regex("[^0-9+]"), "")
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }
            val waIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                `package` = "com.whatsapp"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(waIntent)
            onFeedback("جاري فتح واتساب وإرسال الرسالة تلقائياً... 💬")
        } catch (e: Exception) {
            onFeedback("تطبيق واتساب غير مثبت أو تعذر إطلاقه")
        }

        serviceScope.launch {
            for (attempt in 1..10) {
                delay(450)
                if (activeTask is AutonomousTask.SendWhatsAppAndClose) {
                    val root = rootInActiveWindow
                    if (root != null) {
                        tryProcessWhatsAppWindow(root, activeTask as AutonomousTask.SendWhatsAppAndClose)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun handleAutonomousWhatsAppEvent(event: AccessibilityEvent, task: AutonomousTask.SendWhatsAppAndClose) {
        val root = rootInActiveWindow ?: return
        tryProcessWhatsAppWindow(root, task)
    }

    private fun tryProcessWhatsAppWindow(root: AccessibilityNodeInfo, task: AutonomousTask.SendWhatsAppAndClose) {
        // Step 0: Ensure message is typed if not preloaded
        val editNode = findFirstEditableNode(root)
        if (editNode != null && task.message.isNotBlank()) {
            val currentText = editNode.text?.toString() ?: ""
            if (currentText.isBlank()) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.message)
                }
                editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }

        val sendKeywords = listOf("Send", "إرسال", "ارسال", "send_btn", "send")
        val sendButton = findNodeByKeywords(root, sendKeywords) ?: findNodeById(root, "com.whatsapp:id/send")

        if (sendButton != null) {
            val clicked = performSafeClick(sendButton)
            if (clicked) {
                task.onFeedback("تم إرسال رسالة الواتساب: \"${task.message}\" بنجاح 💬")
                if (task.autoReturnHome) {
                    handler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        task.onFeedback("تم إرسال الواتساب والعودة للشاشة الرئيسية 🏠")
                        activeTask = null
                        taskStep = 0
                    }, 800)
                } else {
                    activeTask = null
                    taskStep = 0
                }
            }
        }
    }

    /**
     * Autonomous Facebook Post Creator & Publisher:
     * Types dictated text into Facebook post composer and automatically clicks "Post" / "نشر".
     */
    fun dispatchAutonomousFacebookPost(postContent: String, autoReturnHome: Boolean = false, onFeedback: (String) -> Unit) {
        activeTask = AutonomousTask.PublishFacebookPost(postContent, autoReturnHome, onFeedback)
        taskStep = 0

        // Launch Facebook with Share Intent or Main App
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, postContent)
                `package` = "com.facebook.katana"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(shareIntent)
            onFeedback("جاري فتح فيسبوك وكتابة المنشور ونشره تلقائياً... 📘")
        } catch (e: Exception) {
            // Fallback to Facebook Lite or standard launcher
            try {
                val liteIntent = packageManager.getLaunchIntentForPackage("com.facebook.lite")
                    ?: packageManager.getLaunchIntentForPackage("com.facebook.katana")
                if (liteIntent != null) {
                    liteIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(liteIntent)
                    onFeedback("جاري كتابة المنشور على فيسبوك... 📘")
                } else {
                    onFeedback("تطبيق فيسبوك غير مثبت على الهاتف")
                    activeTask = null
                    return
                }
            } catch (ex: Exception) {
                onFeedback("تعذر إطلاق فيسبوك")
                activeTask = null
                return
            }
        }

        serviceScope.launch {
            for (attempt in 1..12) {
                delay(500)
                if (activeTask is AutonomousTask.PublishFacebookPost) {
                    val root = rootInActiveWindow
                    if (root != null) {
                        tryProcessFacebookWindow(root, activeTask as AutonomousTask.PublishFacebookPost)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun handleAutonomousFacebookEvent(event: AccessibilityEvent, task: AutonomousTask.PublishFacebookPost) {
        val root = rootInActiveWindow ?: return
        tryProcessFacebookWindow(root, task)
    }

    private fun tryProcessFacebookWindow(root: AccessibilityNodeInfo, task: AutonomousTask.PublishFacebookPost) {
        // Step 0: Find "What's on your mind?" / "بم تفكر؟" if on main feed
        if (taskStep == 0) {
            val feedPostKeywords = listOf("What's on your mind", "بم تفكر", "بما تفكر", "إنشاء منشور", "انشاء منشور", "Create post", "Write something")
            val promptNode = findNodeByKeywords(root, feedPostKeywords)
            if (promptNode != null) {
                performSafeClick(promptNode)
                taskStep = 1
                return
            }
        }

        // Step 1: Type post text in the active composer
        val editNode = findFirstEditableNode(root)
        if (editNode != null) {
            val currentText = editNode.text?.toString() ?: ""
            if (!currentText.contains(task.postContent)) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.postContent)
                }
                editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            taskStep = 2
        }

        // Step 2: Find Post / Publish button and click it
        if (taskStep >= 1) {
            val postKeywords = listOf("نشر", "Post", "POST", "مشاركة", "Share", "Publish", "إنشاء", "انشاء")
            val postButton = findNodeByKeywords(root, postKeywords)
            if (postButton != null && postButton.isEnabled) {
                val clicked = performSafeClick(postButton)
                if (clicked) {
                    task.onFeedback("تم نشر البوست على فيسبوك بنجاح: \"${task.postContent}\" 📘✅")
                    if (task.autoReturnHome) {
                        handler.postDelayed({
                            performGlobalAction(GLOBAL_ACTION_HOME)
                        }, 1000)
                    }
                    activeTask = null
                    taskStep = 0
                }
            }
        }
    }

    /**
     * Autonomous Screen Commenting:
     * Detects comment field on whatever topic/post the user is currently viewing on screen,
     * types the dictated comment, and submits it immediately.
     */
    fun dispatchAutonomousCommentOnScreen(commentText: String, onFeedback: (String) -> Unit) {
        activeTask = AutonomousTask.CommentOnScreen(commentText, onFeedback)
        taskStep = 0

        val root = rootInActiveWindow
        if (root != null) {
            tryProcessCommentWindow(root, activeTask as AutonomousTask.CommentOnScreen)
        }

        serviceScope.launch {
            for (attempt in 1..8) {
                delay(400)
                if (activeTask is AutonomousTask.CommentOnScreen) {
                    val r = rootInActiveWindow
                    if (r != null) {
                        tryProcessCommentWindow(r, activeTask as AutonomousTask.CommentOnScreen)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun handleAutonomousCommentEvent(event: AccessibilityEvent, task: AutonomousTask.CommentOnScreen) {
        val root = rootInActiveWindow ?: return
        tryProcessCommentWindow(root, task)
    }

    private fun tryProcessCommentWindow(root: AccessibilityNodeInfo, task: AutonomousTask.CommentOnScreen) {
        // Step 0: Click on comment trigger or find editable box
        val commentKeywords = listOf("اكتب تعليقاً", "اكتب تعليقا", "Write a comment", "تعليق", "Comment", "Add a comment", "أضف تعليقاً", "اضف تعليق")
        val commentBox = findNodeByKeywords(root, commentKeywords) ?: findFirstEditableNode(root)

        if (commentBox != null) {
            if (commentBox.isEditable || commentBox.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.commentText)
                }
                commentBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                taskStep = 1
            } else {
                performSafeClick(commentBox)
                taskStep = 1
                return
            }
        }

        // Step 1: Submit/Send the comment
        if (taskStep >= 1) {
            val submitKeywords = listOf("نشر", "إرسال", "ارسال", "Post", "Send", "submit_comment", "send_comment")
            val submitButton = findNodeByKeywords(root, submitKeywords)
            if (submitButton != null) {
                val clicked = performSafeClick(submitButton)
                if (clicked) {
                    task.onFeedback("تم إرسال التعليق على الموضوع المعروض: \"${task.commentText}\" 💬✅")
                    activeTask = null
                    taskStep = 0
                }
            } else {
                // If no discrete send button, comment was typed into active field
                task.onFeedback("تمت كتابة التعليق في الحقل: \"${task.commentText}\" ✍️")
                activeTask = null
                taskStep = 0
            }
        }
    }

    /**
     * Autonomous Facebook Messenger Direct Message Sender:
     * Opens chat with contact or creates message, inputs dictated text, and sends.
     */
    fun dispatchAutonomousMessenger(recipient: String, message: String, autoReturnHome: Boolean = false, onFeedback: (String) -> Unit) {
        activeTask = AutonomousTask.SendMessengerMessage(recipient, message, autoReturnHome, onFeedback)
        taskStep = 0

        try {
            val messengerIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
                `package` = "com.facebook.orca"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(messengerIntent)
            onFeedback("جاري فتح الماسنجر وإرسال الرسالة إلى ($recipient)... 💬")
        } catch (e: Exception) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage("com.facebook.orca")
                    ?: packageManager.getLaunchIntentForPackage("com.facebook.mlite")
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(launchIntent)
                    onFeedback("جاري كتابة وإرسال الرسالة عبر الماسنجر... 💬")
                } else {
                    onFeedback("تطبيق الماسنجر (Messenger) غير مثبت على الهاتف")
                    activeTask = null
                    return
                }
            } catch (ex: Exception) {
                onFeedback("تعذر إطلاق الماسنجر")
                activeTask = null
                return
            }
        }

        serviceScope.launch {
            for (attempt in 1..10) {
                delay(450)
                if (activeTask is AutonomousTask.SendMessengerMessage) {
                    val root = rootInActiveWindow
                    if (root != null) {
                        tryProcessMessengerWindow(root, activeTask as AutonomousTask.SendMessengerMessage)
                    }
                } else {
                    break
                }
            }
        }
    }

    private fun handleAutonomousMessengerEvent(event: AccessibilityEvent, task: AutonomousTask.SendMessengerMessage) {
        val root = rootInActiveWindow ?: return
        tryProcessMessengerWindow(root, task)
    }

    private fun tryProcessMessengerWindow(root: AccessibilityNodeInfo, task: AutonomousTask.SendMessengerMessage) {
        // Step 0: Search contact if name given and on contacts screen
        if (taskStep == 0 && task.recipient.isNotBlank()) {
            val searchBox = findNodeByKeywords(root, listOf("Search", "بحث", "إلى", "To", "search_box"))
            if (searchBox != null) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.recipient)
                }
                searchBox.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                taskStep = 1
                return
            }
        }

        // Step 1: Type message text in chat composer
        val editNode = findFirstEditableNode(root)
        if (editNode != null) {
            val currentText = editNode.text?.toString() ?: ""
            if (!currentText.contains(task.message)) {
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.message)
                }
                editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
            taskStep = 2
        }

        // Step 2: Click Send in Messenger
        val sendKeywords = listOf("Send", "إرسال", "ارسال", "send_button", "send")
        val sendButton = findNodeByKeywords(root, sendKeywords)
        if (sendButton != null) {
            val clicked = performSafeClick(sendButton)
            if (clicked) {
                task.onFeedback("تم إرسال رسالة الماسنجر إلى (${task.recipient}): \"${task.message}\" 💬✅")
                if (task.autoReturnHome) {
                    handler.postDelayed({
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }, 800)
                }
                activeTask = null
                taskStep = 0
            }
        }
    }

    private fun handleClickTargetEvent(event: AccessibilityEvent, task: AutonomousTask.ClickTarget) {
        val root = rootInActiveWindow ?: return
        val node = findNodeByKeywords(root, task.targetKeywords)
        if (node != null) {
            val clicked = performSafeClick(node)
            if (clicked) {
                task.onFeedback("تم النقر على العنصر المطلوب بنجاح 🎯")
                activeTask = null
            }
        }
    }

    private fun handleTypeTextEvent(event: AccessibilityEvent, task: AutonomousTask.TypeText) {
        val root = rootInActiveWindow ?: return
        val editNode = findFirstEditableNode(root)
        if (editNode != null) {
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, task.text)
            }
            editNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            task.onFeedback("تمت كتابة النص المطلوب في الحقل ✍️")
            activeTask = null
        }
    }

    // =========================================================================
    // Universal Hands-Free Navigation & Phone Gestures
    // =========================================================================

    fun navigateHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    fun navigateBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun openRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun openQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }

    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    fun scrollDown(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findFirstScrollableNode(root)
            if (scrollable != null && scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                return true
            }
        }
        // Fallback to swipe gesture down
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metrics = resources.displayMetrics
            val centerX = metrics.widthPixels / 2f
            val startY = metrics.heightPixels * 0.75f
            val endY = metrics.heightPixels * 0.25f
            return performSwipeGesture(centerX, startY, centerX, endY)
        }
        return false
    }

    fun scrollUp(): Boolean {
        val root = rootInActiveWindow
        if (root != null) {
            val scrollable = findFirstScrollableNode(root)
            if (scrollable != null && scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)) {
                return true
            }
        }
        // Fallback to swipe gesture up
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val metrics = resources.displayMetrics
            val centerX = metrics.widthPixels / 2f
            val startY = metrics.heightPixels * 0.25f
            val endY = metrics.heightPixels * 0.75f
            return performSwipeGesture(centerX, startY, centerX, endY)
        }
        return false
    }

    private fun performSwipeGesture(startX: Float, startY: Float, endX: Float, endY: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 250)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findFirstScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstScrollableNode(child)
            if (found != null) return found
        }
        return null
    }

    fun dispatchCloseApp(appName: String?, onFeedback: (String) -> Unit) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
            onFeedback("تم إغلاق التطبيق والخروج للشاشة الرئيسية 🏠")
        }, 300)
    }

    fun performClickOnNode(keywords: List<String>, onFeedback: (String) -> Unit) {
        val root = rootInActiveWindow
        if (root != null) {
            val node = findNodeByKeywords(root, keywords)
            if (node != null && performSafeClick(node)) {
                onFeedback("تم النقر المباشر على العنصر 🎯")
                return
            }
        }
        activeTask = AutonomousTask.ClickTarget(keywords, onFeedback)
    }

    private fun performSafeClick(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }

        // If not clickable directly, simulate a touch gesture at the node's bounds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            if (rect.centerX() > 0 && rect.centerY() > 0) {
                return performTouchGesture(rect.centerX().toFloat(), rect.centerY().toFloat())
            }
        }
        return false
    }

    private fun performTouchGesture(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable || node.className?.toString()?.contains("EditText", ignoreCase = true) == true) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableNode(child)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeByKeywords(node: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val viewId = node.viewIdResourceName?.toString() ?: ""

        for (kw in keywords) {
            if (text.contains(kw, ignoreCase = true) ||
                contentDesc.contains(kw, ignoreCase = true) ||
                viewId.contains(kw, ignoreCase = true)
            ) {
                return node
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByKeywords(child, keywords)
            if (found != null) return found
        }
        return null
    }

    private fun findNodeById(node: AccessibilityNodeInfo, targetId: String): AccessibilityNodeInfo? {
        val viewId = node.viewIdResourceName?.toString() ?: ""
        if (viewId.equals(targetId, ignoreCase = true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeById(child, targetId)
            if (found != null) return found
        }
        return null
    }
}
