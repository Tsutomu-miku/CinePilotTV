package tv.cinepilot.tv.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup

class TvFlowLayout(context: Context) : ViewGroup(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec)
        val contentWidth = maxWidth - paddingLeft - paddingRight
        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = paddingTop + paddingBottom
        var measuredWidth = paddingLeft + paddingRight

        forEachChild { child ->
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, totalHeight)
            val params = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + params.leftMargin + params.rightMargin
            val childHeight = child.measuredHeight + params.topMargin + params.bottomMargin
            if (lineWidth > 0 && lineWidth + childWidth > contentWidth) {
                totalHeight += lineHeight
                measuredWidth = maxOf(measuredWidth, paddingLeft + paddingRight + lineWidth)
                lineWidth = 0
                lineHeight = 0
            }
            lineWidth += childWidth
            lineHeight = maxOf(lineHeight, childHeight)
        }

        totalHeight += lineHeight
        measuredWidth = maxOf(measuredWidth, paddingLeft + paddingRight + lineWidth)
        setMeasuredDimension(resolveSize(measuredWidth, widthMeasureSpec), resolveSize(totalHeight, heightMeasureSpec))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val contentRight = right - left - paddingRight
        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0

        forEachChild { child ->
            val params = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + params.leftMargin + params.rightMargin
            val childHeight = child.measuredHeight + params.topMargin + params.bottomMargin
            if (x > paddingLeft && x + childWidth > contentRight) {
                x = paddingLeft
                y += lineHeight
                lineHeight = 0
            }
            val childLeft = x + params.leftMargin
            val childTop = y + params.topMargin
            child.layout(childLeft, childTop, childLeft + child.measuredWidth, childTop + child.measuredHeight)
            x += childWidth
            lineHeight = maxOf(lineHeight, childHeight)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(attrs: android.util.AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateLayoutParams(params: LayoutParams?): LayoutParams {
        return MarginLayoutParams(params)
    }

    override fun checkLayoutParams(params: LayoutParams?): Boolean {
        return params is MarginLayoutParams
    }

    private fun forEachChild(block: (View) -> Unit) {
        for (index in 0 until childCount) {
            val child = getChildAt(index)
            if (child.visibility != View.GONE) {
                block(child)
            }
        }
    }
}
