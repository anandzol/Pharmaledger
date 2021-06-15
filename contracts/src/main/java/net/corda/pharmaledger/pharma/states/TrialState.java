package net.corda.pharmaledger.pharma.states;

import com.google.gson.JsonArray;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.pharma.contracts.TrialStateContract;

@BelongsToContract(TrialStateContract.class)
public class TrialState {
    private int trialID;
    private JsonArray patient; 
    private String status;


    public int getTrialID() {
        return this.trialID;
    }

    public void setTrialID(int trialID) {
        this.trialID = trialID;
    }

    public JsonArray getPatient() {
        return this.patient;
    }

    public void setPatient(JsonArray patient) {
        this.patient = patient;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


}
