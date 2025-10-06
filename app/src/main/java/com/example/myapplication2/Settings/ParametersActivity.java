package com.example.myapplication2.Settings;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.ConectInsole;
import com.example.myapplication2.ConectInsole2;
import com.example.myapplication2.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class ParametersActivity extends AppCompatActivity{

    SeekBar limup, limdown;
    TextView limdownText, limupText;
    String InRight, InLeft;
    FloatingActionButton mBackBtn;
    Button saveChanges;
    int percentageAdjustLeft, percentageAdjustRight;
    ConectInsole conectInsole;
    ConectInsole2 conectInsole2;
    private Calendar calendar;
    private SharedPreferences sharedPreferences;
    List<Short> resultList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parameters);
    }

    @Override
    public void onStart() {
        super.onStart();

        conectInsole = new ConectInsole(ParametersActivity.this);
        conectInsole2 = new ConectInsole2(ParametersActivity.this);
        limupText = findViewById(R.id.limupText);
        limdownText = findViewById(R.id.limdownText);
        mBackBtn = findViewById(R.id.buttonback2);
        saveChanges = findViewById(R.id.savechanges);
        limup = findViewById(R.id.limup);
        limdown = findViewById(R.id.limdown);

        sharedPreferences = getSharedPreferences("My_Appinsolesamount", MODE_PRIVATE);
        InRight = sharedPreferences.getString("Sright", "default");
        InLeft = sharedPreferences.getString("Sleft", "default");


        if (InRight.equals("false") && InLeft.equals("true")) {
            limdown.setVisibility(View.GONE);
            limdownText.setVisibility(View.GONE);
            limupText.setText("Limiar: 0%");

            limup.setProgress(0);
            limup.incrementProgressBy(50);
            limup.setMax(+50);
            limup.setMin(-50);

            limup.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    limupText.setText("Limiar em:" + String.valueOf(progress));
                    percentageAdjustLeft = progress;

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }
        if (InLeft.equals("false") && InRight.equals("true")) {
            limdown.setVisibility(View.GONE);
            limdownText.setVisibility(View.GONE);
            limupText.setText("Limiar: 0%");

            limup.setProgress(0);
            limup.incrementProgressBy(50);
            limup.setMax(+50);
            limup.setMin(-50);

            limup.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    limupText.setText("Limiar em:" + String.valueOf(progress));
                    percentageAdjustRight = progress;

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });


        }

        else {

            //configura palmilha direita
            limupText.setText("Limiar palmilha direita: 0%");

            limup.setProgress(0);
            limup.incrementProgressBy(50);
            limup.setMax(+50);
            limup.setMin(-50);

            limup.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    limupText.setText("Limiar palmilha direita:" + String.valueOf(progress));
                    percentageAdjustRight = progress;

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
            //configura palmilha esquerda
            limdownText.setText("Limiar palmilha esquerda: 0%");

            limdown.setProgress(0);
            limdown.incrementProgressBy(50);
            limdown.setMax(+50);
            limdown.setMin(-50);

            limdown.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    limdownText.setText("Limiar palmilha esquerda:" + String.valueOf(progress));
                    percentageAdjustLeft = progress;

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });

        }

        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(InRight.equals("true")){
                    //recalcular limiar e enviar a palmilha direita
                    loadTreshData(percentageAdjustRight);
                }
                if(InLeft.equals("true")){
                    //recalcular limiar e enviar a palmilha esquerda
                    loadTreshData2(percentageAdjustLeft);

                }


            }
        });

        mBackBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }


    private void loadTreshData(int treshold) {

        byte cmd = 0x2A;
        byte freq = 1;
        List<Short> tnumbers = new ArrayList<>();
        SharedPreferences sharedPreferences = getSharedPreferences("ConfigPrefs1", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("percentageR", String.valueOf(treshold));

        tnumbers.add( (short) sharedPreferences.getInt("S1", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S2", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S3", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S4", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S5", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S6", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S7", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S8", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S9", 0));

        List<Short> newList=PersentLimCalc(tnumbers,treshold);
        System.out.println("Resultados de limiar R" + newList.get(0)+","+  newList.get(1)+","+ newList.get(2)+","+ newList.get(3)+","+ newList.get(4)+","+  newList.get(5)+","+ newList.get(6)+","+ newList.get(7)+","+ newList.get(8));
        conectInsole.createAndSendConfigData(cmd, freq, newList.get(0), newList.get(1), newList.get(2), newList.get(3), newList.get(4),  newList.get(5), newList.get(6), newList.get(7), newList.get(8));
    }

    private void loadTreshData2(int treshold) {

        byte cmd = 0x2A;
        byte freq = 1;

        List<Short> tnumbers = new ArrayList<>();
        sharedPreferences = getSharedPreferences("ConfigPrefs2", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("percentageL", String.valueOf(treshold));

        tnumbers.add( (short) sharedPreferences.getInt("S1", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S2", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S3", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S4", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S5", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S6", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S7", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S8", 0));
        tnumbers.add( (short) sharedPreferences.getInt("S9", 0));
        List<Short> newListL=PersentLimCalcL(tnumbers,treshold);
        System.out.println("Resultados de limiar L" + newListL.get(0)+","+  newListL.get(1)+","+ newListL.get(2)+","+ newListL.get(3)+","+ newListL.get(4)+","+  newListL.get(5)+","+ newListL.get(6)+","+ newListL.get(7)+","+ newListL.get(8));
        conectInsole2.createAndSendConfigData(cmd, freq, newListL.get(0), newListL.get(1), newListL.get(2), newListL.get(3), newListL.get(4),  newListL.get(5), newListL.get(6), newListL.get(7), newListL.get(8));
    }

    public List<Short> PersentLimCalc(List<Short> tnumbers, int percent) {
        // Decodifica os dados puros e os headers originais
        DecodedResult result     = processList(tnumbers);
        List<Short> dataList      = result.getDataList();     // valores originais de 0x000..0xFFF
        List<Boolean> flags       = result.getHeaderFlags();  // true se header era 0x3, false se era 0x1

        // Cria lista nova vazia
        List<Short> tnumberNew   = new ArrayList<>(tnumbers.size());

        for (int i = 0; i < tnumbers.size(); i++) {
            int original       = dataList.get(i);
            int delta          = (int) Math.round(original * percent / 100.0);
            int adjusted       = original + delta;
            if (adjusted < 0)         adjusted = 0;
            else if (adjusted > 0x0FFF) adjusted = 0x0FFF;

            int headerNibble   = flags.get(i) ? 0x3 : 0x1;
            short newValue     = (short) ((headerNibble << 12) | (adjusted & 0x0FFF));

            // adiciona novo valor—não use set()
            tnumberNew.add(newValue);
        }

        return tnumberNew;
    }

    public static class DecodedResult {
        private final List<Short> dataList;
        private final List<Boolean> headerFlags;

        public DecodedResult(List<Short> dataList, List<Boolean> headerFlags) {
            this.dataList = dataList;
            this.headerFlags = headerFlags;
        }

        public List<Short> getDataList() {
            return dataList;
        }

        public List<Boolean> getHeaderFlags() {
            return headerFlags;
        }
    }

    /**
     * Recebe uma lista de shorts no formato:
     *   [header (4 bits; 0x3 ou 0x1) | data (12 bits)]
     * Retorna um DecodedResult contendo:
     *  - a lista dos 12 bits de dados originais
     *  - uma lista de flags indicando quais headers eram 0x3 (true) ou 0x1 (false)
     */
    public static DecodedResult processList(List<Short> limSList) {
        List<Short> dataList = new ArrayList<>(limSList.size());
        List<Boolean> headerFlags = new ArrayList<>(limSList.size());

        for (short v : limSList) {
            // extrai header (4 bits mais significativos)
            int header = (v >> 12) & 0xF;
            headerFlags.add(header == 0x3);

            // extrai os 12 bits de dados originais
            short data = (short) (v & 0x0FFF);
            dataList.add(data);
        }

        return new DecodedResult(dataList, headerFlags);
    }
    public List<Short> PersentLimCalcL(List<Short> tnumbers, int percent) {
        // Decodifica os dados puros e os headers originais
        DecodedResultL result     = processListL(tnumbers);
        List<Short> dataList      = result.getDataList();     // valores originais de 0x000..0xFFF
        List<Boolean> flags       = result.getHeaderFlags();  // true se header era 0x3, false se era 0x1

        // Cria lista nova vazia
        List<Short> tnumberNewL   = new ArrayList<>(tnumbers.size());

        for (int i = 0; i < tnumbers.size(); i++) {
            int original       = dataList.get(i);
            int delta          = (int) Math.round(original * percent / 100.0);
            int adjusted       = original + delta;
            if (adjusted < 0)         adjusted = 0;
            else if (adjusted > 0x0FFF) adjusted = 0x0FFF;

            int headerNibble   = flags.get(i) ? 0x3 : 0x1;
            short newValue     = (short) ((headerNibble << 12) | (adjusted & 0x0FFF));

            // adiciona novo valor—não use set()
            tnumberNewL.add(newValue);
        }

        return tnumberNewL;
    }

    public static class DecodedResultL {
        private final List<Short> dataList;
        private final List<Boolean> headerFlags;

        public DecodedResultL(List<Short> dataList, List<Boolean> headerFlags) {
            this.dataList = dataList;
            this.headerFlags = headerFlags;
        }

        public List<Short> getDataList() {
            return dataList;
        }

        public List<Boolean> getHeaderFlags() {
            return headerFlags;
        }
    }

    /**
     * Recebe uma lista de shorts no formato:
     *   [header (4 bits; 0x3 ou 0x1) | data (12 bits)]
     * Retorna um DecodedResult contendo:
     *  - a lista dos 12 bits de dados originais
     *  - uma lista de flags indicando quais headers eram 0x3 (true) ou 0x1 (false)
     */
    public static DecodedResultL processListL(List<Short> limSList) {
        List<Short> dataList = new ArrayList<>(limSList.size());
        List<Boolean> headerFlags = new ArrayList<>(limSList.size());

        for (short v : limSList) {
            // extrai header (4 bits mais significativos)
            int header = (v >> 12) & 0xF;
            headerFlags.add(header == 0x3);

            // extrai os 12 bits de dados originais
            short data = (short) (v & 0x0FFF);
            dataList.add(data);
        }

        return new DecodedResultL(dataList, headerFlags);
    }
}

