package net.corda.pharmaledger.medical.states;

import java.util.ArrayList;
import java.util.List;

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
    private String patientEvaluation;
    private String patientBiometricData;
    private List<AbstractParty> participants;

    public PatientState(int patientID, String mediStaff,  String gender, int weight, int height, int age, AnonymousParty medicalAccount, AnonymousParty pharmaAccount) {
        this.patientID = patientID;
        this.mediStaff = mediStaff;
        JsonObject patientBiometricDataJson = new JsonObject();
        patientBiometricDataJson.addProperty("gender", gender);
        patientBiometricDataJson.addProperty("weight", weight);
        patientBiometricDataJson.addProperty("height", height);
        patientBiometricDataJson.addProperty("age", age);
        this.patientBiometricData=patientBiometricDataJson.toString();
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

    public String getPatientEvaluation() {
        return this.patientEvaluation;
    }

    public void setPatientEvaluation(String patientEvaluation) {
        this.patientEvaluation = patientEvaluation;
    }

    public String getPatientBiometricData() {
        return this.patientBiometricData;
    }

    public void setPatientBiometricData(String patientBiometricData) {
        this.patientBiometricData = patientBiometricData;
    }



    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

}
