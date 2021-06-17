package net.corda.pharmaledger.medical.states;

import java.util.ArrayList;
import java.util.List;

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
    private AnonymousParty medicalAccount;
    private AnonymousParty pharmaAccount;
    private List<AbstractParty> participants;

    public PatientState(int patientID, String mediStaff,  String patientBiometricData, String patientEvaluation, AnonymousParty medicalAccount, AnonymousParty pharmaAccount) {
        this.patientID = patientID;
        this.mediStaff = mediStaff;
        this.patientBiometricData = patientBiometricData;
        this.patientEvaluation = patientEvaluation;
        this.medicalAccount = medicalAccount;
        this.pharmaAccount = pharmaAccount;
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


    public AnonymousParty getMedicalAccount() {
        return this.medicalAccount;
    }

    public void setMedicalAccount(AnonymousParty medicalAccount) {
        this.medicalAccount = medicalAccount;
    }

    public AnonymousParty getPharmaAccount() {
        return this.pharmaAccount;
    }

    public void setPharmaAccount(AnonymousParty pharmaAccount) {
        this.pharmaAccount = pharmaAccount;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return this.participants;
    }

}
