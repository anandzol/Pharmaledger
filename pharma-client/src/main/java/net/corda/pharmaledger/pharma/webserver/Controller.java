package net.corda.pharmaledger.pharma.webserver;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.corda.client.jackson.JacksonSupport;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.pharmaledger.medical.states.PatientEvaluationState;
import net.corda.pharmaledger.medical.states.PatientState;
import net.corda.pharmaledger.pharma.DeleteMedicalStaffData;
import net.corda.pharmaledger.pharma.EditTrial;
import net.corda.pharmaledger.pharma.SendMedicalStaffData;
import net.corda.pharmaledger.pharma.SendShipmentRequest;
import net.corda.pharmaledger.pharma.SendTrial;
import net.corda.pharmaledger.pharma.states.KitShipmentState;
import net.corda.pharmaledger.pharma.states.ShipmentRequestState;

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/v1/pharma") // The paths for HTTP requests are relative to this base path.
public class Controller {
    private static final Logger logger = LoggerFactory.getLogger(RestController.class);
    private final CordaRPCOps proxy;
    private final CordaX500Name me;

    public Controller(NodeRPCConnection rpc) {
        this.proxy = rpc.proxy;
        this.me = proxy.nodeInfo().getLegalIdentities().get(0).getName();

    }

    /** Helpers for filtering the network map cache. */
    public String toDisplayString(X500Name name){
        return BCStyle.INSTANCE.toString(name);
    }

    private boolean isNotary(NodeInfo nodeInfo) {
        return !proxy.notaryIdentities()
                .stream().filter(el -> nodeInfo.isLegalIdentity(el))
                .collect(Collectors.toList()).isEmpty();
    }

    private boolean isMe(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().equals(me);
    }

    private boolean isNetworkMap(NodeInfo nodeInfo){
        return nodeInfo.getLegalIdentities().get(0).getName().getOrganisation().equals("Network Map Service");
    }

    @Configuration
    class Plugin {
        @Bean
        public ObjectMapper registerModule() {
            return JacksonSupport.createNonRpcMapper();
        }
    }

    @GetMapping(value = "/status", produces = TEXT_PLAIN_VALUE)
    public String status() {
        return "200";
    }

