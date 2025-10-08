// app/src/main/java/com/example/myapplication2/patient/Patient.java
package com.example.myapplication2.patient;

public class Patient {
    public String cpf;       // somente dígitos
    public String name;
    public Double weightKg;  // kg
    public Double heightM;   // metros
    public Integer shoe;     // opcional

    public Patient() {} // Firebase exige construtor vazio

    public Patient(String cpf, String name, Double weightKg, Double heightM, Integer shoe) {
        this.cpf = cpf;
        this.name = name;
        this.weightKg = weightKg;
        this.heightM = heightM;
        this.shoe = shoe;
    }
}
