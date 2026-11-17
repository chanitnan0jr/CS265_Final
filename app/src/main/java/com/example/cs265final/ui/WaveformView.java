package com.example.cs265final.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.cs265final.R;

/** Draws rounded vertical waveform bars using semantic active/inactive theme colors. */
public final class WaveformView extends View {
  private static final float[] DEFAULT_BAR_HEIGHTS =
      new float[] {
        0.16f, 0.24f, 0.18f, 0.30f, 0.42f, 0.68f, 0.38f, 0.24f, 0.48f, 0.34f,
        0.22f, 0.50f, 0.36f, 0.60f, 0.92f, 0.72f, 0.54f, 0.80f, 0.42f, 0.26f,
        0.44f, 0.30f, 0.56f, 0.38f, 0.24f, 0.30f, 0.20f, 0.24f, 0.16f
      };

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF barRect = new RectF();
  private boolean active;

  public WaveformView(Context context) {
    super(context);
  }

  public WaveformView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public WaveformView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setActive(boolean active) {
    if (this.active != active) {
      this.active = active;
      invalidate();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    int width = getWidth() - getPaddingLeft() - getPaddingRight();
    int height = getHeight() - getPaddingTop() - getPaddingBottom();
    if (width <= 0 || height <= 0) {
      return;
    }
    int count = DEFAULT_BAR_HEIGHTS.length;
    float barWidth = width / (float) Math.max(1, count * 2 - 1);
    float radius = barWidth / 2f;
    paint.setColor(
        ContextCompat.getColor(
            getContext(), active ? R.color.primary_coral : R.color.border_soft));
    float leftStart = getPaddingLeft();
    float topStart = getPaddingTop();
    for (int i = 0; i < count; i++) {
      float fraction = Math.max(0.14f, Math.min(1f, DEFAULT_BAR_HEIGHTS[i]));
      float barHeight = height * fraction;
      float left = leftStart + i * barWidth * 2f;
      float top = topStart + (height - barHeight) / 2f;
      barRect.set(left, top, left + barWidth, top + barHeight);
      canvas.drawRoundRect(barRect, radius, radius, paint);
    }
  }
}