    @GetMapping(value = "/servertime", produces = TEXT_PLAIN_VALUE)
    public String serverTime() {
        return (LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC"))).toString();
    }

    @GetMapping(value = "/addresses", produces = TEXT_PLAIN_VALUE)
    public String addresses() {
        return proxy.nodeInfo().getAddresses().toString();
    }

    @GetMapping(value = "/identities", produces = TEXT_PLAIN_VALUE)
    public String identities() {
        return proxy.nodeInfo().getLegalIdentities().toString();
    }

    @GetMapping(value = "/platformversion", produces = TEXT_PLAIN_VALUE)
    public String platformVersion() {
        return Integer.toString(proxy.nodeInfo().getPlatformVersion());
    }

    @GetMapping(value = "/peers", produces = APPLICATION_JSON_VALUE)
    public HashMap<String, List<String>> getPeers() {
        HashMap<String, List<String>> myMap = new HashMap<>();

        // Find all nodes that are not notaries, ourself, or the network map.
        Stream<NodeInfo> filteredNodes = proxy.networkMapSnapshot().stream()
                .filter(el -> !isNotary(el) && !isMe(el) && !isNetworkMap(el));
        // Get their names as strings
        List<String> nodeNames = filteredNodes.map(el -> el.getLegalIdentities().get(0).getName().toString())
                .collect(Collectors.toList());

        myMap.put("peers", nodeNames);
        return myMap;
    }

    @GetMapping(value = "/notaries", produces = TEXT_PLAIN_VALUE)
    public String notaries() {
        return proxy.notaryIdentities().toString();
    }

    @GetMapping(value = "/flows", produces = TEXT_PLAIN_VALUE)
    public String flows() {
        return proxy.registeredFlows().toString();
    }

    @GetMapping(value = "/states", produces = TEXT_PLAIN_VALUE)
    public String states() {
        return proxy.vaultQuery(ContractState.class).getStates().toString();
    }

    //Participant Management APIs

    @PostMapping(value = "/participants/assignTrial", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> assignTrial(HttpServletRequest request) throws IllegalArgumentException {
        int patientID = Integer.valueOf(request.getParameter("patientID"));
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(EditTrial.class, trialID, patientID, fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @GetMapping(value = "/participants/getallparticipants", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientState>>> getallparticipants() {
        return ResponseEntity.ok(proxy.vaultQuery(PatientState.class).getStates());
    }

    @GetMapping(value = "/participants/getparticipants/{patientID}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientState>>> getparticipant(@PathVariable int patientID) {
        List<StateAndRef<PatientState>> patients = proxy.vaultQuery(PatientState.class).getStates().stream().filter(
            it -> it.getState().getData().getPatientID() == patientID).collect(Collectors.toList());

        return ResponseEntity.ok(patients);
    }

    @GetMapping(value = "/participants/getevaluation/{patientID}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<PatientEvaluationState>>> getevaluation(@PathVariable int patientID) {
        QueryCriteria generalCriteria = new VaultQueryCriteria(Vault.StateStatus.ALL);
        List<StateAndRef<PatientEvaluationState>> patients = proxy.vaultQueryByCriteria(generalCriteria, PatientEvaluationState.class).getStates().stream().filter(
            it -> it.getState().getData().getPatientID() == patientID).collect(Collectors.toList());
        return ResponseEntity.ok(patients);
    }

    @PostMapping(value = "/participants/changetrialdates", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> changeTrialDates(HttpServletRequest request) throws IllegalArgumentException {
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(EditTrial.class, trialID, startDate, endDate, fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // APIs for Trial Management

    @PostMapping(value = "/trials/createtrial", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createTrial(HttpServletRequest request) throws IllegalArgumentException {
        int trialID = Integer.valueOf(request.getParameter("trialID"));
        String trialPatients = request.getParameter("trialPatients");
        String trialTemplateID = request.getParameter("trialTemplateID");
        String trialStatus = request.getParameter("trialStatus");
        String trialStartDate = request.getParameter("trialStartDate");
        String trialEndDate = request.getParameter("trialEndDate");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(SendTrial.class, trialID, trialPatients, trialTemplateID,
            trialStatus, trialStartDate, trialEndDate, fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping(value = "/trials/trackshipment/{kitID}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StateAndRef<ShipmentRequestState>>> trackShipment(@PathVariable String kitID) throws IllegalArgumentException {
        List<StateAndRef<KitShipmentState>> kits = proxy.vaultQuery(KitShipmentState.class).getStates().stream().filter(
            it -> it.getState().getData().getKitID().equals(kitID)).collect(Collectors.toList());
        if (kits.isEmpty()) {
            throw new IllegalArgumentException("No such kit exist");
        }
        String packageID = kits.get(0).getState().getData().getPackageID();
        return ResponseEntity.ok(proxy.vaultQuery(ShipmentRequestState.class).getStates().stream().filter(
            it -> it.getState().getData().getPackageID().equals(packageID)).collect(Collectors.toList()));
    }

    // APIs for Kit Management

    @PostMapping(value = "/kits/createshipment", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createShipment(HttpServletRequest request) {
        String shipmentMappingID = request.getParameter("shipmentMappingID");
        String packageID = request.getParameter("packageID");
        String fromPharma = request.getParameter("fromPharma");
        String toLogistics = request.getParameter("toLogistics");

        try {
            String result = proxy.startTrackedFlowDynamic(SendShipmentRequest.class, shipmentMappingID, packageID, fromPharma, toLogistics).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // APIs for Medical Management

    @PostMapping (value = "/medical/createstaff", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> createStaff(HttpServletRequest request) {
        String staffID = request.getParameter("staffID");
        String staffName = request.getParameter("staffName");
        String proficiency = request.getParameter("proficiency");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(SendMedicalStaffData.class, staffID, 
                staffName, proficiency, fromPharma, toMedical).getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping(value = "/medical/createstaff/{staffID}", produces = TEXT_PLAIN_VALUE)
    public ResponseEntity<String> deleteStaff(HttpServletRequest request) {
        String staffID = request.getParameter("staffID");
        String fromPharma = request.getParameter("fromPharma");
        String toMedical = request.getParameter("toMedical");

        try {
            String result = proxy.startTrackedFlowDynamic(DeleteMedicalStaffData.class, staffID, fromPharma, toMedical)
                .getReturnValue().get();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}