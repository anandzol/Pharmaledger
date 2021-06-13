package net.corda.pharmaledger.medical.states;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.medical.contracts.PatientAddressStateContract;
import com.google.gson.*;


@BelongsToContract(PatientAddressStateContract.class)
public class PatientAddressState {
    private JsonObject patientAddress;

    public JsonObject getPatientAddress() {
        return this.patientAddress;
    }

    public void setPatientAddress(JsonObject patientAddress) {
        this.patientAddress = patientAddress;
    }
}

