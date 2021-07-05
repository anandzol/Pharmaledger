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
    private String patientID;
    private String shipmentMappingID;
    private String mediStaff;
    private String patientEvaluation;
    //private String patientBiometricData; currently using expanded bio data
    private AnonymousParty medicalAccount;
    private AnonymousParty pharmaAccount;
    private int Age;
    private String Gender;
    private int weight; 
    private int height;
    private List<AbstractParty> participants;

    public PatientState(String patientID, String shipmentMappingID, String mediStaff, int Age, String Gender, int weight, int height,AnonymousParty medicalAccount, AnonymousParty pharmaAccount) {
        this.patientID = patientID;
        this.shipmentMappingID = shipmentMappingID;
        this.mediStaff = mediStaff;
        //this.patientEvaluation = patientEvaluation;
        this.medicalAccount = medicalAccount;
        this.pharmaAccount = pharmaAccount;
        this.Age = Age;
        this.Gender = Gender;
        this.weight = weight;
        this.height = height;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(medicalAccount);
        participants.add(pharmaAccount);
    }

    /*public PatientState(int patientID, String mediStaff, int Age,  String Gender, int Height, int Weight, String patientEvaluation, AnonymousParty medicalAccount, AnonymousParty pharmaAccount) {
        this.patientID = patientID;
        this.mediStaff = mediStaff;
        //this.patientBiometricData = patientBiometricData;
        this.patientEvaluation = patientEvaluation;

        this.medicalAccount = medicalAccount;
        this.pharmaAccount = pharmaAccount;
        this.participants = new ArrayList<AbstractParty>();
        participants.add(medicalAccount);
        participants.add(pharmaAccount);
    }*/


    public String getShipmentMappingID() {
        return this.shipmentMappingID;
    }

    public void setShipmentMappingID(String shipmentMappingID) {
        this.shipmentMappingID = shipmentMappingID;
    }

    public int getAge() {
        return this.Age;
    }

    public void setAge(int Age) {
        this.Age = Age;
    }

    public String getGender() {
        return this.Gender;
    }

    public void setGender(String Gender) {
        this.Gender = Gender;
    }

    public int getWeight() {
        return this.weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getHeight() {
        return this.height;
    }

    public void setHeight(int height) {
        this.height = height;
    }
    public void setParticipants(List<AbstractParty> participants) {
        this.participants = participants;
    }
    public String getPatientID() {
        return this.patientID;
    }

    public void setPatientID(String patientID) {
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

    /*public String getPatientBiometricData() {
        return this.patientBiometricData;
    }

    public void setPatientBiometricData(String patientBiometricData) {
        this.patientBiometricData = patientBiometricData;
    }*/


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
