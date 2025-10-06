package com.example.myapplication2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class HeatMapViewR extends View {

    private Bitmap footBitmap;
    private List<SensorRegionR> regions = new ArrayList<>();
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HeatMapViewR(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        footBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.footrightt);
    }

    public void setRegions(List<SensorRegionR> regs) {
        regions = regs;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Desenha a imagem do pé
        canvas.drawBitmap(footBitmap, null, new Rect(0, 0, getWidth(), getHeight()), null);

        // Desenha os pontos suavizados com degradê HSV dinâmico
        for (SensorRegionR r : regions) {
            float cx = r.x * getWidth();
            float cy = r.y * getHeight();
            float radius = r.radius * getWidth();

            // Cor baseada na pressão bruta (0–4095) mapeada para o espectro de cores
            int centerColor = getHeatColor(r.pressure);
            int edgeColor = Color.TRANSPARENT;

            RadialGradient gradient = new RadialGradient(
                    cx, cy, radius,
                    centerColor, edgeColor,
                    Shader.TileMode.CLAMP
            );

            paint.setShader(gradient);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setShader(null);
        }
    }
    private int getHeatColor(float rawValue) {
        // Normaliza para [0f,1f]
        float frac = Math.min(1f, Math.max(0f, rawValue / 4095f));
        // Inverte frac para que 0 seja azul e 1 seja vermelho
        float hue = (1f - frac) * 240f;
        return Color.HSVToColor(new float[]{ hue, 1f, 1f });
    }

    public static class SensorRegionR {
        public float x, y, pressure, radius;

        public SensorRegionR(float x, float y, float pressure, float radius) {
            this.x = x;
            this.y = y;
            this.pressure = pressure;
            this.radius = radius;
        }
    }
}
