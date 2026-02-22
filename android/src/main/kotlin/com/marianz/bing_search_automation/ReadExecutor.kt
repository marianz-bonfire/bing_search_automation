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

    companion object {
        val ARTICLE_SEARCH_TERMS = listOf(
            "news",
            "latest headlines",
            "breaking news",
            "technology news",
            "sports news"
        )
    }

    fun performRead(searchTerm: String, onComplete: () -> Unit) {
        if (isReading) {
            Logger.d("Already reading, skipping")
            onComplete()
            return
        }

        Logger.d("Starting read session with term: $searchTerm")
        isReading = true
        currentOnComplete = onComplete
        articlesRead = 0

        // Step 1: Try to find and read articles
        attemptToReadArticles()
    }

    private fun attemptToReadArticles() {
        Logger.d("Attempting to read articles...")

        val root = service.rootInActiveWindow ?: run {
            Logger.d("No root node, waiting...")
            handler.postDelayed({
                attemptToReadArticles()
            }, 2000)
            return
        }

        // First, try to find clickable news items
        val newsItems = findClickableNewsItems(root)

        if (newsItems.isNotEmpty()) {
            Logger.d("Found ${newsItems.size} news items, clicking first one")
            clickAndReadNewsItem(newsItems.first())
        } else {
            // If no news items found, try to search
            searchForNews()
        }
    }

    private fun findClickableNewsItems(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val newsItems = mutableListOf<AccessibilityNodeInfo>()

        // Look for items that might be news articles
        fun traverse(node: AccessibilityNodeInfo?, depth: Int = 0) {
            if (node == null || depth > 15) return

            val text = node.text?.toString()?.lowercase() ?: ""

            // Check if this looks like a news item
            val isPotentialNews = (
                    node.isClickable &&
                            text.isNotBlank() &&
                            text.length > 15 && // Reasonable length for a headline
                            !text.contains("search", ignoreCase = true) &&
                            !text.contains("bing", ignoreCase = true) &&
                            (text.contains("news") ||
                                    text.contains("article") ||
                                    text.contains("story") ||
                                    text.contains("update") ||
                                    text.contains("headline"))
                    )

            if (isPotentialNews) {
                newsItems.add(node)
            }

            // Also look for items in common news containers
            if (node.className?.contains("RecyclerView") == true ||
                node.className?.contains("ListView") == true ||
                node.className?.contains("ScrollView") == true) {

                // This might be a feed, check its children
                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i), depth + 1)
                }
            } else {
                // Continue normal traversal
                for (i in 0 until node.childCount) {
                    traverse(node.getChild(i), depth + 1)
                }
            }
        }

        traverse(root)
        return newsItems.distinctBy { it.text?.toString() }.take(5) // Limit to 5 items
    }

    private fun clickAndReadNewsItem(item: AccessibilityNodeInfo) {
        Logger.d("Clicking news item: ${item.text?.take(50)}...")

        // Click the item
        val clicked = item.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        Logger.d("News item clicked: $clicked")

        // Wait for article to load
        handler.postDelayed({
            readCurrentArticle()
        }, 3000)
    }

    private fun readCurrentArticle() {
        Logger.d("Reading article ${articlesRead + 1}/$ARTICLES_TO_READ")

        // Simulate reading by waiting
        handler.postDelayed({
            // Try to scroll or interact with the article
            simulateArticleInteraction()

            handler.postDelayed({
                articlesRead++
                Logger.d("Finished reading article $articlesRead/$ARTICLES_TO_READ")

                if (articlesRead >= ARTICLES_TO_READ) {
                    completeReadingSession()
                } else {
                    // Go back and find next article
                    goBackAndFindNextArticle()
                }
            }, 7000) // Wait 7 seconds for "reading"
        }, 2000) // Wait 2 seconds for article to load
    }

    private fun simulateArticleInteraction() {
        val root = service.rootInActiveWindow ?: return

        // Try to find and click "Read more" or "Continue reading"
        val readMoreButtons = listOf(
            NodeFinder.findByText(root, "Read more"),
            NodeFinder.findByText(root, "Continue reading"),
            NodeFinder.findByText(root, "See more")
        )

        val readMoreButton = readMoreButtons.firstOrNull { it != null && it.isClickable }
        readMoreButton?.let {
            Logger.d("Found 'Read more' button, clicking")
            it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Try to scroll
        val scrollableView = findScrollableView(root)
        scrollableView?.let {
            if (it.isScrollable) {
                Logger.d("Found scrollable view, scrolling")
                it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            }
        }
    }

    private fun findScrollableView(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isScrollable) return node

        if (node.className?.contains("ScrollView") == true ||
            node.className?.contains("RecyclerView") == true ||
            node.className?.contains("ListView") == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val result = findScrollableView(node.getChild(i))
            if (result != null) return result
        }

        return null
    }

    private fun goBackAndFindNextArticle() {
        Logger.d("Going back to find next article...")

        // Go back
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

        // Wait for back to complete
        handler.postDelayed({
            attemptToReadArticles()
        }, 2000)
    }

    private fun searchForNews() {
        Logger.d("Searching for news...")

        val root = service.rootInActiveWindow ?: return
        val searchBox = findSearchBox(root)

        if (searchBox != null) {
            performNewsSearch(searchBox)
        } else {
            // If no search box, use simplified approach
            simplifiedReading()
        }
    }

    private fun findSearchBox(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return NodeFinder.findByText(root, "Search") ?:
        NodeFinder.findByText(root, "Search the web") ?:
        NodeFinder.findFirstEditText(root)
    }

    private fun performNewsSearch(searchBox: AccessibilityNodeInfo) {
        // Click search box
        if (searchBox.isClickable) {
            searchBox.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Wait, then enter search term
        handler.postDelayed({
            val root = service.rootInActiveWindow ?: return@postDelayed
            val editText = NodeFinder.findFirstEditText(root) ?: return@postDelayed

            // Use a simple search term
            val searchTerm = ARTICLE_SEARCH_TERMS.random()
            Logger.d("Searching for: $searchTerm")

            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, searchTerm)
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

            // Submit search
            handler.postDelayed({
                editText.performAction(EditorInfo.IME_ACTION_SEARCH)

                // Wait for results, then try to read
                handler.postDelayed({
                    attemptToReadArticles()
                }, 3000)
            }, 1000)
        }, 1000)
    }

    private fun simplifiedReading() {
        Logger.d("Using simplified reading approach")

        // Just simulate reading without UI interaction
        handler.postDelayed({
            articlesRead++
            Logger.d("Simulated reading article $articlesRead/$ARTICLES_TO_READ")

            if (articlesRead < ARTICLES_TO_READ) {
                handler.postDelayed({
                    articlesRead++
                    Logger.d("Simulated reading article $articlesRead/$ARTICLES_TO_READ")

                    if (articlesRead < ARTICLES_TO_READ) {
                        handler.postDelayed({
                            articlesRead++
                            Logger.d("Simulated reading article $articlesRead/$ARTICLES_TO_READ")
                            completeReadingSession()
                        }, 5000)
                    } else {
                        completeReadingSession()
                    }
                }, 5000)
            } else {
                completeReadingSession()
            }
        }, 5000)
    }

    private fun completeReadingSession() {
        Logger.d("Reading session completed successfully")
        isReading = false
        currentOnComplete?.invoke()
        currentOnComplete = null
        articlesRead = 0
    }

    fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        isReading = false
        currentOnComplete = null
    }
}