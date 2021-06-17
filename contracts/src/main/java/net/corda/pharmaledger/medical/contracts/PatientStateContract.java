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
			//require.using("Weight should not be greater than 200KGs", patient.getPatientBiometricData().get(Constants.BIOMETRIC_WEIGHT).getAsInt() > 200);
			//require.using("Age should not be greater than 150", patient.getPatientBiometricData().get(Constants.BIOMETRIC_AGE).getAsInt() > 150);
			//require.using("Height should not be greater than 300cm", patient.getPatientBiometricData().get(Constants.BIOMETRIC_HEIGHT).getAsInt() > 300);
			//require.using("Gender should not be empty", !StringUtils.isEmpty(patient.getPatientBiometricData().get(Constants.BIOMETRIC_GENDER).getAsString()));
			//require.using("Blood Type should not be empty", !StringUtils.isEmpty(patient.getPatientBiometricData().get(Constants.BIOMETRIC_BLOODTYPE).getAsString()));
			return null;
		});
	}

	public interface Commands extends CommandData {
        class Create implements Commands {}
    }
}
