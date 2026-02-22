package com.marianz.bing_search_automation


import android.accessibilityservice.AccessibilityService
import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inputmethod.EditorInfo
import kotlin.random.Random


@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
class ReadExecutor(private val service: AccessibilityService) {
    private val handler = Handler(Looper.getMainLooper())
    private var articlesRead = 0
    private val ARTICLES_TO_READ = 3
    private var isReading = false
    private var currentOnComplete: (() -> Unit)? = null
    private val TAG = "ReadExecutor"

    // Article indicators to look for
    private val ARTICLE_INDICATORS = listOf(
        "news", "article", "story", "update", "headline",
        "breaking", "latest", "trending", "top stories"
    )

    fun performRead(onComplete: () -> Unit) {
        if (isReading) {
            Log.d(TAG, "Already reading, skipping")
            onComplete()
            return
        }

        Log.d(TAG, "Starting read session on home page")
        isReading = true
        currentOnComplete = onComplete
        articlesRead = 0

        // Start reading articles directly
        attemptToReadArticles()
    }

    private fun attemptToReadArticles() {
        Log.d(TAG, "Attempting to read articles, current count: $articlesRead/$ARTICLES_TO_READ")

        val root = service.rootInActiveWindow ?: run {
            Log.d(TAG, "No root node, waiting...")
            handler.postDelayed({
                attemptToReadArticles()
            }, 2000)
            return
        }

        root.refresh()
        Log.d(TAG, "Root refreshed")

        // Find articles on the current page
        val articles = findArticles(root)
        Log.d(TAG, "Found ${articles.size} potential articles")

        if (articles.isNotEmpty()) {
            // Filter out articles we might have already read
            val unreadArticles = articles
            Log.d(TAG, "Found ${unreadArticles.size} unread articles")

            if (unreadArticles.isNotEmpty()) {
                val nextArticle = unreadArticles.first()
                Log.d(TAG, "Selected article: '${nextArticle.text?.take(50)}...'")
                clickAndReadArticle(nextArticle)
            } else {
                Log.d(TAG, "No unread articles found")
                completeReadingSession()
            }
        } else {
            Log.d(TAG, "No articles found on page")
            completeReadingSession()
        }
    }

    private fun findArticles(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val articles = mutableListOf<AccessibilityNodeInfo>()
        val processedTexts = mutableSetOf<String>()

        fun traverse(node: AccessibilityNodeInfo?, depth: Int = 0) {
            if (node == null || depth > 20) return

            val text = node.text?.toString()?.trim() ?: ""
            val contentDesc = node.contentDescription?.toString()?.trim() ?: ""

            // Check if this looks like an article
            val isArticle = (
                    node.isClickable &&
                            text.isNotBlank() &&
                            text.length in 20..200 && // Article headlines typically this length
                            !text.contains("search", ignoreCase = true) &&
                            !text.contains("bing", ignoreCase = true) &&
                            !text.contains("setting", ignoreCase = true) &&
                            !text.contains("menu", ignoreCase = true) &&
                            (ARTICLE_INDICATORS.any { text.contains(it, ignoreCase = true) } ||
                                    ARTICLE_INDICATORS.any { contentDesc.contains(it, ignoreCase = true) } ||
                                    text.contains("?", ignoreCase = true) || // Headlines often have questions
                                    text.matches(Regex(".*[.!?]$"))) // Ends with punctuation
                    )

            if (isArticle && !processedTexts.contains(text)) {
                Log.d(TAG, "Found potential article: '$text'")
                processedTexts.add(text)
                articles.add(node)
            }

            // Continue traversal
            for (i in 0 until node.childCount) {
                traverse(node.getChild(i), depth + 1)
            }
        }

        traverse(root)

        // Return unique articles, limit to 10 max
        return articles.distinctBy { it.text?.toString() }.take(10)
    }

