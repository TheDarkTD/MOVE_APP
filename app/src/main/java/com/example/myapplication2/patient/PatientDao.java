// app/src/main/java/com/example/myapplication2/patient/PatientDao.java
package com.example.myapplication2.patient;

import com.google.android.gms.tasks.Task;
import com.google.firebase.database.*;

public class PatientDao {

    private final DatabaseReference root; // /Users/{uid}/patients

    public PatientDao(String uid) {
        this.root = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("patients");
    }

    public static String normalizeCpf(String cpfRaw) {
        if (cpfRaw == null) return null;
        return cpfRaw.replaceAll("\\D+", "");
    }

    public Task<Void> save(Patient p) {
        String key = normalizeCpf(p.cpf);
        return root.child(key).setValue(p);
    }

    public DatabaseReference refByCpf(String cpfRaw) {
        String key = normalizeCpf(cpfRaw);
        return root.child(key);
    }
}
