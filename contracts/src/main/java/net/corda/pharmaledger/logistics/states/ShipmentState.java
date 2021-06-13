package net.corda.pharmaledger.logistics.states;

import com.google.gson.*;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.logistics.contracts.ShipmentStateContract;

@BelongsToContract(ShipmentStateContract.class)
public class ShipmentState {
    private int shipmentID;
    private JsonObject shipmentDetails; 

    public int getShipmentID() {
        return this.shipmentID;
    }

    public void setShipmentID(int shipmentID) {
        this.shipmentID = shipmentID;
    }

    public JsonObject getShipmentDetails() {
        return this.shipmentDetails;
    }

    public void setShipmentDetails(JsonObject shipmentDetails) {
        this.shipmentDetails = shipmentDetails;
    }

}