    private fun clickAndReadArticle(article: AccessibilityNodeInfo) {
        Log.d(TAG, "Clicking article ${articlesRead + 1}/$ARTICLES_TO_READ: '${article.text?.take(50)}...'")

        // Find clickable parent if needed
        val clickableTarget = if (article.isClickable) article
        else NodeFinder.findClickableParent(article) ?: article

        val clickSuccess = clickableTarget.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Log.d(TAG, "Article clicked: $clickSuccess")

        // Wait for article to load
        handler.postDelayed({
            readCurrentArticle()
        }, 3000)
    }

    private fun readCurrentArticle() {
        Log.d(TAG, "Reading article ${articlesRead + 1}/$ARTICLES_TO_READ")

        // Simulate reading by scrolling through the article
        simulateReading()

        handler.postDelayed({
            articlesRead++
            Log.d(TAG, "Finished reading article $articlesRead/$ARTICLES_TO_READ")

            if (articlesRead >= ARTICLES_TO_READ) {
                Log.d(TAG, "Completed all $ARTICLES_TO_READ articles")
                completeReadingSession()
            } else {
                Log.d(TAG, "Moving to next article")
                goBackAndFindNextArticle()
            }
        }, 8000) // Wait 8 seconds for "reading"
    }

    private fun simulateReading() {
        val root = service.rootInActiveWindow ?: return

        // Try to scroll through the article
        val scrollableViews = findScrollableViews(root)

        if (scrollableViews.isNotEmpty()) {
            Log.d(TAG, "Found ${scrollableViews.size} scrollable views, simulating reading")

            // Perform multiple scrolls to simulate reading
            handler.postDelayed({
                scrollableViews.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Log.d(TAG, "First scroll")

                handler.postDelayed({
                    scrollableViews.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    Log.d(TAG, "Second scroll")

                    handler.postDelayed({
                        scrollableViews.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        Log.d(TAG, "Third scroll")
                    }, 2000)
                }, 2000)
            }, 2000)
        } else {
            Log.d(TAG, "No scrollable views found")
        }

        // Look for "Read more" or similar buttons
        val readMoreTexts = listOf("Read more", "Continue reading", "See more", "Full story", "Read full article")
        for (text in readMoreTexts) {
            val button = NodeFinder.findByText(root, text)
            if (button != null && button.isClickable) {
                Log.d(TAG, "Found '$text' button, clicking")
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                break
            }
        }
    }

    private fun findScrollableViews(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val scrollables = mutableListOf<AccessibilityNodeInfo>()

        fun traverse(currentNode: AccessibilityNodeInfo?) {
            if (currentNode == null) return

            if (currentNode.isScrollable ||
                currentNode.className?.contains("ScrollView") == true ||
                currentNode.className?.contains("RecyclerView") == true ||
                currentNode.className?.contains("ListView") == true ||
                currentNode.className?.contains("WebView") == true) {
                scrollables.add(currentNode)
            }

            for (i in 0 until currentNode.childCount) {
                traverse(currentNode.getChild(i))
            }
        }

        traverse(node)
        return scrollables
    }

    private fun goBackAndFindNextArticle() {
        Log.d(TAG, "Going back to home page to find next article...")

        // Perform back action
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        Log.d(TAG, "Back action performed")

        // Wait for back to complete and page to load
        handler.postDelayed({
            // Sometimes need to go back twice to reach home
            handler.postDelayed({
                Log.d(TAG, "Looking for next article")
                attemptToReadArticles()
            }, 2000)
        }, 2000)
    }

    private fun completeReadingSession() {
        Log.d(TAG, "Reading session completed successfully")
        Log.d(TAG, "Total articles read: $articlesRead")

        isReading = false
        currentOnComplete?.invoke()
        currentOnComplete = null
        articlesRead = 0

        // Ensure we're back on home page
        handler.postDelayed({
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        }, 1000)
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up ReadExecutor")
        handler.removeCallbacksAndMessages(null)
        isReading = false
        currentOnComplete = null
        articlesRead = 0
    }
}