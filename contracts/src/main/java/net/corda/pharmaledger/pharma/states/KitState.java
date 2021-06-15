package net.corda.pharmaledger.pharma.states;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.pharma.contracts.KitStateContract;

@BelongsToContract(KitStateContract.class)
public class KitState {
    private int kitID;
    private JsonObject patient;
    private JsonObject shipment;
    private JsonArray medication;
    

    public KitState() {
    }

    public int getKitID() {
        return this.kitID;
    }

    public void setKitID(int kitID) {
        this.kitID = kitID;
    }


    public JsonObject getPatient() {
        return this.patient;
    }

    public void setPatient(JsonObject patient) {
        this.patient = patient;
    }

    public JsonObject getShipment() {
        return this.shipment;
    }

    public void setShipment(JsonObject shipment) {
        this.shipment = shipment;
    }

    public JsonArray getMedication() {
        return this.medication;
    }

    public void setMedication(JsonArray medication) {
        this.medication = medication;
    }

}
