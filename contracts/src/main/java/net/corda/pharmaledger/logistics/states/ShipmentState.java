package net.corda.pharmaledger.logistics.states;

import com.google.gson.*;

import net.corda.core.contracts.BelongsToContract;
import net.corda.pharmaledger.logistics.contracts.ShipmentStateContract;

@BelongsToContract(ShipmentStateContract.class)
public class ShipmentState {
    private int shipmentID;
    private JsonObject shipmentLabel;
    private String status; 

    public int getShipmentID() {
        return this.shipmentID;
    }

    public void setShipmentID(int shipmentID) {
        this.shipmentID = shipmentID;
    }


    public JsonObject getShipmentLabel() {
        return this.shipmentLabel;
    }

    public void setShipmentLabel(JsonObject shipmentLabel) {
        this.shipmentLabel = shipmentLabel;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
