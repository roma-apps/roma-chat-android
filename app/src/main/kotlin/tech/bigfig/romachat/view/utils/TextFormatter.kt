/* Copyright 2017 Andrew Dawson
 * Copyright (c) 2019 Vasily Kabunov
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>.
 */

package tech.bigfig.romachat.view.utils

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import tech.bigfig.romachat.data.entity.Status
import java.net.URI
import java.net.URISyntaxException

object TextFormatter {

    /**
     * Finds links, mentions, and hashtags in a piece of text and makes them clickable, associating
     * them with callbacks to notify when they're clicked.
     *
     * @param view the returned text will be put in
     * @param content containing text with mentions, links, or hashtags
     * @param mentions any '@' mentions which are known to be in the content
     * @param listener to notify about particular spans that are clicked
     */
    fun setClickableText(
        view: TextView, content: Spanned,
        mentions: Array<Status.Mention>?, listener: MessageClickListener?
    ) {
        val builder = SpannableStringBuilder(content)
        val urlSpans = content.getSpans(0, content.length, URLSpan::class.java)
        for (span in urlSpans) {
            val start = builder.getSpanStart(span)
            val end = builder.getSpanEnd(span)
            val flags = builder.getSpanFlags(span)
            val text = builder.subSequence(start, end)
            var customSpan: ClickableSpan? = null

            if (text[0] == '#') {
                val tag = text.subSequence(1, text.length).toString()
                customSpan = object : ClickableSpanNoUnderline() {
                    override fun onClick(widget: View) {
                        listener?.onTagClick(tag)
                    }
                }
            } else if (text[0] == '@' && mentions != null && mentions.isNotEmpty()) {
                val accountUsername = text.subSequence(1, text.length).toString()
                /* There may be multiple matches for users on different instances with the same
                 * username. If a match has the same domain we know it's for sure the same, but if
                 * that can't be found then just go with whichever one matched last. */
                var id: String? = null
                for (mention in mentions) {
                    if (mention.localUsername.equals(accountUsername, true)) {
                        id = mention.id
                        if (mention.url != null && mention.url.contains(getDomain(span.url))) {
                            break
                        }
                    }
                }
                if (id != null) {
                    val accountId = id
                    customSpan = object : ClickableSpanNoUnderline() {
                        override fun onClick(widget: View) {
                            listener?.onAccountClick(accountId)
                        }
                    }
                }
            }

            if (customSpan == null) {
                customSpan = object : CustomUrlSpan(span.url) {
                    override fun onClick(view: View) {
                        listener?.onUrlClick(url)
                    }
                }
            }
            builder.removeSpan(span)
            builder.setSpan(customSpan, start, end, flags)

            /* Add zero-width space after links in end of line to fix its too large hitbox.
             * See also : https://github.com/romaapp/Roma/issues/846
             *            https://github.com/romaapp/Roma/pull/916 */
            if (end >= builder.length || builder.subSequence(end, end + 1).toString() == "\n") {
                builder.insert(end, "\u200B")
            }
        }

        view.text = builder
        view.linksClickable = true
        view.movementMethod = LinkMovementMethod.getInstance()

        view.setOnClickListener { listener?.onClick() }
        view.setOnLongClickListener {
            listener?.onLongClick()
            true
        }
    }

    private fun getDomain(urlString: String): String {
        val uri: URI
        try {
            uri = URI(urlString)
        } catch (e: URISyntaxException) {
            return ""
        }

        val host = uri.host
        return if (host.startsWith("www.")) {
            host.substring(4)
        } else {
            host
        }
    }

    abstract class ClickableSpanNoUnderline : ClickableSpan() {
        override fun updateDrawState(ds: TextPaint) {
            super.updateDrawState(ds)
            ds.isUnderlineText = false
        }
    }
}