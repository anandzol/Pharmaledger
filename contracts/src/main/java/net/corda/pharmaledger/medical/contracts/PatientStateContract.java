package net.corda.pharmaledger.medical.contracts;

import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.pharmaledger.medical.states.PatientState;

public class PatientStateContract implements Contract {
	public static final String ID = "net.corda.pharmaledger.medical.contracts.PatientStateContract";
	@Override
	public void verify(LedgerTransaction tx) throws IllegalArgumentException {
		List<ContractState> outputs = tx.getOutputStates();
		requireThat(require -> {
			PatientState patient = (PatientState) outputs.get(0);
			require.using("ID should be greater than 0", patient.getPatientID() > 0);
			require.using("Name should not be empty", !StringUtils.isEmpty(patient.getMediStaff()));
			require.using("Shipment Mapping ID should not be empty", !StringUtils.isEmpty(patient.getShipmentMappingID()));
			require.using("Medical Staff Details should not be empty", !StringUtils.isEmpty(patient.getMediStaff()));
			require.using("Age should be positive value", patient.getAge() >= 0);
			require.using("Weight should be positive value", patient.getWeight() > 0);
			require.using("Height should be positive value", patient.getHeight() > 0);
			require.using("Gender should not be empty", !StringUtils.isEmpty(patient.getGender()));
			return null;
		});
	}

	public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
