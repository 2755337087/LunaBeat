@file:Suppress("unused")

package com.example.LyricBox.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.util.Base64
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max
import kotlin.math.roundToInt

class SuperIslandGlowProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class ShaderMode {
        HIGH_END,
        MIDDLE
    }

    var shaderMode: ShaderMode = ShaderMode.HIGH_END
        set(value) {
            if (field != value) {
                field = value
                configureShader()
                invalidate()
            }
        }

    var progressFraction: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var headGlowAlpha: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    /**
     * Controls how much the head texture is scaled in shader sampling.
     * Lower values reduce over-stretch blur; higher values keep a softer glow.
     */
    var headScale: Float = 2.35f
        set(value) {
            field = value.coerceAtLeast(1f)
            invalidate()
        }

    /**
     * Optional post-blur for the whole progress bar as a fallback style.
     * Keep 0 to disable.
     */
    var postBlurRadiusPx: Float = 1f
        //样式2进度条贴图模糊
        set(value) {
            field = value.coerceAtLeast(0f)
            applyPostBlurIfNeeded()
            invalidate()
        }

    var trackHeightPx: Float = dp(6f)
        set(value) {
            field = max(1f, value)
            invalidate()
        }

    var trackHorizontalPaddingPx: Float = dp(12f)
        set(value) {
            field = max(0f, value)
            invalidate()
        }

    var trackColor: Int = Color.argb(51, 255, 255, 255)
        set(value) {
            field = value
            invalidate()
        }

    var fallbackProgressColor: Int = Color.argb(153, 255, 255, 255)
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Keep shader/fallback colors in sync for theme adaptation.
     */
    fun setStyleColors(trackColor: Int, progressColor: Int) {
        this.trackColor = trackColor
        this.fallbackProgressColor = progressColor
    }

    private val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fallbackPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var runtimeShader: RuntimeShader? = null
    private var headBitmapShader: BitmapShader? = null
    private var headWidth = 75f
    private var headHeight = 38f
    private var glowAnimator: ValueAnimator? = null

    init {
        configureShader()
    }

    fun setProgress(progress: Int, max: Int) {
        progressFraction = if (max <= 0) 0f else progress.toFloat() / max.toFloat()
    }

    fun animateHeadGlow(targetAlpha: Float, durationMillis: Long = 220L) {
        glowAnimator?.cancel()
        glowAnimator = ValueAnimator.ofFloat(headGlowAlpha, targetAlpha.coerceIn(0f, 1f)).apply {
            duration = durationMillis
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                headGlowAlpha = animator.animatedValue as Float
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = dp(272f).roundToInt()
        val desiredHeight = dp(38f).roundToInt()
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val widthPx = width.toFloat()
        val heightPx = height.toFloat()
        if (widthPx <= 0f || heightPx <= 0f) return

        val shader = runtimeShader
        if (shader == null) {
            drawFallbackProgress(canvas, widthPx, heightPx)
            return
        }

        if (shaderMode == ShaderMode.MIDDLE) {
            drawFallbackProgress(canvas, widthPx, heightPx)
        }

        val trackHeight = trackHeightPx.coerceAtMost(heightPx)
        val trackWidth = max(0f, widthPx - trackHorizontalPaddingPx * 2f)
        val trackPosition = floatArrayOf(trackHorizontalPaddingPx, heightPx / 2f)
        val trackSize = floatArrayOf(trackWidth, trackHeight)
        val canvasSize = floatArrayOf(widthPx, heightPx)
        val headSize = floatArrayOf(headWidth, headHeight)

        shader.setFloatUniform("uResolution", canvasSize)
        shader.setFloatUniform("uTrackCanvasSize", canvasSize)
        shader.setFloatUniform("uTrackPosition", trackPosition)
        shader.setFloatUniform("uTrackSize", trackSize)
        shader.setFloatUniform("uHeadSize", headSize)
        shader.setFloatUniform("uTrackProgress", progressFraction)
        shader.setFloatUniform("uHeadGlowAlpha", headGlowAlpha)
        shader.setFloatUniform("uHeadScale", headScale)
        shader.setFloatUniform(
            "uTrackTint",
            floatArrayOf(
                Color.red(trackColor) / 255f,
                Color.green(trackColor) / 255f,
                Color.blue(trackColor) / 255f
            )
        )
        shader.setFloatUniform("uTrackAlpha", Color.alpha(trackColor) / 255f)
        shader.setFloatUniform(
            "uProgressTint",
            floatArrayOf(
                Color.red(fallbackProgressColor) / 255f,
                Color.green(fallbackProgressColor) / 255f,
                Color.blue(fallbackProgressColor) / 255f
            )
        )
        shader.setIntUniform("uIsRtl", if (layoutDirection == LAYOUT_DIRECTION_RTL) 1 else 0)

        canvas.drawRect(0f, 0f, widthPx, heightPx, shaderPaint)
    }

    override fun onDetachedFromWindow() {
        glowAnimator?.cancel()
        glowAnimator = null
        super.onDetachedFromWindow()
    }

    private fun configureShader() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            runtimeShader = null
            shaderPaint.shader = null
            return
        }

        val shaderText = when (shaderMode) {
            ShaderMode.HIGH_END -> HIGH_END_SHADER
            ShaderMode.MIDDLE -> MIDDLE_SHADER
        }
        val shader = RuntimeShader(shaderText)
        val inputShader = headBitmapShader ?: createHeadBitmapShader().also { headBitmapShader = it }
        shader.setInputShader("uTex", inputShader)
        runtimeShader = shader
        shaderPaint.shader = shader
        applyPostBlurIfNeeded()
    }

    private fun applyPostBlurIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (postBlurRadiusPx > 0f) {
            setRenderEffect(
                RenderEffect.createBlurEffect(
                    postBlurRadiusPx,
                    postBlurRadiusPx,
                    Shader.TileMode.CLAMP
                )
            )
        } else {
            setRenderEffect(null)
        }
    }

    private fun createHeadBitmapShader(): BitmapShader {
        val bytes = Base64.decode(HEAD_PNG_BASE64, Base64.DEFAULT)
        val bitmap = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
        headWidth = bitmap.width.toFloat()
        headHeight = bitmap.height.toFloat()
        return BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }

    private fun drawFallbackProgress(canvas: Canvas, widthPx: Float, heightPx: Float) {
        val trackHeight = trackHeightPx.coerceAtMost(heightPx)
        val left = trackHorizontalPaddingPx
        val right = max(left, widthPx - trackHorizontalPaddingPx)
        val top = (heightPx - trackHeight) / 2f
        val bottom = top + trackHeight
        val radius = trackHeight / 2f

        fallbackPaint.color = trackColor
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, fallbackPaint)

        val progressWidth = (right - left) * progressFraction
        if (progressWidth <= 0f) return

        fallbackPaint.color = fallbackProgressColor
        if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            canvas.drawRoundRect(right - progressWidth, top, right, bottom, radius, radius, fallbackPaint)
        } else {
            canvas.drawRoundRect(left, top, left + progressWidth, bottom, radius, radius, fallbackPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    companion object {
        private const val HEAD_PNG_BASE64 = "iVBORw0KGgoAAAANSUhEUgAAAEsAAAAmCAQAAAAmLa7lAAAD9UlEQVRYw+2YvY7dNhBGz1CU7rpNkyJd3v8l7JcIkARxkSBO5WKdhe31ipOCI2r4p12nCBDAurgSRVHi0TfD4VDwbfuPNpW+XB9VfI2Kv+Nqk38PcvFMrY59A81PEv0KLBXRy+4F0HJn7jzXjV9TG0A9kWdg8gIlZPoaHm3eSu1/nKmHHYPJBKo/azUSe7BUeDQq4vZaALW6NkCLF/qcGkj51wAygJHOkKmYMaEFCQ82BFAZIM330p0diNJA1jplqFRqnGatXnGoEhcI/b8vhWLcZEDZARKJUOmlY72i02qmTVv2GL5WuutKAJREAhJiaI0Bh75VhYORMnNNapizlNU6/UkIBhYA2MtzFAG9MKJMwKQpn2gey/9CFd8CyUwpCLtBK8mekUZmjJXD9+NMOnPVMBSUFu70noSS2FGUgAKB3bS0ENzqFQfu3YLkt/JqBDsPpRxYSimwNIgZ7JEPPPCZZEYV03Lg9lG9DocaR2dL6Th3u1jNSrSajZWFhciNyMLCSuSOaPdEghlTeeB3fuJPPpVxKsX9OyOK63KxR8XSxVbOVzY7rtxYWdiI3NjK8Y7VSq9YWVkJBKKNQeUjv/Ga1/xhY1K7GaEy4kJkJbKxWrcrWzluhpQBcruboRzl3G6zFhlwYyMgxaOEL3zPzjvec1+0Or2sU0uKIrE8+EBaXWd1/Vp0OtocLxaLsRcLFHm78R0/8gN3fCjjfWLE4JKPNvWQKharywRSGbWhuRMLoPl6KnMik1xDxxlftMEr5poHQiKx80Qi8cTGbjUbSmIDdvZm+lWHFoAFWFyu8MR73vKOjxVUmmHtQGLns8l/7LPHLY3Db9xKKRsze9idtX9lnhe5YytYAeFvfuUNv/DJAqzOU5soqgk1vagiUg4QUoa62HEpNZsLChsLwUZibnNjNZcXFoR73vIzf/HFQeWYJiqjcKrWIE8OMgioZ1ANLqjm7gJi0MECjQCL+a2apyUeueeBR8snzrwCXpAGamV5sf05saZqPsRhSjM5qfPUw1t3K2mXdeEVi91i4Myz68UDnaNKidPB2oaCSYnt5xDK++Rqq0Dqzeiz0+cyTumm8HkWcQSDFmw3rL261rl8nTT3YEwzriPm9flWndhkJIpax1maQ5VU5tkM9Tpvl2HCSAm9HuwEPf1rrNY052Ky3JJhukiVy3u1ai9TB+uWsd7lx+tEmayDdIpWZ+/iBk+9RtQSHs4sS6dGHK6qZbh+brG1Wyd6z6KDa5awz6yqLxb8Mvz4IdU6+/wcQjXF+2vJxibXK+qLLzYZ7/iy0qyOZh9Jrr4AHZFe6rj+gk8jX/cxqQVtzqvUzl/LIO0cOKv7tv2Pt38A9nPaiTc6xcoAAAAASUVORK5CYII="

        private val HIGH_END_SHADER = """
            uniform vec2 uResolution;
            uniform float uTime;// nouse
            uniform shader uTex;// nouse

            uniform float uTrackProgress;
            uniform vec2 uTrackSize;
            uniform vec2 uTrackPosition;
            uniform vec2 uTrackCanvasSize;
            uniform vec2 uHeadSize;
            uniform float uHeadGlowAlpha;
            uniform float uHeadScale;
            uniform vec3 uTrackTint;
            uniform float uTrackAlpha;
            uniform vec3 uProgressTint;
            uniform int uIsRtl;


            vec4 addBlend(vec4 src, vec4 dst) {
                vec3 color = src.rgb + dst.rgb;
                float alpha = src.a + (1.0 - src.a) + dst.a;
                return vec4(color, alpha);
            }

            vec4 alphaBlend(vec4 src, vec4 dst) {
                vec3 color = src.rgb + (1.0 - src.a) * dst.rgb;
                float alpha = src.a + dst.a * (1.0 - src.a);
                return vec4(color, alpha);
            }


            float rbx( in vec2 p, in vec2 b, in vec4 r ) {
                r.xy = (p.x>0.0)?r.xy : r.zw;
                r.x  = (p.y>0.0)?r.x  : r.y;
                vec2 q = abs(p)-b+r.x;
                return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r.x;
            }

            float capsule(in vec2 uv, in vec2 pos, in vec2 size) {
                vec2 p = uv - vec2(0.5);
                p.x *= uResolution.x / uResolution.y;

                vec2 b = size / uResolution.y / 2.0;
                //    p.y -= 0.5 - pos.y / uResolution.y;
                p.y += (uResolution.y / 2.0 - pos.y - size.y / 2.0) / uResolution.y;
                p.x -= pos.x / uResolution.y + b.x - uResolution.x / uResolution.y / 2.0;

                vec4 r = vec4(min(size.x, size.y) / uResolution.y / 2.0);
                float d = rbx(p, b, r);
                // return smoothstep(0.001, -0.001, d);
                return d;
            }

            vec4 draw_track(in vec2 uv) {
                float d = capsule(uv, vec2(uTrackPosition.x, uResolution.y - uTrackPosition.y - uTrackSize.y / 2.0), uTrackSize);
                float a = smoothstep(1./uResolution.y, -1./uResolution.y, d);
                return vec4(uTrackTint * a, uTrackAlpha * a);
            }

            vec4 draw_track_progress(in vec2 uv, in float progress) {
                // 1. the current progress
                float m = min(uTrackSize.x, uTrackSize.y);

                vec2 size = uTrackSize;
                size.x = mix(m, uTrackSize.x, progress);

                //    float d = capsule(uv, uTrackPosition, size);
                float d = capsule(uv, vec2(uTrackPosition.x, uResolution.y - uTrackPosition.y - uTrackSize.y / 2.0), size);
                float a = smoothstep(1./uResolution.y, -1./uResolution.y, d) * 0.6;

                //    vec2 csize = uTrackCanvasSize;
                //    vec2 hsize = uHeadSize ;
                vec2 hsize = vec2(75.0, 38.0) * uHeadScale;
                float thight = 6.0 * uHeadScale;
                float xoffset = 52.0 * uHeadScale;

                vec2 st = uv;
                // map st to tracksize
                st.x = (st.x - uTrackPosition.x / uResolution.x) / (uTrackSize.x / uResolution.x);
                st.y -= (uResolution.y - uTrackPosition.y - uTrackSize.y / 2.0) / uResolution.y;
                st.y *= uResolution.y / uTrackSize.y;

                // float startFade = clamp(st.x / (uTrackSize.y / uTrackSize.x * (hsize.x / hsize.y)), 0.0, 1.0);
                float startFade = smoothstep(0.0, uTrackSize.y / uTrackSize.x * (hsize.x / hsize.y) * 1.5, st.x);

                st.x -= mix(uTrackSize.y / 2.0, uTrackSize.x - uTrackSize.y / 2.0, progress) / uTrackSize.x;


                // scale the head size to the track size
                st.y -= 0.5;
                st.x /= (uTrackSize.y / thight) * (hsize.x / uTrackSize.x);
                st.y /= (uTrackSize.y / thight) * (hsize.y / uTrackSize.y);
                st.y += 0.5;

                // move the head to the start of the track
                st.x -= -xoffset / hsize.x;

                vec4 head = uTex.eval(st*uHeadSize);// * 0.5;
                head *= startFade * uHeadGlowAlpha;
                head.rgb *= uProgressTint;
                return alphaBlend(head, vec4(uProgressTint * a, a));
            }

            vec4 main(vec2 fragCoord){
                vec2 vUv = fragCoord / uResolution;
                if (uIsRtl == 1) {
                    vUv.x = 1.0 - vUv.x;
                }
                vec4 color = vec4(0.0);

                // 1. the track
                vec4 track = draw_track(vUv);
                //    return track;
                color = alphaBlend(track, color);

                // 2. the progress
                vec4 progress = draw_track_progress(vUv, uTrackProgress);
                color = alphaBlend(progress, color);
                return color;
            }
        """.trimIndent()

        private val MIDDLE_SHADER = """
            uniform vec2 uResolution;
            uniform float uTime;// nouse
            uniform shader uTex;// nouse

            uniform float uTrackProgress;
            uniform vec2 uTrackSize;
            uniform vec2 uTrackPosition;
            uniform vec2 uTrackCanvasSize;
            uniform vec2 uHeadSize;
            uniform float uHeadGlowAlpha;
            uniform float uHeadScale;
            uniform vec3 uTrackTint;
            uniform float uTrackAlpha;
            uniform vec3 uProgressTint;
            uniform int uIsRtl;


            vec4 addBlend(vec4 src, vec4 dst) {
                vec3 color = src.rgb + dst.rgb;
                float alpha = src.a + (1.0 - src.a) + dst.a;
                return vec4(color, alpha);
            }

            vec4 alphaBlend(vec4 src, vec4 dst) {
                vec3 color = src.rgb + (1.0 - src.a) * dst.rgb;
                float alpha = src.a + dst.a * (1.0 - src.a);
                return vec4(color, alpha);
            }


            float rbx( in vec2 p, in vec2 b, in vec4 r ) {
                r.xy = (p.x>0.0)?r.xy : r.zw;
                r.x  = (p.y>0.0)?r.x  : r.y;
                vec2 q = abs(p)-b+r.x;
                return min(max(q.x,q.y),0.0) + length(max(q,0.0)) - r.x;
            }

            float capsule(in vec2 uv, in vec2 pos, in vec2 size) {
                vec2 p = uv - vec2(0.5);
                p.x *= uResolution.x / uResolution.y;

                vec2 b = size / uResolution.y / 2.0;
                //    p.y -= 0.5 - pos.y / uResolution.y;
                p.y += (uResolution.y / 2.0 - pos.y - size.y / 2.0) / uResolution.y;
                p.x -= pos.x / uResolution.y + b.x - uResolution.x / uResolution.y / 2.0;

                vec4 r = vec4(min(size.x, size.y) / uResolution.y / 2.0);
                float d = rbx(p, b, r);
                // return smoothstep(0.001, -0.001, d);
                return d;
            }

            vec4 draw_track(in vec2 uv) {
                float d = capsule(uv, vec2(uTrackPosition.x, uResolution.y - uTrackPosition.y - uTrackSize.y / 2.0), uTrackSize);
                float a = smoothstep(0.001, -0.001, d);
                return vec4(uTrackTint * a, uTrackAlpha * a);
            }

            vec4 draw_track_progress(in vec2 uv, in float progress) {
                //    vec2 csize = uTrackCanvasSize;
                //    vec2 hsize = uHeadSize ;
                vec2 hsize = vec2(75.0, 38.0) * uHeadScale;
                float thight = 6.0 * uHeadScale;
                float xoffset = 52.0 * uHeadScale;

                vec2 st = uv;
                // map st to tracksize
                st.x = (st.x - uTrackPosition.x / uResolution.x) / (uTrackSize.x / uResolution.x);
                st.y = (st.y - (uResolution.y - uTrackPosition.y - uTrackSize.y / 2.0) / uResolution.y) * (uResolution.y / uTrackSize.y);

                // float startFade = clamp(st.x / (uTrackSize.y / uTrackSize.x * (hsize.x / hsize.y)), 0.0, 1.0);
                float startFade = smoothstep(0.0, uTrackSize.y / uTrackSize.x * (hsize.x / hsize.y) * 1.5, st.x);

                st.x -= mix(uTrackSize.y / 2.0, uTrackSize.x - uTrackSize.y / 2.0, progress) / uTrackSize.x;


                // scale the head size to the track size
                st.y -= 0.5;
                st.x /= (uTrackSize.y / thight) * (hsize.x / uTrackSize.x);
                st.y /= (uTrackSize.y / thight) * (hsize.y / uTrackSize.y);
                st.y += 0.5;

                // move the head to the start of the track
                st.x -= -xoffset / hsize.x;

                vec4 head = uTex.eval(st*uHeadSize);// * 0.5;
                head *= startFade * uHeadGlowAlpha;
                head.rgb *= uProgressTint;
                return head;
            }

            vec4 main(vec2 fragCoord){
                vec2 vUv = fragCoord / uResolution;
                if (uIsRtl == 1) {
                    vUv.x = 1.0 - vUv.x;
                }
                vec4 color = vec4(0.0);

                // 1. the track
            //    vec4 track = draw_track(vUv);
            //    //    return track;
            //    color = alphaBlend(track, color);

            //     2. the progress
                vec4 progress = draw_track_progress(vUv, uTrackProgress);
                color = alphaBlend(progress, color);
                return color;
            }
        """.trimIndent()
    }
}
