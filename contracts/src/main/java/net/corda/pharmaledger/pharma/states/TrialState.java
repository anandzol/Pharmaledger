package net.corda.pharmaledger.pharma.states;

import com.google.gson.*;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.pharma.contracts.TrialStateContract;

@BelongsToContract(TrialStateContract.class)
public class TrialState {
    private int trialID;
    private JsonObject trialDetails; 


    public int getTrialID() {
        return this.trialID;
    }

    public void setTrialID(int trialID) {
        this.trialID = trialID;
    }

    public JsonObject getTrialDetails() {
        return this.trialDetails;
    }

    public void setTrialDetails(JsonObject trialDetails) {
        Gson gson = new Gson();
        this.trialDetails = trialDetails;
    }
}
