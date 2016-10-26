/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.processing.distributor.core;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingBadRequestException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.exception.WorkerAlreadyExistsException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionDefinition;
import fr.gouv.vitam.processing.common.model.Distribution;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.ProcessStep;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.model.WorkFlow;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.processing.engine.core.monitoring.ProcessMonitoringImpl;

public class ProcessDistributorImplTest {
    private WorkerParameters params;
    private static final ProcessDistributorImpl PROCESS_DISTRIBUTOR =
        ProcessDistributorImplFactory.getDefaultDistributor();
    private static final String WORKFLOW_ID = "workflowJSONv1";
    private ProcessMonitoringImpl processMonitoring;
    private WorkFlow worfklow;
    private static JunitHelper junitHelper;
    private String urlWorkspace;
    private static int port;

    private static final String WORKER_DESCRIPTION =
        "{ \"name\" : \"workername\", \"family\" : \"familyname\", \"capacity\" : 10, \"storage\" : 100," +
            "\"status\" : \"Active\", \"configuration\" : {\"serverHost\" : \"localhost\", \"serverPort\" : \"89102\" } }";

    @Before
    public void setUp() throws Exception {
        junitHelper = JunitHelper.getInstance();
        port = junitHelper.findAvailablePort();
        urlWorkspace = "http://localhost:" + Integer.toString(port);
        params = WorkerParametersFactory.newWorkerParameters();
        params.setWorkerGUID(GUIDFactory.newGUID());
        // TODO P1 : ??? mandatory
        params.setUrlMetadata("http://localhost:8083");
        params.setUrlWorkspace(urlWorkspace);
        processMonitoring = ProcessMonitoringImpl.getInstance();
        final List<Step> steps = new ArrayList<>();
        final Step step = new Step().setStepName("TEST");
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        action.setActionDefinition(
            new ActionDefinition().setActionKey("ExtractSeda").setBehavior(ProcessBehavior.NOBLOCKING));
        actions.add(action);
        step.setBehavior(ProcessBehavior.NOBLOCKING).setActions(actions);
        steps.add(step);
        worfklow = new WorkFlow().setSteps(steps).setId(WORKFLOW_ID);
        // set process_id and step_id (set in the engine)
        params.setProcessId("processId");
        final Map<String, ProcessStep> processSteps =
            processMonitoring.initOrderedWorkflow("processId", worfklow, "containerName");
        for (final Map.Entry<String, ProcessStep> entry : processSteps.entrySet()) {
            params.setStepUniqId(entry.getKey());
        }
    }


    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        junitHelper.releasePort(port);
    }


    @Test
    public void givenProcessDistributorWhendistributeThenProcessRefDistribution() {
        final Step step = new Step();
        step.setStepName("Traiter_archives");
        step.setBehavior(ProcessBehavior.BLOCKING);
        step.setDistribution(new Distribution().setKind(DistributionKind.REF).setElement("testElement"));
        final List<Action> actions = new ArrayList<>();
        final Action action = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("ExtractSeda");
        actionDefinition.setBehavior(ProcessBehavior.NOBLOCKING);
        action.setActionDefinition(actionDefinition);
        actions.add(action);
        step.setActions(actions);
        final ProcessStep processStep = new ProcessStep(step, "containerName", WORKFLOW_ID, 0, 0, 0);
        final String familyId = "NewFamilyId" + GUIDFactory.newGUID().getId();
        final String workerId = "NewWorkerId";
        try {
            WorkerManager.registerWorker(familyId, workerId, WORKER_DESCRIPTION);
        } catch (WorkerAlreadyExistsException e) {
            fail("In Junit, there can't be an already registered Worker . Parallel issue");
        } catch (ProcessingBadRequestException e) {
            fail("Incorrect worker description");
        }
        
        PROCESS_DISTRIBUTOR.distribute(params, processStep, WORKFLOW_ID);
    }
    
    

    @Test(expected = IllegalArgumentException.class)
    public void givenProcessDistributorWhendistributeThenCatchIllegalArgumentException() {
        final Step step = new Step();
        final Action a = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("notExist");
        actionDefinition.setBehavior(ProcessBehavior.NOBLOCKING);
        a.setActionDefinition(actionDefinition);
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);
        
        
        PROCESS_DISTRIBUTOR.distribute(params, null, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithListKindThenCatchHandlerNotFoundException() {
        final Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.LIST));
        final Action a = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("notExist");
        actionDefinition.setBehavior(ProcessBehavior.NOBLOCKING);
        a.setActionDefinition(actionDefinition);
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);

        final ProcessStep processStep = new ProcessStep(step, "containerName", WORKFLOW_ID, 0, 0, 0);

        PROCESS_DISTRIBUTOR.distribute(params, processStep, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeWithRefThenCatchHandlerNotFoundException() {
        final Step step = new Step().setDistribution(new Distribution().setKind(DistributionKind.REF));
        final Action a = new Action();
        final ActionDefinition actionDefinition = new ActionDefinition();
        actionDefinition.setActionKey("notExist");
        actionDefinition.setBehavior(ProcessBehavior.NOBLOCKING);
        a.setActionDefinition(actionDefinition);
        final List<Action> actions = new ArrayList<>();
        actions.add(a);
        step.setActions(actions);

        final ProcessStep processStep = new ProcessStep(step, "containerName", WORKFLOW_ID, 0, 0, 0);

        PROCESS_DISTRIBUTOR.distribute(params, processStep, WORKFLOW_ID);
    }

    @Test
    public void givenProcessDistributorWhenDistributeThenProcessStepsNotEmpty()
        throws IllegalArgumentException, ProcessingException {
        // final WorkerImpl worker = mock(WorkerImpl.class);
        final List<EngineResponse> response = new ArrayList<>();
        response.add(new ProcessResponse().setStatus(StatusCode.OK));
        // when(worker.run(anyObject(), anyObject())).thenReturn(response);

        final Step step = worfklow.getSteps().get(0);
        final ProcessStep processStep = new ProcessStep(step, "containerName", WORKFLOW_ID, 0, 0, 0);

        PROCESS_DISTRIBUTOR.distribute(params, processStep, WORKFLOW_ID);

        // checkMonitoring
        // String processId = (String) params.getAdditionalProperties().get(WorkParams.PROCESS_ID);
        final String processId = params.getProcessId();
        final Map<String, ProcessStep> map = processMonitoring.getWorkflowStatus(processId);
        assertNotNull(map);
        // At least one element has been added to be processed
        for (final Map.Entry<String, ProcessStep> entry : map.entrySet()) {
            assertTrue(entry.getValue().getElementToProcess() > 0);
        }
    }
}
