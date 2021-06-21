package net.corda.pharmaledger.medical;

import java.security.PublicKey;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.r3.corda.lib.accounts.contracts.states.AccountInfo;
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount;
import com.r3.corda.lib.accounts.workflows.services.AccountService;
import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.TransactionSignature;
import net.corda.core.flows.CollectSignatureFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.ReceiveFinalityFlow;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.pharmaledger.accountUtilities.NewKeyForAccount;
import net.corda.pharmaledger.medical.contracts.PatientEvaluationStateContract;
import net.corda.pharmaledger.medical.states.PatientEvaluationState;
import net.corda.pharmaledger.medical.states.PatientState;

@InitiatingFlow
@StartableByRPC
public class updatePatientEvaluationData  extends FlowLogic<String> {
    private int patientID;
    private String symptoms;
    private String evaluationDate;
    private String  evaluationResult;
    private String fromMedical;
    private String toPharma;

    public updatePatientEvaluationData(int patientID, String symptoms, String evaluationDate, String evaluationResult, String fromMedical, String toPharma) {
        this.patientID = patientID;
        this.symptoms = symptoms;
        this.evaluationDate = evaluationDate;
        this.evaluationResult = evaluationResult;
        this.fromMedical = fromMedical;
        this.toPharma = toPharma;
    }

    @Override
    @Suspendable
    public String call() throws FlowException {
        List<StateAndRef<PatientState>> patientStateAndRefs = getServiceHub().getVaultService()
        .queryBy(PatientState.class).getStates();

        StateAndRef<PatientState> inputStateAndRef = patientStateAndRefs.stream().filter(patientStateAndRef -> {
            PatientState patientState = patientStateAndRef.getState().getData();
            return patientState.getPatientID()==patientID;
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Patient ID Not Found"));

        List<StateAndRef<PatientEvaluationState>> patientEvaluationStateAndRefs = getServiceHub().getVaultService()
        .queryBy(PatientEvaluationState.class).getStates();

        StateAndRef<PatientEvaluationState> updateinputStateAndRef = patientEvaluationStateAndRefs.stream().filter(patientEvaluationStateAndRef -> {
            PatientEvaluationState patientEvaluationState = patientEvaluationStateAndRef.getState().getData();
            return patientEvaluationState.getPatientID()==patientID;
        }).findAny().orElseThrow(() -> new IllegalArgumentException("Patient ID Not Found"));
        
        AccountService accountService = getServiceHub().cordaService(KeyManagementBackedAccountService.class);
        AccountInfo myAccount = accountService.accountInfo(fromMedical).get(0).getState().getData();
        PublicKey myKey = subFlow(new NewKeyForAccount(myAccount.getIdentifier().getId())).getOwningKey();
        AccountInfo targetAccount = accountService.accountInfo(toPharma).get(0).getState().getData();
        AnonymousParty targetAcctAnonymousParty = subFlow(new RequestKeyForAccount(targetAccount));
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

        Date evaluationDateObject = new Date();
        try {
            evaluationDateObject = new SimpleDateFormat("dd/MM/yyyy").parse(evaluationDate);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        PatientEvaluationState patientEvaluationData = new PatientEvaluationState (patientID, symptoms, evaluationDateObject,evaluationResult,  new AnonymousParty(myKey), targetAcctAnonymousParty);

        TransactionBuilder txbuilder = new TransactionBuilder(notary)
        .addInputState(updateinputStateAndRef)
        .addOutputState(patientEvaluationData)
        .addCommand(new PatientEvaluationStateContract.Commands.Create(), Arrays.asList(targetAcctAnonymousParty.getOwningKey(),myKey));

    SignedTransaction locallySignedTx = getServiceHub().signInitialTransaction(txbuilder,Arrays.asList(getOurIdentity().getOwningKey(),myKey));
    FlowSession sessionForAccountToSendTo = initiateFlow(targetAccount.getHost());

    List<TransactionSignature> accountToMoveToSignature = (List<TransactionSignature>) subFlow(new CollectSignatureFlow(locallySignedTx,
            sessionForAccountToSendTo,targetAcctAnonymousParty.getOwningKey()));
    SignedTransaction signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature);

    subFlow(new FinalityFlow(signedByCounterParty,
            Arrays.asList(sessionForAccountToSendTo).stream().filter(it -> it.getCounterparty() != getOurIdentity()).collect(Collectors.toList())));
    return "Successfully sent evaluation for patient ID:" + patientID;
    }
    
}

@InitiatedBy(updatePatientEvaluationData.class)
class updatePatientEvaluationDataResponder extends FlowLogic<Void> {
    //private variable
    private FlowSession counterpartySession;

    //Constructor
    public updatePatientEvaluationDataResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Override
    @Suspendable
    public Void call() throws FlowException {
        subFlow(new SignTransactionFlow(counterpartySession) {
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // Custom Logic to validate transaction.
            }
        });
        subFlow(new ReceiveFinalityFlow(counterpartySession));
        return null;
    }
}