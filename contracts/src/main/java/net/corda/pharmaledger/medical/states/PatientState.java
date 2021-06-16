package net.corda.pharmaledger.medical.states;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.pharmaledger.medical.contracts.PatientStateContract;

@BelongsToContract(PatientStateContract.class)
public class PatientState implements ContractState {
    private int patientID;
    private String mediStaff;
    private JsonArray patientEvaluation;
    private JsonObject patientBiometricData;
    private List<AbstractParty> participants;

    public PatientState(int patientID, String mediStaff,  String gender, int weight, int height, int age, AnonymousParty medicalAccount, AnonymousParty pharmaAccount) {
        this.patientID = patientID;
        this.mediStaff = mediStaff;
        this.patientBiometricData=null;
        this.patientBiometricData.addProperty("gender", gender);
        this.patientBiometricData.addProperty("weight", weight);
        this.patientBiometricData.addProperty("height", height);
        this.patientBiometricData.addProperty("age", age);
        this.participants = new ArrayList<AbstractParty>();
        participants.add(medicalAccount);
        participants.add(pharmaAccount);
    }

    

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



    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

}
