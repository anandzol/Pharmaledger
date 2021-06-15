package net.corda.pharmaledger.medical.states;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.medical.contracts.PatientStateContract;

@BelongsToContract(PatientStateContract.class)
public class PatientState {
    private int patientID;
    private String mediStaff;
    private String patientAddress;
    private JsonArray patientEvaluation;
    private JsonObject patientBiometricData;


    public int getPatientID() {
        return this.patientID;
    }

    public void setPatientID(int patientID) {
        this.patientID = patientID;
    }

    public String getMediStaff() {
        return this.mediStaff;
    }

    public void setMediStaff(String mediStaff) {
        this.mediStaff = mediStaff;
    }

    public String getPatientAddress() {
        return this.patientAddress;
    }

    public void setPatientAddress(String patientAddress) {
        this.patientAddress = patientAddress;
    }

    public JsonArray getPatientEvaluation() {
        return this.patientEvaluation;
    }

    public void setPatientEvaluation(JsonArray patientEvaluation) {
        this.patientEvaluation = patientEvaluation;
    }

    public JsonObject getPatientBiometricData() {
        return this.patientBiometricData;
    }

    public void setPatientBiometricData(JsonObject patientBiometricData) {
        this.patientBiometricData = patientBiometricData;
    }

}
